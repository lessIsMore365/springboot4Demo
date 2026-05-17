package org.example.java21demo;

import lombok.extern.slf4j.Slf4j;
import org.example.java21demo.model.OrderInfo;
import org.example.java21demo.model.WeatherResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.FailedException;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

/**
 * 结构化并发演示 - Java 25 预览特性 (JEP 505)
 * 需要 --enable-preview 编译和运行
 */
@Slf4j
@Service
@SuppressWarnings("preview")
public class StructuredConcurrencyDemoService {

    // ==================== 1. awaitAllSuccessfulOrThrow：并行获取用户和订单 ====================

    public Map<String, Object> fetchUserAndOrders(Long userId) {
        try (var scope = StructuredTaskScope.open()) {
            Subtask<String> userTask = scope.fork(() -> fetchUserName(userId));
            Subtask<List<OrderInfo>> ordersTask = scope.fork(() -> fetchUserOrders(userId));

            scope.join();  // 任一子任务失败则抛出 FailedException

            return Map.of(
                    "userName", userTask.get(),
                    "orders", ordersTask.get(),
                    "totalOrders", ordersTask.get().size(),
                    "mode", "awaitAllSuccessfulOrThrow - 任一任务失败则整体失败"
            );
        } catch (FailedException e) {
            return Map.of("error", "子任务执行失败: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("error", "任务被中断");
        }
    }

    // ==================== 2. anySuccessfulResultOrThrow：多服务竞速查询 ====================

    public Map<String, Object> queryWeatherRace(String city) {
        try (var scope = StructuredTaskScope.open(Joiner.<WeatherResult>anySuccessfulResultOrThrow())) {
            scope.fork(() -> queryWeatherService("气象局A", city, 150));
            scope.fork(() -> queryWeatherService("气象局B", city, 200));
            scope.fork(() -> queryWeatherService("气象局C", city, 100));

            WeatherResult result = scope.join();  // 返回最先成功的结果

            return Map.of(
                    "weather", result,
                    "mode", "anySuccessfulResultOrThrow - 取最先返回的结果，其他任务自动取消"
            );
        } catch (FailedException e) {
            return Map.of("error", "所有服务均失败: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("error", "任务被中断");
        }
    }

    // ==================== 3. 支付+通知并行处理 ====================

    public Map<String, Object> processPayment(Long orderId) {
        try (var scope = StructuredTaskScope.open()) {
            Subtask<String> paymentTask = scope.fork(() -> processPaymentTask(orderId));
            Subtask<String> notifyTask = scope.fork(() -> sendNotification(orderId));

            scope.join();

            return Map.of(
                    "paymentResult", paymentTask.get(),
                    "notificationResult", notifyTask.get(),
                    "mode", "支付和通知并行执行，相互独立但必须都成功"
            );
        } catch (FailedException e) {
            return Map.of("error", "事务失败: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("error", "任务被中断");
        }
    }

    // ==================== 4. 错误处理 ====================

    public Map<String, Object> customErrorHandling() {
        try (var scope = StructuredTaskScope.open()) {
            Subtask<String> task1 = scope.fork(() -> {
                Thread.sleep(50);
                return "正常任务";
            });
            Subtask<String> task2 = scope.fork(() -> {
                Thread.sleep(100);
                if (true) throw new RuntimeException("模拟业务失败");
                return "不会执行";
            });

            scope.join();

            return Map.of("result", task1.get());
        } catch (FailedException e) {
            return Map.of(
                    "error", "业务执行失败，捕获 " + e.getSuppressed().length + " 个抑制异常",
                    "suppressedCount", e.getSuppressed().length,
                    "mode", "join() 抛出 FailedException，可捕获并包装自定义异常",
                    "failedSubtaskException", e.getCause().getMessage()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("error", "任务被中断");
        }
    }

    // ==================== 5. 超时控制 ====================

    public Map<String, Object> timeoutDemo() {
        try (var scope = StructuredTaskScope.open(
                Joiner.<String>anySuccessfulResultOrThrow(),
                config -> config.withTimeout(Duration.ofMillis(100)))) {

            scope.fork(() -> {
                Thread.sleep(500);
                return "正常任务完成";
            });

            scope.join();  // 100ms 超时后抛出 FailedException (cause: TimeoutException)

            return Map.of(
                    "result", "成功",
                    "note", "任务在超时前完成"
            );
        } catch (FailedException e) {
            return Map.of(
                    "result", "超时",
                    "note", "join() 在 " + 100 + "ms 后抛出 FailedException（cause: " +
                            (e.getCause() != null ? e.getCause().getClass().getSimpleName() : "null") + "）",
                    "mode", "超时控制确保不会无限等待"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Map.of("error", "任务被中断");
        }
    }

    // ==================== 6. 传统做法 vs 结构化并发 ====================

    @SuppressWarnings("preview")
    public Map<String, Object> compareTraditionalVsStructured(Long userId) {
        // ---- 传统做法：CompletableFuture.allOf() ----
        Instant oldStart = Instant.now();
        CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> fetchUserName(userId));
        CompletableFuture<List<OrderInfo>> ordersFuture = CompletableFuture.supplyAsync(() -> fetchUserOrders(userId));

        String oldApproachResult;
        try {
            CompletableFuture.allOf(userFuture, ordersFuture).join();
            String userName = userFuture.get();
            List<OrderInfo> orders = ordersFuture.get();
            oldApproachResult = String.format("用户=%s, 订单数=%d", userName, orders.size());
        } catch (Exception e) {
            oldApproachResult = "失败: " + e.getMessage();
        }
        long oldMs = Duration.between(oldStart, Instant.now()).toMillis();

        // ---- 新特性：StructuredTaskScope ----
        Instant newStart = Instant.now();
        String newApproachResult;
        try (var scope = StructuredTaskScope.open()) {
            Subtask<String> userTask = scope.fork(() -> fetchUserName(userId));
            Subtask<List<OrderInfo>> ordersTask = scope.fork(() -> fetchUserOrders(userId));
            scope.join();
            newApproachResult = String.format("用户=%s, 订单数=%d", userTask.get(), ordersTask.get().size());
        } catch (Exception e) {
            newApproachResult = "失败: " + e.getMessage();
        }
        long newMs = Duration.between(newStart, Instant.now()).toMillis();

        return Map.of(
                "traditional", Map.of(
                        "approach", "CompletableFuture.allOf() + 手动 join/get",
                        "code", "CompletableFuture.supplyAsync(task1); CompletableFuture.supplyAsync(task2);\n"
                                + "CompletableFuture.allOf(f1, f2).join();\n"
                                + "f1.get(); f2.get(); // 需分别处理异常",
                        "result", oldApproachResult,
                        "elapsedMs", oldMs
                ),
                "structuredConcurrency", Map.of(
                        "approach", "StructuredTaskScope.open() (awaitAllSuccessfulOrThrow)",
                        "code", "try (var scope = StructuredTaskScope.open()) {\n"
                                + "    Subtask<T> t1 = scope.fork(task1);\n"
                                + "    Subtask<T> t2 = scope.fork(task2);\n"
                                + "    scope.join(); // 失败自动抛出 FailedException\n"
                                + "    t1.get(); t2.get();\n"
                                + "} // 自动取消未完成的任务",
                        "result", newApproachResult,
                        "elapsedMs", newMs
                ),
                "keyDifferences", List.of(
                        "结构化并发: try-with-resources 自动清理 → 无资源泄漏",
                        "结构化并发: 任一失败 → 其他任务自动取消",
                        "传统做法: 需手动处理异常和取消 → 容易遗漏",
                        "结构化并发: 父子任务关系清晰，调用栈可追踪"
                )
        );
    }

    @SuppressWarnings("preview")
    public Map<String, Object> compareRaceTraditionalVsStructured(String city) {
        // ---- 传统做法：CompletableFuture.anyOf() ----
        Instant oldStart = Instant.now();
        CompletableFuture<WeatherResult> cf1 = CompletableFuture.supplyAsync(() -> queryWeatherService("A", city, 150));
        CompletableFuture<WeatherResult> cf2 = CompletableFuture.supplyAsync(() -> queryWeatherService("B", city, 200));
        CompletableFuture<WeatherResult> cf3 = CompletableFuture.supplyAsync(() -> queryWeatherService("C", city, 100));

        String oldResult;
        try {
            WeatherResult r = (WeatherResult) CompletableFuture.anyOf(cf1, cf2, cf3).join();
            oldResult = r.source() + " 最先返回";
            cf1.cancel(true);
            cf2.cancel(true);
            cf3.cancel(true);
        } catch (Exception e) {
            oldResult = "失败: " + e.getMessage();
        }
        long oldMs = Duration.between(oldStart, Instant.now()).toMillis();

        // ---- 新特性：anySuccessfulResultOrThrow ----
        Instant newStart = Instant.now();
        String newResult;
        try (var scope = StructuredTaskScope.open(Joiner.<WeatherResult>anySuccessfulResultOrThrow())) {
            scope.fork(() -> queryWeatherService("A", city, 150));
            scope.fork(() -> queryWeatherService("B", city, 200));
            scope.fork(() -> queryWeatherService("C", city, 100));
            WeatherResult r = scope.join();
            newResult = r.source() + " 最先返回";
        } catch (Exception e) {
            newResult = "失败: " + e.getMessage();
        }
        long newMs = Duration.between(newStart, Instant.now()).toMillis();

        return Map.of(
                "traditional", Map.of(
                        "approach", "CompletableFuture.anyOf() + 手动 cancel",
                        "code", "var f1 = CompletableFuture.supplyAsync(service1);\n"
                                + "var f2 = CompletableFuture.supplyAsync(service2);\n"
                                + "WeatherResult r = (WeatherResult) CompletableFuture.anyOf(f1, f2).join();\n"
                                + "f1.cancel(true); f2.cancel(true); // 必须手动取消！",
                        "result", oldResult,
                        "elapsedMs", oldMs
                ),
                "structuredConcurrency", Map.of(
                        "approach", "StructuredTaskScope.open(Joiner.anySuccessfulResultOrThrow())",
                        "code", "try (var scope = StructuredTaskScope.open(Joiner.<T>anySuccessfulResultOrThrow())) {\n"
                                + "    scope.fork(service1);\n"
                                + "    scope.fork(service2);\n"
                                + "    T result = scope.join();\n"
                                + "} // 自动取消其他任务",
                        "result", newResult,
                        "elapsedMs", newMs
                ),
                "keyDifferences", List.of(
                        "anyOf() 返回 Object，需强制转型 → 结构化并发类型安全",
                        "anyOf() 其余任务继续运行 → 结构化并发自动取消",
                        "anyOf() 异常处理分散 → 结构化并发统一处理"
                )
        );
    }

    // ==================== 模拟的远程服务方法 ====================

    private String fetchUserName(Long userId) {
        log.info("获取用户信息 - userId={}, thread={}", userId, Thread.currentThread());
        sleepSafely(80);
        return "用户#" + userId;
    }

    private List<OrderInfo> fetchUserOrders(Long userId) {
        log.info("获取用户订单 - userId={}, thread={}", userId, Thread.currentThread());
        sleepSafely(120);
        return List.of(
                new OrderInfo(1001L, new BigDecimal("99.90"), "已完成", LocalDateTime.now().minusDays(1)),
                new OrderInfo(1002L, new BigDecimal("199.00"), "待发货", LocalDateTime.now().minusHours(5))
        );
    }

    private WeatherResult queryWeatherService(String source, String city, long delayMs) {
        log.info("查询天气: source={}, city={}, delay={}ms", source, city, delayMs);
        sleepSafely(delayMs);
        return new WeatherResult(source, city, 25, "晴");
    }

    private String processPaymentTask(Long orderId) {
        log.info("处理支付 - orderId={}", orderId);
        sleepSafely(100);
        return "支付成功, 交易号: TXN" + System.currentTimeMillis();
    }

    private String sendNotification(Long orderId) {
        log.info("发送通知 - orderId={}", orderId);
        sleepSafely(50);
        return "通知已发送";
    }

    private void sleepSafely(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
