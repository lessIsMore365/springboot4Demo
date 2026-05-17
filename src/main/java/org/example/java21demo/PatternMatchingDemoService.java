package org.example.java21demo;

import lombok.extern.slf4j.Slf4j;
import org.example.java21demo.model.Circle;
import org.example.java21demo.model.Rectangle;
import org.example.java21demo.model.Shape;
import org.example.java21demo.model.Triangle;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 模式匹配演示 - Java 21 正式特性
 * 涵盖：switch 模式匹配、instanceof 类型模式、record 解构、sealed class 穷举、guarded pattern
 */
@Slf4j
@Service
public class PatternMatchingDemoService {

    // ==================== 1. Switch + Record Pattern：穷举图形面积 ====================

    public Map<String, Object> computeArea(Shape shape) {
        // 使用 switch 表达式 + record pattern 解构
        double area = switch (shape) {
            case Circle(var radius)          -> Math.PI * radius * radius;
            case Rectangle(var w, var h)     -> w * h;
            case Triangle(var base, var h)   -> 0.5 * base * h;
            // 不需要 default 分支 — sealed class 保证穷举性
        };

        return Map.of(
                "shapeType", shape.getClass().getSimpleName(),
                "shapeDetails", shape.toString(),
                "area", Math.round(area * 100.0) / 100.0,
                "pattern", "switch + record pattern: Circle(var r) -> π*r²"
        );
    }

    // ==================== 2. instanceof 类型模式 ====================

    public Map<String, Object> describeType(Object obj) {
        // instanceof 模式匹配 — 一步完成类型检查和变量绑定
        String description = switch (obj) {
            case null          -> "null 值";
            case String s      -> String.format("字符串: \"%s\" (长度=%d)", s, s.length());
            case Integer i     -> "整数: " + i;
            case Double d      -> String.format("浮点数: %.4f", d);
            case Long l        -> "长整数: " + l;
            case Boolean b     -> "布尔值: " + b;
            case List<?> list  -> "列表: " + list.size() + " 个元素";
            case Shape s       -> "图形: " + s.getClass().getSimpleName();
            case int[] arr     -> "int[]: " + arr.length + " 个元素";
            default            -> "未知类型: " + obj.getClass().getName();
        };

        return Map.of(
                "input", String.valueOf(obj),
                "inputType", obj != null ? obj.getClass().getName() : "null",
                "description", description
        );
    }

    // ==================== 3. Guarded Pattern（when 子句） ====================

    public Map<String, Object> categorizeShape(Shape shape) {
        String category = switch (shape) {
            case Circle(var r) when r > 100      -> "巨型圆";
            case Circle(var r) when r > 10       -> "大圆";
            case Circle(var r)                    -> "小圆";
            case Rectangle(var w, var h)
                    when w == h                   -> "正方形 (边长=" + w + ")";
            case Rectangle(var w, var h)
                    when w > 2 * h || h > 2 * w   -> "长条形矩形";
            case Rectangle(var w, var h)          -> "标准矩形";
            case Triangle(var b, var h)
                    when b == h                   -> "等腰直角三角形";
            case Triangle _                       -> "普通三角形";
        };

        return Map.of(
                "shape", shape.toString(),
                "category", category,
                "pattern", "guarded pattern: case Circle(var r) when r > 100 -> \"巨型圆\""
        );
    }

    // ==================== 4. 嵌套 Record Pattern ====================

    public record Point(int x, int y) {}
    public record Line(Point start, Point end) {}
    public record ColoredLine(Line line, String color) {}

    public Map<String, Object> nestedPatternDemo(Object obj) {
        String result = switch (obj) {
            case ColoredLine(Line(Point(var x1, var y1), Point(var x2, var y2)), var color)
                    -> String.format("%s 色线段: (%d,%d)→(%d,%d)", color, x1, y1, x2, y2);
            case Line(Point(var x1, var y1), Point(var x2, var y2))
                    -> String.format("线段: (%d,%d)→(%d,%d), 长度=%.2f",
                                      x1, y1, x2, y2, Math.hypot(x2 - x1, y2 - y1));
            default -> "其他类型: " + obj.getClass().getSimpleName();
        };

        return Map.of(
                "input", obj.toString(),
                "result", result,
                "pattern", "嵌套 record pattern: ColoredLine(Line(Point(var x1, var y1), Point(var x2, var y2)), var color)"
        );
    }

