package org.example.java21demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/java21/structured-concurrency")
@RequiredArgsConstructor
public class StructuredConcurrencyDemoController {

    private final StructuredConcurrencyDemoService service;

    /**
     * GET /java21/structured-concurrency/user-orders?userId=1
     * ShutdownOnFailure 演示：并行获取用户信息和订单
     */
    @GetMapping("/user-orders")
    public Map<String, Object> fetchUserAndOrders(@RequestParam(defaultValue = "1") Long userId) {
        return service.fetchUserAndOrders(userId);
    }

    /**
     * GET /java21/structured-concurrency/weather?city=北京
     * ShutdownOnSuccess 演示：多服务竞速返回最先结果
     */
    @GetMapping("/weather")
    public Map<String, Object> queryWeather(@RequestParam(defaultValue = "北京") String city) {
        return service.queryWeatherRace(city);
    }

    /**
     * GET /java21/structured-concurrency/payment?orderId=1001
     * 支付+通知并行处理
     */
    @GetMapping("/payment")
    public Map<String, Object> processPayment(@RequestParam(defaultValue = "1001") Long orderId) {
        return service.processPayment(orderId);
    }

    /**
     * GET /java21/structured-concurrency/error-handling
     * 自定义错误处理演示（含 handleComplete 回调）
     */
    @GetMapping("/error-handling")
    public Map<String, Object> errorHandling() {
        return service.customErrorHandling();
    }

    /**
     * GET /java21/structured-concurrency/timeout
     * joinUntil 超时控制演示
     */
    @GetMapping("/timeout")
    public Map<String, Object> timeout() {
        return service.timeoutDemo();
    }

    /**
     * GET /java21/structured-concurrency/compare-traditional?userId=1
     * 传统 CompletableFuture vs StructuredTaskScope 深度对比
     */
    @GetMapping("/compare-traditional")
    public Map<String, Object> compareAllOf(@RequestParam(defaultValue = "1") Long userId) {
        return service.compareTraditionalVsStructured(userId);
    }

    /**
     * GET /java21/structured-concurrency/compare-race?city=北京
     * 传统 anyOf vs ShutdownOnSuccess 深度对比
     */
    @GetMapping("/compare-race")
    public Map<String, Object> compareRace(@RequestParam(defaultValue = "北京") String city) {
        return service.compareRaceTraditionalVsStructured(city);
    }
}
