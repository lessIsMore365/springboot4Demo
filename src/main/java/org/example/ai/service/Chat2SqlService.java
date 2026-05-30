package org.example.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai.provider.AiModelProvider;
import org.example.ai.provider.AiModelRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

@Service
public class Chat2SqlService {

    private static final Logger log = LoggerFactory.getLogger(Chat2SqlService.class);

    private final AiModelRouter router;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public Chat2SqlService(
            AiModelRouter router,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            DataSource dataSource) {
        this.router = router;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public Map<String, Object> query(String question) {
        return query(question, null);
    }

    public Map<String, Object> query(String question, String providerName) {
        AiModelProvider provider = router.resolve(providerName);
        String schemaInfo = extractSchemaInfo();
        String sql = generateSql(question, schemaInfo, provider);
        sql = cleanSql(sql);
        validateSql(sql);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", question);
        result.put("sql", sql);
        result.put("rowCount", rows.size());
        result.put("rows", rows);
        result.put("model", provider.getModel());
        return result;
    }

    private String extractSchemaInfo() {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            sb.append("数据库: ").append(meta.getDatabaseProductName())
                    .append(" ").append(meta.getDatabaseProductVersion()).append("\n\n");
            Set<String> tables = new LinkedHashSet<>();
            try (ResultSet rs = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            for (String table : tables) {
                sb.append("表: ").append(table).append("\n");
                sb.append("  列:\n");
                try (ResultSet rs = meta.getColumns(null, "public", table, "%")) {
                    while (rs.next()) {
                        String col = rs.getString("COLUMN_NAME");
                        String type = rs.getString("TYPE_NAME");
                        String comment = rs.getString("REMARKS");
                        sb.append("    - ").append(col).append(" ").append(type);
                        if (comment != null && !comment.isEmpty()) sb.append(" -- ").append(comment);
                        sb.append("\n");
                    }
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            log.warn("Failed to extract schema info: {}", e.getMessage());
            sb.append("(schema info unavailable: ").append(e.getMessage()).append(")");
        }
        return sb.toString();
    }

    private String generateSql(String question, String schemaInfo, AiModelProvider provider) {
        String systemPrompt = """
                你是一个SQL生成助手。根据用户提供的问题和数据库表结构，生成对应的SQL查询语句。
                要求：
                1. 只生成 SELECT 查询语句，禁止 INSERT/UPDATE/DELETE/DROP/ALTER/TRUNCATE/CREATE
                2. 只返回SQL语句本身，不要包含任何解释、注释或markdown格式
                3. 使用 PostgreSQL 语法
                4. 如涉及时间，使用 ISO 格式字符串
                5. 如果问题无法转化为SQL，返回: UNSUPPORTED
                6. 默认 LIMIT 100，除非用户指定""";

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", provider.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content",
                                    "表结构：\n" + schemaInfo + "\n问题：" + question)
                    ),
                    "max_tokens", 500,
                    "temperature", 0.1
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
            return "UNSUPPORTED";
        } catch (Exception e) {
            log.error("SQL generation failed", e);
            return "UNSUPPORTED: " + e.getMessage();
        }
    }

    private String cleanSql(String sql) {
        if (sql == null) return "UNSUPPORTED";
        sql = sql.trim();
        sql = sql.replaceAll("```sql\\s*", "").replaceAll("```\\s*", "");
        sql = sql.replaceAll("^\\s*SELECT", "SELECT");
        sql = sql.replaceAll(";\\s*$", "");
        return sql.trim();
    }

    private void validateSql(String sql) {
        if (sql == null || sql.isEmpty() || sql.startsWith("UNSUPPORTED")) {
            throw new IllegalArgumentException("无法将问题转化为SQL查询");
        }
        String upper = sql.toUpperCase().trim();
        if (!upper.startsWith("SELECT")) {
            throw new IllegalArgumentException("只允许 SELECT 查询，生成的SQL: " + sql);
        }
        String[] forbidden = {"INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "CREATE", "EXEC", "EXECUTE", "GRANT", "REVOKE"};
        for (String kw : forbidden) {
            if (upper.contains(kw)) {
                throw new IllegalArgumentException("禁止的关键字: " + kw);
            }
        }
    }
}