    // ==================== 5. 实际场景：统一响应处理 ====================

    public sealed interface ApiResponse permits SuccessResponse, ErrorResponse {}
    public record SuccessResponse(Object data, String message) implements ApiResponse {}
    public record ErrorResponse(int code, String error, List<String> details) implements ApiResponse {}

    public Map<String, Object> handleApiResponse(ApiResponse response) {
        return switch (response) {
            case SuccessResponse(var data, var msg) -> Map.of(
                    "type", "SUCCESS",
                    "data", data,
                    "message", msg
            );
            case ErrorResponse(var code, var error, var details)
                    when details.isEmpty() -> Map.of(
                    "type", "ERROR",
                    "code", code,
                    "error", error
            );
            case ErrorResponse(var code, var error, var details) -> Map.of(
                    "type", "ERROR",
                    "code", code,
                    "error", error,
                    "details", details,
                    "hint", "含 " + details.size() + " 条详细信息"
            );
        };
    }

    // ==================== 6. Unnamed Pattern (_) ====================

    public Map<String, Object> unnamedPatternDemo(Shape shape) {
        String category = switch (shape) {
            case Circle _          -> "圆形 (忽略半径)";
            case Rectangle(_, _)  -> "矩形 (忽略宽高)";
            case Triangle _       -> "三角形 (忽略底高)";
        };

        return Map.of(
                "shape", shape.getClass().getSimpleName(),
                "category", category,
                "note", "使用 _ 忽略不需要的组件，代替未使用的变量名"
        );
    }

    // ==================== 7. 传统 if-else vs 模式匹配对比 ====================

    /**
     * 传统做法：计算面积 — 用 if-else + instanceof + 强制转型
     */
    public Map<String, Object> computeAreaTraditional(Shape shape) {
        StringBuilder code = new StringBuilder();
        code.append("if (shape instanceof Circle) {\n");
        code.append("    Circle c = (Circle) shape;\n");
        code.append("    return Math.PI * c.radius() * c.radius();\n");
        code.append("} else if (shape instanceof Rectangle) {\n");
        code.append("    Rectangle r = (Rectangle) shape;\n");
        code.append("    return r.width() * r.height();\n");
        code.append("} else if (shape instanceof Triangle) {\n");
        code.append("    Triangle t = (Triangle) shape;\n");
        code.append("    return 0.5 * t.base() * t.height();\n");
        code.append("} else {\n");
        code.append("    throw new IllegalStateException(\"未知图形\");\n");
        code.append("}");

        double area;
        if (shape instanceof Circle) {
            Circle c = (Circle) shape;
            area = Math.PI * c.radius() * c.radius();
        } else if (shape instanceof Rectangle) {
            Rectangle r = (Rectangle) shape;
            area = r.width() * r.height();
        } else if (shape instanceof Triangle) {
            Triangle t = (Triangle) shape;
            area = 0.5 * t.base() * t.height();
        } else {
            throw new IllegalStateException("未知图形: " + shape.getClass());
        }

        return Map.of(
                "traditional", Map.of(
                        "approach", "if-else + instanceof + 强制转型",
                        "code", code.toString(),
                        "area", Math.round(area * 100.0) / 100.0,
                        "problems", List.of(
                                "需要 instanceof 检查 + 强制转型，重复代码",
                                "编译器不检查穷举性 → 新增子类容易遗漏"
                        )
                ),
                "patternMatching", Map.of(
                        "approach", "switch + record pattern",
                        "code", "switch (shape) {\n"
                                + "    case Circle(var r) -> Math.PI * r * r;\n"
                                + "    case Rectangle(var w, var h) -> w * h;\n"
                                + "    case Triangle(var b, var h) -> 0.5 * b * h;\n"
                                + "}",
                        "area", computeArea(shape).get("area"),
                        "advantages", List.of(
                                "一步完成类型判断 + 解构 → 无需强制转型",
                                "sealed class 保证穷举性 → 编译期检查"
                        )
                )
        );
    }

