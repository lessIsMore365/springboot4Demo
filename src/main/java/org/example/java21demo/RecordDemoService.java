package org.example.java21demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Record 全生态化演示
 * 展示 Record 的多种使用场景：DTO、泛型、验证、嵌套、流操作、序列化、自定义构造器
 */
@Slf4j
@Service
public class RecordDemoService {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();  // 支持 Java 8+ 时间类型

    // ==================== Record 类型定义 ====================

    // 简单 DTO
    public record ProductDTO(Long id, String name, BigDecimal price, String category) {}

    // 泛型 Record
    public record PageResult<T>(List<T> items, int page, int size, long total) {}

    // 带验证的 compact constructor
    public record Email(String value) {
        public Email {
            if (value == null || !value.contains("@")) {
                throw new IllegalArgumentException("无效邮箱: " + value);
            }
        }
    }

    // 嵌套 Record
    public record Address(String city, String street, String zipCode) {}
    public record Customer(Long id, String name, Email email, Address address) {}

    // Record 实现接口
    public interface Identifiable {
        Long id();
    }
    public record TagDTO(Long id, String name) implements Identifiable {}

    // 带自定义方法的 Record
    public record Money(BigDecimal amount, String currency) {
        public Money add(Money other) {
            if (!currency.equals(other.currency)) {
                throw new IllegalArgumentException("币种不匹配");
            }
            return new Money(amount.add(other.amount), currency);
        }

        public static Money zero(String currency) {
            return new Money(BigDecimal.ZERO, currency);
        }
    }

    // ==================== 1. Record 作为 DTO ====================

    public Map<String, Object> recordAsDto() {
        var product = new ProductDTO(1L, "MacBook Pro", new BigDecimal("14999.00"), "电子产品");

        return Map.of(
                "product", product,
                "id", product.id(),
                "name", product.name(),
                "price", product.price(),
                "category", product.category(),
                "toString", product.toString(),
                "equals", product.equals(new ProductDTO(1L, "MacBook Pro",
                        new BigDecimal("14999.00"), "电子产品")),
                "note", "Record 自动生成: 规范构造器、访问器、equals、hashCode、toString"
        );
    }

    // ==================== 2. 泛型 Record ====================

    public Map<String, Object> genericRecord() {
        List<ProductDTO> products = List.of(
                new ProductDTO(1L, "MacBook", new BigDecimal("14999"), "电子"),
                new ProductDTO(2L, "iPhone", new BigDecimal("8999"), "电子"),
                new ProductDTO(3L, "AirPods", new BigDecimal("1999"), "配件")
        );

        PageResult<ProductDTO> page = new PageResult<>(products, 1, 10, 100);

        return Map.of(
                "page", page,
                "itemsClass", page.items().getClass().getName(),
                "total", page.total(),
                "note", "泛型 Record PageResult<T> 保留类型信息，可安全使用"
        );
    }

    // ==================== 3. 自定义构造器与验证 ====================

    public Map<String, Object> validationInConstructor() {
        Map<String, Object> results = new java.util.LinkedHashMap<>();

        // 合法邮箱
        Email validEmail = new Email("admin@example.com");
        results.put("validEmail", validEmail.value());

        // 非法邮箱
        try {
            new Email("invalid-email");
            results.put("invalidEmailResult", "创建成功 (不应该)");
        } catch (IllegalArgumentException e) {
            results.put("invalidEmailResult", "验证失败: " + e.getMessage());
        }

        results.put("note", "Compact constructor 中验证，确保 Record 对象始终有效（不可变 + 有效 = 安全）");

        return results;
    }

    // ==================== 4. 嵌套 Record ====================

    public Map<String, Object> nestedRecords() {
        var customer = new Customer(
                100L, "张三",
                new Email("zhangsan@example.com"),
                new Address("北京", "长安街 100 号", "100000")
        );

        // 深度访问
        String city = customer.address().city();
        String emailValue = customer.email().value();

        return Map.of(
                "customer", customer,
                "city", city,
                "email", emailValue,
                "note", "嵌套 Record 通过 .a().b().c() 链式访问，深层不可变"
        );
    }

