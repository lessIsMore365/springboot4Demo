package org.example.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.entity.AiChatHistory;
import org.example.entity.AiChatSession;
import org.example.mapper.AiChatHistoryMapper;
import org.example.mapper.AiChatSessionMapper;
import org.example.ai.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AiChatHistoryService {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatHistoryMapper historyMapper;

    public AiChatHistoryService(AiChatSessionMapper sessionMapper, AiChatHistoryMapper historyMapper) {
        this.sessionMapper = sessionMapper;
        this.historyMapper = historyMapper;
    }

    public AiChatSession createSession(String sessionId, Long userId, String username, String title, String model) {
        AiChatSession session = new AiChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setUsername(username);
        session.setTitle(title != null ? title : "新对话");
        session.setModel(model);
        session.setMessageCount(0);
        session.setTotalTokens(0);
        session.setTotalCost(BigDecimal.ZERO);
        sessionMapper.insert(session);
        return session;
    }

    public void saveMessages(String sessionId, List<ChatMessage> messages, String model) {
        AiChatSession session = getSession(sessionId);
        if (session == null) {
            session = createSession(sessionId, null, null, null, model);
        }
        int seq = 0;
        int totalTokens = session.getTotalTokens() != null ? session.getTotalTokens() : 0;
        for (ChatMessage msg : messages) {
            AiChatHistory history = new AiChatHistory();
            history.setSessionId(sessionId);
            history.setRole(msg.role());
            history.setContent(msg.content());
            if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                history.setToolCalls(toJson(msg.toolCalls()));
            }
            history.setToolCallId(msg.toolCallId());
            history.setSeq(seq++);
            history.setCreateTime(LocalDateTime.now());
            historyMapper.insert(history);
        }
        session.setMessageCount(session.getMessageCount() + messages.size());
        session.setTotalTokens(totalTokens);
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.updateById(session);
    }

    public AiChatSession getSession(String sessionId) {
        QueryWrapper<AiChatSession> qw = new QueryWrapper<>();
        qw.eq("session_id", sessionId);
        return sessionMapper.selectOne(qw);
    }

    public IPage<AiChatSession> listSessions(int page, int size, String username) {
        QueryWrapper<AiChatSession> qw = new QueryWrapper<>();
        if (username != null && !username.isBlank()) qw.eq("username", username);
        qw.orderByDesc("update_time");
        return sessionMapper.selectPage(new Page<>(page, size), qw);
    }

    public List<AiChatHistory> getHistory(String sessionId) {
        QueryWrapper<AiChatHistory> qw = new QueryWrapper<>();
        qw.eq("session_id", sessionId).orderByAsc("seq");
        return historyMapper.selectList(qw);
    }

    public void deleteSession(String sessionId) {
        QueryWrapper<AiChatHistory> hqw = new QueryWrapper<>();
        hqw.eq("session_id", sessionId);
        historyMapper.delete(hqw);
        QueryWrapper<AiChatSession> sqw = new QueryWrapper<>();
        sqw.eq("session_id", sessionId);
        sessionMapper.delete(sqw);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        QueryWrapper<AiChatSession> sqw = new QueryWrapper<>();
        stats.put("totalSessions", sessionMapper.selectCount(sqw));
        QueryWrapper<AiChatHistory> hqw = new QueryWrapper<>();
        stats.put("totalMessages", historyMapper.selectCount(hqw));
        return stats;
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