    /**
     * 传统做法：描述对象类型 — 用 if-else 链
     */
    public Map<String, Object> describeTypeTraditional(Object obj) {
        StringBuilder code = new StringBuilder();
        code.append("if (obj == null) { ... }\n");
        code.append("else if (obj instanceof String) { String s = (String) obj; ... }\n");
        code.append("else if (obj instanceof Integer) { Integer i = (Integer) obj; ... }\n");
        code.append("else if (obj instanceof Double) { Double d = (Double) obj; ... }\n");
        code.append("else if (obj instanceof List) { List<?> list = (List<?>) obj; ... }\n");
        code.append("else { ... }");

        String description;
        if (obj == null) {
            description = "null 值";
        } else if (obj instanceof String) {
            String s = (String) obj;
            description = String.format("字符串: \"%s\" (长度=%d)", s, s.length());
        } else if (obj instanceof Integer) {
            Integer i = (Integer) obj;
            description = "整数: " + i;
        } else if (obj instanceof Double) {
            Double d = (Double) obj;
            description = String.format("浮点数: %.4f", d);
        } else if (obj instanceof List) {
            List<?> list = (List<?>) obj;
            description = "列表: " + list.size() + " 个元素";
        } else {
            description = "未知类型: " + obj.getClass().getName();
        }

        return Map.of(
                "traditional", Map.of(
                        "approach", "if-else + instanceof + 转型",
                        "code", code.toString(),
                        "result", description,
                        "problems", List.of(
                                "每个分支都要 instanceof + 强制转型两步操作",
                                "添加新类型需要新增 else-if 分支",
                                "容易忘记 null 检查"
                        )
                ),
                "patternMatching", Map.of(
                        "approach", "switch + 类型模式",
                        "code", "switch (obj) {\n"
                                + "    case null -> ...;\n"
                                + "    case String s -> ...;\n"
                                + "    case Integer i -> ...;\n"
                                + "    case List<?> list -> ...;\n"
                                + "    default -> ...;\n"
                                + "}",
                        "result", describeType(obj).get("description"),
                        "advantages", List.of(
                                "类型判断 + 变量绑定一步完成",
                                "null 作为显式 case 处理",
                                "更清晰的代码结构"
                        )
                )
        );
    }

    /**
     * 传统做法：处理 API 响应 — 用 Visitor 模式或 if-else
     */
    public Map<String, Object> handleApiResponseTraditional(ApiResponse response) {
        StringBuilder code = new StringBuilder();
        code.append("// 传统方式 1：if-else + instanceof\n");
        code.append("if (response instanceof SuccessResponse) {\n");
        code.append("    SuccessResponse sr = (SuccessResponse) response;\n");
        code.append("    return Map.of(\"type\", \"SUCCESS\", \"data\", sr.data(), ...);\n");
        code.append("} else if (response instanceof ErrorResponse) {\n");
        code.append("    ErrorResponse er = (ErrorResponse) response;\n");
        code.append("    if (er.details().isEmpty()) { ... } else { ... }\n");
        code.append("}\n\n");
        code.append("// 传统方式 2：Visitor 模式（需要额外定义 accept/Visitor 接口）");

        Map<String, Object> result;
        if (response instanceof SuccessResponse) {
            SuccessResponse sr = (SuccessResponse) response;
            result = Map.of("type", "SUCCESS", "data", sr.data(), "message", sr.message());
        } else if (response instanceof ErrorResponse) {
            ErrorResponse er = (ErrorResponse) response;
            if (er.details().isEmpty()) {
                result = Map.of("type", "ERROR", "code", er.code(), "error", er.error());
            } else {
                result = Map.of("type", "ERROR", "code", er.code(), "error", er.error(),
                        "details", er.details());
            }
        } else {
            throw new IllegalStateException("未知响应类型");
        }

        return Map.of(
                "traditional", Map.of(
                        "approach", "if-else + instanceof + 转型（或 Visitor 模式）",
                        "code", code.toString(),
                        "result", result,
                        "problems", List.of(
                                "需要编写大量 if-else 或 Visitor 样板代码",
                                "嵌套判断（如 details.isEmpty()）需要多层 if"
                        )
                ),
                "patternMatching", Map.of(
                        "approach", "switch + record pattern + guarded pattern",
                        "code", "switch (response) {\n"
                                + "    case SuccessResponse(var data, var msg) -> ...;\n"
                                + "    case ErrorResponse(var code, var err, var details)\n"
                                + "        when details.isEmpty() -> ...;\n"
                                + "    case ErrorResponse(var code, var err, var details) -> ...;\n"
                                + "}",
                        "result", handleApiResponse(response),
                        "advantages", List.of(
                                "一步解构：类型判断 + 字段提取 + 条件过滤",
                                "when 子句取代嵌套 if"
                        )
                )
        );
    }
}