    // ==================== 5. Record 在 Stream/集合中的使用 ====================

    public Map<String, Object> recordsInStreams() {
        List<ProductDTO> products = List.of(
                new ProductDTO(1L, "MacBook Pro", new BigDecimal("14999"), "电子"),
                new ProductDTO(2L, "iPhone 16", new BigDecimal("8999"), "电子"),
                new ProductDTO(3L, "AirPods Pro", new BigDecimal("1999"), "配件"),
                new ProductDTO(4L, "iPad Air", new BigDecimal("5499"), "电子"),
                new ProductDTO(5L, "充电器", new BigDecimal("149"), "配件")
        );

        // 使用 Record 作为中间类型分组
        record CategorySummary(String category, long count, BigDecimal totalPrice) {}

        List<CategorySummary> summaries = products.stream()
                .collect(Collectors.groupingBy(ProductDTO::category,
                        Collectors.teeing(
                                Collectors.counting(),
                                Collectors.reducing(BigDecimal.ZERO, ProductDTO::price, BigDecimal::add),
                                (count, total) -> new CategorySummary(
                                        null, count, total)  // category 后续填入
                        )))
                .entrySet().stream()
                .map(e -> new CategorySummary(e.getKey(), e.getValue().count(), e.getValue().totalPrice()))
                .sorted(Comparator.comparing(CategorySummary::totalPrice).reversed())
                .toList();

        // Record 与 Pattern Matching 结合
        String topCategory = switch (summaries.getFirst()) {
            case CategorySummary(var cat, var cnt, var total)
                    when total.compareTo(new BigDecimal("10000")) > 0
                    -> String.format("热销品类: %s (销量:%d, 金额:%s)", cat, cnt, total);
            case CategorySummary(var cat, var cnt, var total)
                    -> String.format("品类: %s (销量:%d, 金额:%s)", cat, cnt, total);
        };

        return Map.of(
                "summaries", summaries,
                "topCategory", topCategory,
                "note", "Record 作为 Stream 中间类型 → 不可变、语义清晰、支持 pattern matching 解构"
        );
    }

    // ==================== 6. Record 序列化/反序列化 ====================

    public Map<String, Object> serializationDemo() throws JsonProcessingException {
        var customer = new Customer(
                1L, "李四",
                new Email("lisi@example.com"),
                new Address("上海", "南京路 200 号", "200000")
        );

        // 序列化
        String json = objectMapper.writeValueAsString(customer);

        // 反序列化
        Customer deserialized = objectMapper.readValue(json, Customer.class);

        // 验证往返一致性
        boolean roundtripOk = customer.equals(deserialized);

        return Map.of(
                "original", customer,
                "json", json,
                "deserialized", deserialized,
                "roundtripEquals", roundtripOk,
                "note", "Jackson 2.12+ 原生支持 Record 序列化/反序列化，无需额外注解"
        );
    }

    // ==================== 7. Record 实现接口 ====================

    public Map<String, Object> recordImplementsInterface() {
        List<Identifiable> items = List.of(
                new TagDTO(1L, "Java"),
                new TagDTO(2L, "Spring"),
                new TagDTO(3L, "Virtual Thread")
        );

        List<Long> ids = items.stream().map(Identifiable::id).toList();

        return Map.of(
                "items", items,
                "allIds", ids,
                "note", "Record 可实现接口，便于多态使用"
        );
    }

    // ==================== 8. 自定义方法 Record ====================

    public Map<String, Object> recordWithMethods() {
        Money price1 = new Money(new BigDecimal("100.00"), "CNY");
        Money price2 = new Money(new BigDecimal("50.50"), "CNY");
        Money total = price1.add(price2);
        Money zero = Money.zero("CNY");

        return Map.of(
                "price1", price1,
                "price2", price2,
                "total", total,
                "zero", zero,
                "note", "Record 支持实例方法和静态工厂方法，实现富领域模型"
        );
    }

