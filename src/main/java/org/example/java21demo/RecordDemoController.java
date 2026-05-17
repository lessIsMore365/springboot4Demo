package org.example.java21demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/java21/record")
@RequiredArgsConstructor
public class RecordDemoController {

    private final RecordDemoService service;

    /**
     * GET /java21/record/dto
     * Record 作为 DTO 演示
     */
    @GetMapping("/dto")
    public Map<String, Object> recordAsDto() {
        return service.recordAsDto();
    }

    /**
     * GET /java21/record/generic
     * 泛型 Record 演示
     */
    @GetMapping("/generic")
    public Map<String, Object> genericRecord() {
        return service.genericRecord();
    }

    /**
     * GET /java21/record/validation
     * Compact constructor 验证演示
     */
    @GetMapping("/validation")
    public Map<String, Object> validation() {
        return service.validationInConstructor();
    }

    /**
     * GET /java21/record/nested
     * 嵌套 Record 演示
     */
    @GetMapping("/nested")
    public Map<String, Object> nested() {
        return service.nestedRecords();
    }

    /**
     * GET /java21/record/streams
     * Record 在 Stream 中的使用
     */
    @GetMapping("/streams")
    public Map<String, Object> streams() {
        return service.recordsInStreams();
    }

    /**
     * GET /java21/record/serialization
     * Record 序列化/反序列化
     */
    @GetMapping("/serialization")
    public Map<String, Object> serialization() throws JsonProcessingException {
        return service.serializationDemo();
    }

    /**
     * GET /java21/record/implements
     * Record 实现接口演示
     */
    @GetMapping("/implements")
    public Map<String, Object> implementsInterface() {
        return service.recordImplementsInterface();
    }

    /**
     * GET /java21/record/methods
     * Record 自定义方法演示
     */
    @GetMapping("/methods")
    public Map<String, Object> withMethods() {
        return service.recordWithMethods();
    }

    /**
     * GET /java21/record/local
     * 方法内 Local Record 演示
     */
    @GetMapping("/local")
    public Map<String, Object> localRecord() {
        return service.localRecordDemo();
    }

    /**
     * GET /java21/record/compare-pojo
     * 传统 POJO vs Record 深度对比
     */
    @GetMapping("/compare-pojo")
    public Map<String, Object> comparePojo() {
        return service.comparePojoVsRecord();
    }
}
