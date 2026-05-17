package org.example.java21demo;

import org.example.java21demo.model.WeatherResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.FailedException;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.StructuredTaskScope.Subtask;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("preview")
@DisplayName("结构化并发演示测试")
class StructuredConcurrencyDemoTest {

    private final StructuredConcurrencyDemoService service = new StructuredConcurrencyDemoService();

    // ==================== awaitAllSuccessfulOrThrow 测试 ====================

    @Test
    @DisplayName("awaitAllSuccessfulOrThrow: 并行任务成功时返回全部结果")
    void testAwaitAllSuccessfulOrThrowSuccess() {
        Map<String, Object> result = service.fetchUserAndOrders(1L);

        assertNotNull(result.get("userName"));
        assertEquals("用户#1", result.get("userName"));
        assertNotNull(result.get("orders"));
        assertEquals(2, result.get("totalOrders"));
        assertTrue(result.toString().contains("awaitAllSuccessfulOrThrow"));
    }

    @Test
    @DisplayName("awaitAllSuccessfulOrThrow: fork 返回 Subtask 而非 Future")
    void testForkReturnsSubtask() {
        try (var scope = StructuredTaskScope.open()) {
            Subtask<String> subtask = scope.fork(() -> "hello");

            scope.join();

            String result = subtask.get();
            assertEquals("hello", result);

            assertEquals(Subtask.State.SUCCESS, subtask.state());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== anySuccessfulResultOrThrow 测试 ====================

    @Test
    @DisplayName("anySuccessfulResultOrThrow: 返回第一个成功的任务结果")
    void testAnySuccessfulResultOrThrow() {
        Map<String, Object> result = service.queryWeatherRace("北京");

        assertNotNull(result.get("weather"));
        WeatherResult weather = (WeatherResult) result.get("weather");
        assertEquals("北京", weather.city());
        assertTrue(result.toString().contains("anySuccessfulResultOrThrow"));
    }

    @Test
    @DisplayName("anySuccessfulResultOrThrow: scope.join() 返回第一个完成的结果")
    void testAnySuccessfulResultOrThrowFirstWins() {
        try (var scope = StructuredTaskScope.open(Joiner.<String>anySuccessfulResultOrThrow())) {
            scope.fork(() -> {
                Thread.sleep(200);
                return "慢任务";
            });
            scope.fork(() -> {
                Thread.sleep(10);
                return "快任务";
            });

            String result = scope.join();

            assertEquals("快任务", result,
                    "anySuccessfulResultOrThrow 应返回最先完成的任务结果");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== 支付处理测试 ====================

    @Test
    @DisplayName("支付+通知并行处理")
    void testProcessPayment() {
        Map<String, Object> result = service.processPayment(1001L);

        assertTrue(result.containsKey("paymentResult"));
        assertTrue(result.containsKey("notificationResult"));
    }

    // ==================== 超时测试 ====================

    @Test
    @DisplayName("超时控制：open(joiner, config.withTimeout) 超时后抛出 FailedException")
    void testTimeout() {
        Map<String, Object> result = service.timeoutDemo();

        assertTrue(result.containsKey("result"));
        assertTrue(result.containsKey("note"));
    }

    // ==================== 错误处理测试 ====================

    @Test
    @DisplayName("自定义错误处理：catch FailedException 自定义异常包装")
    void testCustomErrorHandling() {
        Map<String, Object> result = service.customErrorHandling();

        assertTrue(result.containsKey("error"));
        assertNotNull(result.get("suppressedCount"));
    }

    // ==================== Subtask 状态测试 ====================

    @Test
    @DisplayName("Subtask.state() 在不同阶段的正确状态")
    void testSubtaskState() {
        try (var scope = StructuredTaskScope.open()) {
            Subtask<Integer> successTask = scope.fork(() -> 42);
            Subtask<String> failTask = scope.fork(() -> {
                throw new RuntimeException("预期失败");
            });

            scope.join();

            assertEquals(Subtask.State.SUCCESS, successTask.state());
            assertEquals(42, successTask.get());

            assertEquals(Subtask.State.FAILED, failTask.state());
            assertNotNull(failTask.exception());

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== 结构化保证测试 ====================

    @Test
    @DisplayName("scope 关闭后确保所有子任务已终止")
    void testScopeGuaranteesTermination() throws Exception {
        List<Thread> captured = new java.util.concurrent.CopyOnWriteArrayList<>();

        try (var scope = StructuredTaskScope.open()) {
            scope.fork(() -> {
                captured.add(Thread.currentThread());
                return "task1";
            });
            scope.fork(() -> {
                captured.add(Thread.currentThread());
                return "task2";
            });

            scope.join();
        }

        for (Thread t : captured) {
            assertFalse(t.isAlive(),
                    "scope 关闭后子任务线程应已终止: " + t.getName());
        }
    }

    @Test
    @DisplayName("传统 CompletableFuture.allOf vs StructuredTaskScope 对比")
    void testCompareTraditionalVsStructured() {
        Map<String, Object> result = service.compareTraditionalVsStructured(1L);

        assertTrue(result.containsKey("traditional"));
        assertTrue(result.containsKey("structuredConcurrency"));
        assertTrue(result.containsKey("keyDifferences"));
    }

    @Test
    @DisplayName("传统 CompletableFuture.anyOf vs anySuccessfulResultOrThrow 对比")
    void testCompareRaceTraditionalVsStructured() {
        Map<String, Object> result = service.compareRaceTraditionalVsStructured("北京");

        assertTrue(result.containsKey("traditional"));
        assertTrue(result.containsKey("structuredConcurrency"));
    }
}