    // ==================== 9. Local Record（方法内 Record） ====================

    public Map<String, Object> localRecordDemo() {
        record PersonScore(String name, int score) implements Comparable<PersonScore> {
            @Override
            public int compareTo(PersonScore o) {
                return Integer.compare(o.score, this.score);
            }
        }

        var scores = List.of(
                new PersonScore("Alice", 95),
                new PersonScore("Bob", 87),
                new PersonScore("Charlie", 92)
        );

        var ranked = scores.stream().sorted().toList();

        return Map.of(
                "scores", scores,
                "ranked", ranked,
                "note", "方法内定义的 Local Record，适合临时数据结构"
        );
    }

    // ==================== 10. 传统 POJO vs Record 对比 ====================

    /**
     * 传统 POJO — 等效于 ProductDTO record
     */
    @SuppressWarnings("unused")
    private static class ProductPOJO {
        private final Long id;
        private final String name;
        private final BigDecimal price;
        private final String category;

        public ProductPOJO(Long id, String name, BigDecimal price, String category) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.category = category;
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
        public String getCategory() { return category; }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ProductPOJO that)) return false;
            return java.util.Objects.equals(id, that.id)
                    && java.util.Objects.equals(name, that.name)
                    && java.util.Objects.equals(price, that.price)
                    && java.util.Objects.equals(category, that.category);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id, name, price, category);
        }

        @Override
        public String toString() {
            return "ProductPOJO[id=" + id + ", name=" + name
                    + ", price=" + price + ", category=" + category + "]";
        }
    }

    public Map<String, Object> comparePojoVsRecord() {
        // 等效定义
        record ProductRecord(Long id, String name, BigDecimal price, String category) {}

        var pojo = new ProductPOJO(1L, "MacBook", new BigDecimal("9999"), "电子");
        var record = new ProductRecord(1L, "MacBook", new BigDecimal("9999"), "电子");

        // 统计代码行数
        String pojoCode = """
            // 传统 POJO（约 40 行）
            public class ProductPOJO {
                private final Long id;
                private final String name;
                private final BigDecimal price;
                private final String category;

                public ProductPOJO(Long id, String name, BigDecimal price, String category) {
                    this.id = id; this.name = name; this.price = price; this.category = category;
                }
                public Long getId() { return id; }
                public String getName() { return name; }
                public BigDecimal getPrice() { return price; }
                public String getCategory() { return category; }
                // equals() — 10+ 行
                // hashCode() — 3+ 行
                // toString() — 3+ 行
            }""";

        String recordCode = """
            // Record（1 行）
            record ProductRecord(Long id, String name, BigDecimal price, String category) {}""";

        return Map.of(
                "pojo", Map.of(
                        "definition", pojoCode,
                        "linesOfCode", "~40 行",
                        "hashCode", pojo.hashCode(),
                        "equals", pojo.equals(record),  // 不同类型不相等！
                        "note", "需要手写/IDE 生成: 构造器、getter、equals、hashCode、toString"
                ),
                "record", Map.of(
                        "definition", recordCode,
                        "linesOfCode", "1 行",
                        "hashCode", record.hashCode(),
                        "equals", record.equals(new ProductRecord(1L, "MacBook",
                                new BigDecimal("9999"), "电子")),
                        "note", "自动生成: 规范构造器、访问器方法、equals、hashCode、toString"
                ),
                "comparison", Map.of(
                        "immutability", "POJO 可用 final 字段模拟 | Record 天生不可变",
                        "boilerplate", "POJO ~40行样板代码 | Record 1行",
                        "serialization", "POJO 需注解 | Record Jackson 2.12+ 原生支持",
                        "inheritance", "POJO 可继承 | Record 不能继承（固定语义）",
                        "threadSafety", "POJO 需自行保证 | Record 不可变 → 天然线程安全"
                )
        );
    }
}
