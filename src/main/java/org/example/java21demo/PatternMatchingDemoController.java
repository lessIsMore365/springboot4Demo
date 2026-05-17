package org.example.java21demo;

import lombok.RequiredArgsConstructor;
import org.example.java21demo.model.Circle;
import org.example.java21demo.model.Rectangle;
import org.example.java21demo.model.Shape;
import org.example.java21demo.model.Triangle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/java21/pattern-matching")
@RequiredArgsConstructor
public class PatternMatchingDemoController {

    private final PatternMatchingDemoService service;

    /**
     * GET /java21/pattern-matching/area?shape=circle&dim1=5
     * Switch + Record Pattern 计算面积
     */
    @GetMapping("/area")
    public Map<String, Object> computeArea(
            @RequestParam(defaultValue = "circle") String shape,
            @RequestParam(defaultValue = "5") double dim1,
            @RequestParam(defaultValue = "3") double dim2) {
        Shape s = switch (shape.toLowerCase()) {
            case "circle"    -> new Circle(dim1);
            case "rectangle" -> new Rectangle(dim1, dim2);
            case "triangle"  -> new Triangle(dim1, dim2);
            default          -> throw new IllegalArgumentException("shape 须为 circle/rectangle/triangle");
        };
        return service.computeArea(s);
    }

    /**
     * GET /java21/pattern-matching/describe?value=hello
     * instanceof 类型模式匹配
     */
    @GetMapping("/describe")
    public Map<String, Object> describe(@RequestParam(defaultValue = "hello") String value) {
        // 尝试解析为不同类型
        Object parsed;
        try { parsed = Integer.parseInt(value); } catch (NumberFormatException e1) {
            try { parsed = Double.parseDouble(value); } catch (NumberFormatException e2) {
                parsed = "true".equalsIgnoreCase(value) ? true :
                         "false".equalsIgnoreCase(value) ? false : value;
            }
        }
        return service.describeType(parsed);
    }

    /**
     * GET /java21/pattern-matching/categorize?shape=circle&dim1=50
     * Guarded Pattern (when 子句) 演示
     */
    @GetMapping("/categorize")
    public Map<String, Object> categorize(
            @RequestParam(defaultValue = "circle") String shape,
            @RequestParam(defaultValue = "5") double dim1,
            @RequestParam(defaultValue = "3") double dim2) {
        Shape s = switch (shape.toLowerCase()) {
            case "circle"    -> new Circle(dim1);
            case "rectangle" -> new Rectangle(dim1, dim2);
            case "triangle"  -> new Triangle(dim1, dim2);
            default          -> throw new IllegalArgumentException("shape 须为 circle/rectangle/triangle");
        };
        return service.categorizeShape(s);
    }

    /**
     * GET /java21/pattern-matching/nested
     * 嵌套 Record Pattern 演示
     */
    @GetMapping("/nested")
    public Map<String, Object> nested() {
        // 构造嵌套结构: ColoredLine(Line(Point(0,0), Point(3,4)), "Red")
        var point1 = new PatternMatchingDemoService.Point(0, 0);
        var point2 = new PatternMatchingDemoService.Point(3, 4);
        var line = new PatternMatchingDemoService.Line(point1, point2);
        var coloredLine = new PatternMatchingDemoService.ColoredLine(line, "红色");

        return service.nestedPatternDemo(coloredLine);
    }

    /**
     * GET /java21/pattern-matching/api-response?type=success
     * Sealed interface + Record Pattern 处理 API 响应
     */
    @GetMapping("/api-response")
    public Map<String, Object> apiResponse(@RequestParam(defaultValue = "success") String type) {
        PatternMatchingDemoService.ApiResponse response;
        if ("success".equals(type)) {
            response = new PatternMatchingDemoService.SuccessResponse(
                    Map.of("id", 1, "name", "test"), "操作成功");
        } else {
            response = new PatternMatchingDemoService.ErrorResponse(
                    404, "资源未找到", java.util.List.of("ID 不存在", "请检查输入"));
        }
        return service.handleApiResponse(response);
    }

    /**
     * GET /java21/pattern-matching/unnamed?shape=circle&dim1=10
     * Unnamed Pattern (_) 演示
     */
    @GetMapping("/unnamed")
    public Map<String, Object> unnamed(
            @RequestParam(defaultValue = "circle") String shape,
            @RequestParam(defaultValue = "10") double dim1,
            @RequestParam(defaultValue = "5") double dim2) {
        Shape s = switch (shape.toLowerCase()) {
            case "circle"    -> new Circle(dim1);
            case "rectangle" -> new Rectangle(dim1, dim2);
            case "triangle"  -> new Triangle(dim1, dim2);
            default          -> throw new IllegalArgumentException("shape 须为 circle/rectangle/triangle");
        };
        return service.unnamedPatternDemo(s);
    }

    /**
     * GET /java21/pattern-matching/compare-area?shape=circle&dim1=5
     * 传统 if-else vs switch 模式匹配计算面积
     */
    @GetMapping("/compare-area")
    public Map<String, Object> compareArea(
            @RequestParam(defaultValue = "circle") String shape,
            @RequestParam(defaultValue = "5") double dim1,
            @RequestParam(defaultValue = "3") double dim2) {
        Shape s = switch (shape.toLowerCase()) {
            case "circle"    -> new Circle(dim1);
            case "rectangle" -> new Rectangle(dim1, dim2);
            case "triangle"  -> new Triangle(dim1, dim2);
            default          -> throw new IllegalArgumentException("shape 须为 circle/rectangle/triangle");
        };
        return service.computeAreaTraditional(s);
    }

    /**
     * GET /java21/pattern-matching/compare-describe?value=hello
     * 传统 if-else 链 vs switch 类型模式匹配
     */
    @GetMapping("/compare-describe")
    public Map<String, Object> compareDescribe(@RequestParam(defaultValue = "hello") String value) {
        Object parsed;
        try { parsed = Integer.parseInt(value); } catch (NumberFormatException e1) {
            try { parsed = Double.parseDouble(value); } catch (NumberFormatException e2) {
                parsed = "true".equalsIgnoreCase(value) ? true :
                         "false".equalsIgnoreCase(value) ? false : value;
            }
        }
        return service.describeTypeTraditional(parsed);
    }

    /**
     * GET /java21/pattern-matching/compare-api-response?type=error
     * 传统 if-else/Visitor vs switch record pattern 处理响应
     */
    @GetMapping("/compare-api-response")
    public Map<String, Object> compareApiResponse(@RequestParam(defaultValue = "success") String type) {
        PatternMatchingDemoService.ApiResponse response;
        if ("success".equals(type)) {
            response = new PatternMatchingDemoService.SuccessResponse(
                    Map.of("id", 1, "name", "test"), "操作成功");
        } else {
            response = new PatternMatchingDemoService.ErrorResponse(
                    404, "资源未找到", java.util.List.of("ID 不存在", "请检查输入"));
        }
        return service.handleApiResponseTraditional(response);
    }
}
