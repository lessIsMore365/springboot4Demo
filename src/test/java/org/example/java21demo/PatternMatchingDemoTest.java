package org.example.java21demo;

import org.example.java21demo.model.Circle;
import org.example.java21demo.model.Rectangle;
import org.example.java21demo.model.Shape;
import org.example.java21demo.model.Triangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("模式匹配演示测试")
class PatternMatchingDemoTest {

    private final PatternMatchingDemoService service = new PatternMatchingDemoService();

    // ==================== Switch + Record Pattern 面积计算 ====================

    @Test
    @DisplayName("圆的面积 = π × r²")
    void testCircleArea() {
        Map<String, Object> result = service.computeArea(new Circle(5.0));
        // π * 5² = 78.54
        double area = (double) result.get("area");
        assertEquals(78.54, area, 0.01);
    }

    @Test
    @DisplayName("矩形面积 = 宽 × 高")
    void testRectangleArea() {
        Map<String, Object> result = service.computeArea(new Rectangle(4.0, 5.0));
        assertEquals(20.0, (double) result.get("area"), 0.01);
    }

    @Test
    @DisplayName("三角形面积 = 底 × 高 / 2")
    void testTriangleArea() {
        Map<String, Object> result = service.computeArea(new Triangle(6.0, 4.0));
        assertEquals(12.0, (double) result.get("area"), 0.01);
    }

    @Test
    @DisplayName("Sealed class 使 switch 穷举 - 不需要 default 分支")
    void testSwitchIsExhaustive() {
        // 编译期保证：如果 switch 覆盖了所有 sealed interface 的 permits 类型，无需 default
        for (Shape s : new Shape[]{new Circle(1), new Rectangle(1, 1), new Triangle(1, 1)}) {
            double area = switch (s) {
                case Circle(var r) -> Math.PI * r * r;
                case Rectangle(var w, var h) -> w * h;
                case Triangle(var b, var h) -> 0.5 * b * h;
            };
            assertTrue(area > 0, "面积应大于 0");
        }
    }

    // ==================== instanceof 类型模式 ====================

    @Test
    @DisplayName("instanceof 模式匹配 String")
    void testInstanceofString() {
        Map<String, Object> result = service.describeType("Hello World");
        assertTrue(result.get("description").toString().contains("Hello World"));
    }

    @Test
    @DisplayName("instanceof 模式匹配 Integer")
    void testInstanceofInteger() {
        Map<String, Object> result = service.describeType(42);
        assertTrue(result.get("description").toString().contains("42"));
    }

    @Test
    @DisplayName("instanceof 模式匹配 null")
    void testInstanceofNull() {
        Map<String, Object> result = service.describeType(null);
        assertEquals("null 值", result.get("description"));
    }

    @Test
    @DisplayName("instanceof 模式匹配 List")
    void testInstanceofList() {
        Map<String, Object> result = service.describeType(List.of(1, 2, 3));
        assertTrue(result.get("description").toString().contains("3 个元素"));
    }

    @Test
    @DisplayName("instanceof 模式匹配 Shape")
    void testInstanceofShape() {
        Map<String, Object> result = service.describeType(new Circle(1));
        assertTrue(result.get("description").toString().contains("图形"));
    }

    // ==================== Guarded Pattern（when 子句） ====================

    @Test
    @DisplayName("guarded pattern: r > 100 为巨型圆")
    void testGuardedLargeCircle() {
        Map<String, Object> result = service.categorizeShape(new Circle(150));
        assertEquals("巨型圆", result.get("category"));
    }

    @Test
    @DisplayName("guarded pattern: 正方形识别（宽 == 高）")
    void testGuardedSquare() {
        Map<String, Object> result = service.categorizeShape(new Rectangle(10, 10));
        assertEquals("正方形 (边长=10.0)", result.get("category"));
    }

    @Test
    @DisplayName("guarded pattern: 长条形矩形识别")
    void testGuardedLongRectangle() {
        Map<String, Object> result = service.categorizeShape(new Rectangle(1, 10));
        assertEquals("长条形矩形", result.get("category"));
    }

    // ==================== 嵌套 Record Pattern ====================

    @Test
    @DisplayName("嵌套 Record Pattern: ColoredLine 解构")
    void testNestedRecordPattern() {
        var point1 = new PatternMatchingDemoService.Point(0, 0);
        var point2 = new PatternMatchingDemoService.Point(3, 4);
        var line = new PatternMatchingDemoService.Line(point1, point2);
        var coloredLine = new PatternMatchingDemoService.ColoredLine(line, "红色");

        Map<String, Object> result = service.nestedPatternDemo(coloredLine);
        assertTrue(result.get("result").toString().contains("红色"));
        assertTrue(result.get("result").toString().contains("(0,0)"));
        assertTrue(result.get("result").toString().contains("(3,4)"));
    }

    // ==================== Unnamed Pattern ====================

    @Test
    @DisplayName("Unnamed pattern _ 忽略不需要的组件")
    void testUnnamedPattern() {
        for (Shape s : new Shape[]{new Circle(1), new Rectangle(1, 1), new Triangle(1, 1)}) {
            String category = switch (s) {
                case Circle _          -> "圆形 (忽略半径)";
                case Rectangle(_, _)  -> "矩形 (忽略宽高)";
                case Triangle _       -> "三角形 (忽略底高)";
            };
            assertNotNull(category);
        }
    }

    // ==================== API Response（sealed interface 实际场景） ====================

    @Test
    @DisplayName("SuccessResponse 模式匹配")
    void testSuccessResponse() {
        PatternMatchingDemoService.ApiResponse response =
                new PatternMatchingDemoService.SuccessResponse(Map.of("id", 1), "OK");

        Map<String, Object> result = service.handleApiResponse(response);
        assertEquals("SUCCESS", result.get("type"));
        assertEquals("OK", result.get("message"));
    }

    @Test
    @DisplayName("ErrorResponse 模式匹配（含 when 子句）")
    void testErrorResponseWithDetails() {
        PatternMatchingDemoService.ApiResponse response =
                new PatternMatchingDemoService.ErrorResponse(500, "服务器错误",
                        List.of("DB超时", "重试失败"));

        Map<String, Object> result = service.handleApiResponse(response);
        assertEquals("ERROR", result.get("type"));
        assertEquals(500, result.get("code"));
        assertEquals("2", result.get("hint").toString().replaceAll("[^0-9]", ""));
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("非法参数应在构造时抛出异常")
    void testInvalidShapeRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Circle(-1));
        assertThrows(IllegalArgumentException.class, () -> new Rectangle(0, 5));
        assertThrows(IllegalArgumentException.class, () -> new Triangle(1, -1));
    }

    @Test
    @DisplayName("传统 if-else vs switch 模式匹配面积对比")
    void testComputeAreaTraditional() {
        Map<String, Object> result = service.computeAreaTraditional(new Circle(5));

        assertTrue(result.containsKey("traditional"));
        assertTrue(result.containsKey("patternMatching"));
        Map<String, Object> pm = (Map<String, Object>) result.get("patternMatching");
        assertNotNull(pm.get("advantages"));
    }

    @Test
    @DisplayName("传统 if-else 链 vs switch 类型匹配对比")
    void testDescribeTypeTraditional() {
        Map<String, Object> result = service.describeTypeTraditional(42);

        assertTrue(result.containsKey("traditional"));
        assertTrue(result.containsKey("patternMatching"));
    }
}
