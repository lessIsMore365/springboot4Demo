package org.example.ai.service;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.example.ai.provider.AiModelProvider;
import org.example.ai.provider.AiModelRouter;
import org.example.entity.AiKnowledgeBase;
import org.example.entity.AiKnowledgeChunk;
import org.example.entity.AiKnowledgeDoc;
import org.example.mapper.AiKnowledgeBaseMapper;
import org.example.mapper.AiKnowledgeChunkMapper;
import org.example.mapper.AiKnowledgeDocMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.*;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private static final int CHUNK_SIZE = 500;

    private final AiKnowledgeBaseMapper kbMapper;
    private final AiKnowledgeDocMapper docMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiModelRouter router;

    public RagService(AiKnowledgeBaseMapper kbMapper, AiKnowledgeDocMapper docMapper,
                      AiKnowledgeChunkMapper chunkMapper, AiModelRouter router) {
        this.kbMapper = kbMapper;
        this.docMapper = docMapper;
        this.chunkMapper = chunkMapper;
        this.router = router;
    }

    // ==================== 知识库管理 ====================

    public List<AiKnowledgeBase> listKbs() {
        return kbMapper.selectList(null);
    }

    public AiKnowledgeBase createKb(String name, String description) {
        AiKnowledgeBase kb = new AiKnowledgeBase();
        kb.setName(name);
        kb.setDescription(description);
        kb.setDocCount(0);
        kb.setChunkCount(0);
        kb.setEnabled(true);
        kbMapper.insert(kb);
        return kb;
    }

    public void deleteKb(Long kbId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiKnowledgeChunk> cqw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        cqw.eq("kb_id", kbId);
        chunkMapper.delete(cqw);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiKnowledgeDoc> dqw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        dqw.eq("kb_id", kbId);
        docMapper.delete(dqw);

        kbMapper.deleteById(kbId);
    }

    // ==================== 文档管理 ====================

    public List<AiKnowledgeDoc> listDocs(Long kbId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiKnowledgeDoc> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("kb_id", kbId).orderByDesc("create_time");
        return docMapper.selectList(qw);
    }

    public AiKnowledgeDoc uploadDoc(Long kbId, MultipartFile file) {
        AiKnowledgeBase kb = kbMapper.selectById(kbId);
        if (kb == null) throw new IllegalArgumentException("知识库不存在: " + kbId);

        String text;
        try {
            text = parseDocument(file);
        } catch (Exception e) {
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }

        AiKnowledgeDoc doc = new AiKnowledgeDoc();
        doc.setKbId(kbId);
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(getExtension(file.getOriginalFilename()));
        doc.setFileSize(file.getSize());
        doc.setContentText(text);
        doc.setStatus("PARSED");
        docMapper.insert(doc);

        List<String> chunks = splitText(text, CHUNK_SIZE);
        for (int i = 0; i < chunks.size(); i++) {
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setDocId(doc.getId());
            chunk.setKbId(kbId);
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setTokenCount(estimateTokens(chunks.get(i)));
            chunkMapper.insert(chunk);
        }

        doc.setChunkCount(chunks.size());
        doc.setStatus("INDEXED");
        docMapper.updateById(doc);

        kb.setDocCount(kb.getDocCount() + 1);
        kb.setChunkCount(kb.getChunkCount() + chunks.size());
        kbMapper.updateById(kb);

        return doc;
    }

    public void deleteDoc(Long docId) {
        AiKnowledgeDoc doc = docMapper.selectById(docId);
        if (doc == null) return;

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiKnowledgeChunk> cqw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        cqw.eq("doc_id", docId);
        chunkMapper.delete(cqw);

        AiKnowledgeBase kb = kbMapper.selectById(doc.getKbId());
        if (kb != null) {
            kb.setDocCount(Math.max(0, kb.getDocCount() - 1));
            kb.setChunkCount(Math.max(0, kb.getChunkCount() - doc.getChunkCount()));
            kbMapper.updateById(kb);
        }

        docMapper.deleteById(docId);
    }

    // ==================== RAG 问答 ====================

    public Map<String, Object> ask(Long kbId, String question, String providerName) {
        AiModelProvider provider = router.resolve(providerName);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiKnowledgeChunk> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("kb_id", kbId);
        List<AiKnowledgeChunk> allChunks = chunkMapper.selectList(qw);

        List<AiKnowledgeChunk> topChunks = searchRelevant(question, allChunks, 5);

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < topChunks.size(); i++) {
            context.append("[片段").append(i + 1).append("] ").append(topChunks.get(i).getContent()).append("\n\n");
        }

        String systemPrompt = """
                你是一个知识库问答助手。请根据提供的文档片段回答用户的问题。
                要求：
                1. 仅根据提供的文档片段作答，不要编造信息
                2. 如果文档片段不足以回答问题，请明确说明"根据已有资料无法回答"
                3. 回答要简洁准确，引用具体片段编号
                4. 使用中文回答""";

        String answer = callLLM(provider, systemPrompt, context + "\n用户问题：" + question);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("answer", answer);
        result.put("sources", topChunks.stream().map(c -> Map.of(
                "docId", c.getDocId(),
                "chunkIndex", c.getChunkIndex(),
                "content", c.getContent().substring(0, Math.min(200, c.getContent().length()))
        )).toList());
        result.put("model", provider.getModel());
        return result;
    }

    // ==================== 内部方法 ====================

    private String parseDocument(MultipartFile file) throws Exception {
        Tika tika = new Tika();
        return tika.parseToString(file.getInputStream());
    }

    private List<String> splitText(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        for (int i = 0; i < len; i += chunkSize) {
            int end = Math.min(i + chunkSize, len);
            chunks.add(text.substring(i, end));
        }
        return chunks;
    }

    private List<AiKnowledgeChunk> searchRelevant(String question, List<AiKnowledgeChunk> allChunks, int topK) {
        String query = question.toLowerCase();
        return allChunks.stream()
                .sorted((a, b) -> {
                    int scoreA = countMatches(a.getContent().toLowerCase(), query);
                    int scoreB = countMatches(b.getContent().toLowerCase(), query);
                    return Integer.compare(scoreB, scoreA);
                })
                .limit(topK)
                .toList();
    }

    private int countMatches(String content, String query) {
        int count = 0;
        for (int i = 0; i <= content.length() - 2; i++) {
            for (int len = 2; len <= Math.min(10, query.length()); len++) {
                if (i + len <= content.length() && query.contains(content.substring(i, i + len))) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private int estimateTokens(String text) {
        return text.length() / 2;
    }

    private String callLLM(AiModelProvider provider, String systemPrompt, String userMessage) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", provider.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "max_tokens", 1000,
                    "temperature", 0.3
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = provider.getRestClient().post()
                    .uri("/v1/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return "AI 响应异常";
        } catch (Exception e) {
            log.error("RAG LLM call failed", e);
            return "AI 调用失败: " + e.getMessage();
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "unknown";
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "unknown";
    }
}
