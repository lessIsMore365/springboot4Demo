package org.example.java21demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/java21/virtual-thread")
@RequiredArgsConstructor
public class VirtualThreadDemoController {

    private final VirtualThreadDemoService service;

    /**
     * GET /java21/virtual-thread/info
     * 查看当前请求线程信息（是否为虚拟线程）
     */
    @GetMapping("/info")
    public Map<String, Object> threadInfo() {
        return service.threadInfo();
    }

    /**
     * GET /java21/virtual-thread/create-virtual?count=10000
     * 创建指定数量的虚拟线程
     */
    @GetMapping("/create-virtual")
    public Map<String, Object> createVirtualThreads(@RequestParam(defaultValue = "10000") int count) {
        return service.createVirtualThreads(count);
    }

    /**
     * GET /java21/virtual-thread/create-platform?count=200
     * 创建指定数量的平台线程（对比用，限制 ≤500）
     */
    @GetMapping("/create-platform")
    public Map<String, Object> createPlatformThreads(@RequestParam(defaultValue = "200") int count) {
        return service.createPlatformThreads(count);
    }

    /**
     * GET /java21/virtual-thread/compare?vCount=10000&pCount=200
     * 对比虚拟线程和平台线程的创建速度
     */
    @GetMapping("/compare")
    public Map<String, Object> compare(
            @RequestParam(defaultValue = "10000") int vCount,
            @RequestParam(defaultValue = "200") int pCount) {
        return service.compare(vCount, pCount);
    }

    /**
     * GET /java21/virtual-thread/massive?count=100000
     * 海量虚拟线程测试（默认 100K，最高 100W）
     */
    @GetMapping("/massive")
    public Map<String, Object> massive(@RequestParam(defaultValue = "100000") int count) {
        return service.massiveVirtualThreads(count);
    }

    /**
     * GET /java21/virtual-thread/pinning
     * 检测 synchronized 导致的虚拟线程 pinning
     */
    @GetMapping("/pinning")
    public Map<String, Object> detectPinning() {
        return service.detectPinning();
    }

    /**
     * GET /java21/virtual-thread/async
     * @Async 虚拟线程执行演示
     */
    @GetMapping("/async")
    public CompletableFuture<Map<String, Object>> asyncDemo() {
        return service.asyncDemo();
    }

    /**
     * GET /java21/virtual-thread/builder-api
     * Thread.ofVirtual() 链式 API 演示
     */
    @GetMapping("/builder-api")
    public Map<String, Object> builderApi() {
        return service.threadBuilderApi();
    }

    /**
     * GET /java21/virtual-thread/compare-traditional?taskCount=100&sleepMs=50
     * 传统线程池 vs 虚拟线程深度对比
     */
    @GetMapping("/compare-traditional")
    public Map<String, Object> compareTraditional(
            @RequestParam(defaultValue = "100") int taskCount,
            @RequestParam(defaultValue = "50") int sleepMs) {
        return service.compareThreadPoolVsVirtual(taskCount, sleepMs);
    }
}
