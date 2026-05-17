package org.example.java21demo;

import org.example.java21demo.model.UserContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("preview")
@DisplayName("Scoped Value 演示测试")
class ScopedValueDemoTest {

    private final ScopedValueDemoService service = new ScopedValueDemoService();

    // ==================== 基本用法测试 ====================

    @Test
    @DisplayName("ScopedValue.where().call() 内可获取绑定值")
    void testScopedValueBasicBinding() {
        UserContext user = new UserContext(1L, "test", "ROLE_USER");
        UserContext retrieved = ScopedValue.where(
                ScopedValueDemoService.CURRENT_USER, user
        ).call(() -> ScopedValueDemoService.CURRENT_USER.get());

        assertEquals(user, retrieved);
    }

    @Test
    @DisplayName("离开作用域后 isBound 为 false")
    void testScopedValueUnboundOutsideScope() {
        ScopedValue.where(ScopedValueDemoService.CURRENT_USER,
                new UserContext(1L, "test", "ROLE_USER")
        ).run(() -> {
            assertTrue(ScopedValueDemoService.CURRENT_USER.isBound());
        });

        // 离开作用域
        assertFalse(ScopedValueDemoService.CURRENT_USER.isBound(),
                "离开 where().run() 后应自动解绑");
    }

    @Test
    @DisplayName("嵌套作用域内层覆盖外层")
    void testNestedScopedValue() {
        ScopedValue.where(ScopedValueDemoService.CURRENT_USER,
                new UserContext(1L, "outer", "ROLE_USER")
        ).run(() -> {
            assertEquals("outer", ScopedValueDemoService.CURRENT_USER.get().username());

            ScopedValue.where(ScopedValueDemoService.CURRENT_USER,
                    new UserContext(2L, "inner", "ROLE_ADMIN")
            ).run(() -> {
                assertEquals("inner", ScopedValueDemoService.CURRENT_USER.get().username());
            });

            assertEquals("outer", ScopedValueDemoService.CURRENT_USER.get().username(),
                    "离开嵌套作用域后应恢复外层值");
        });
    }

    // ==================== orElse / orElseThrow 测试 ====================

    @Test
    @DisplayName("orElse 在未绑定时返回默认值")
    void testOrElseReturnsDefault() {
        UserContext defaultUser = ScopedValueDemoService.CURRENT_USER.orElse(
                new UserContext(0L, "anonymous", "ROLE_GUEST")
        );

        assertEquals("anonymous", defaultUser.username());
        assertEquals(0L, defaultUser.userId());
    }

    @Test
    @DisplayName("orElseThrow 在未绑定时抛出异常")
    void testOrElseThrowThrows() {
        assertThrows(IllegalStateException.class, () -> {
            ScopedValueDemoService.CURRENT_USER.orElseThrow(
                    () -> new IllegalStateException("用户未登录")
            );
        });
    }

    @Test
    @DisplayName("orElse 在已绑定时返回绑定值")
    void testOrElseReturnsBoundValue() {
        UserContext bound = new UserContext(5L, "bound", "ROLE_USER");
        UserContext result = ScopedValue.where(ScopedValueDemoService.CURRENT_USER, bound)
                .call(() -> ScopedValueDemoService.CURRENT_USER.orElse(
                        new UserContext(0L, "default", "ROLE_GUEST")
                ));

        assertEquals(bound, result);
    }

    // ==================== 服务方法测试 ====================

    @Test
    @DisplayName("basicUsage 返回绑定用户信息")
    void testServiceBasicUsage() {
        Map<String, Object> result = service.basicUsage();

        assertTrue((boolean) result.get("isBound"));
        assertEquals(1000L, result.get("boundUserId"));
        assertEquals("admin", result.get("boundUsername"));
    }

    @Test
    @DisplayName("scopeIsolation 离开作用域后解绑")
    void testServiceScopeIsolation() {
        Map<String, Object> result = service.scopeIsolation();

        assertFalse((boolean) result.get("outsideBound"));
    }

    @Test
    @DisplayName("requestContextDemo 深层调用链可访问 ScopedValue")
    void testServiceRequestContext() {
        UserContext user = new UserContext(42L, "demo", "ROLE_USER");
        Map<String, Object> result = service.requestContextDemo(user);

        assertNotNull(result.get("serviceLayerResult"));
        assertNotNull(result.get("repositoryLayerResult"));
        assertTrue(result.get("traceId").toString().startsWith("trace-"));
    }

    @Test
    @DisplayName("multipleBindings 同时绑定多个 ScopedValue")
    void testServiceMultipleBindings() {
        Map<String, Object> result = service.multipleBindings();

        assertNotNull(result.get("user"));
        assertNotNull(result.get("traceId"));
        assertEquals("multi-trace-001", result.get("traceId"));
    }

    // ==================== ThreadLocal 对比测试 ====================

    @Test
    @DisplayName("ScopedValue vs ThreadLocal 对比")
    void testCompareWithThreadLocal() {
        Map<String, Object> result = service.compareWithThreadLocal();

        assertTrue(result.containsKey("scopedValueReadNs"));
        assertTrue(result.containsKey("threadLocalReadNs"));
        assertNotNull(result.get("note"));
    }

    // ==================== ScopedValue 常量测试 ====================

    @Test
    @DisplayName("ScopedValue.newInstance() 创建唯一实例")
    void testNewInstanceCreatesUnique() {
        ScopedValue<String> sv1 = ScopedValue.newInstance();
        ScopedValue<String> sv2 = ScopedValue.newInstance();

        assertNotSame(sv1, sv2, "每次 newInstance() 应返回不同实例");
    }

    @Test
    @DisplayName("传统 ThreadLocal vs ScopedValue 深度对比")
    void testCompareTraditionalVsScoped() {
        Map<String, Object> result = service.compareTraditionalVsScoped();

        assertTrue(result.containsKey("threadLocal"));
        assertTrue(result.containsKey("scopedValue"));
        assertTrue(result.containsKey("keyDifferences"));
        assertTrue(result.get("keyDifferences").toString().contains("泄漏"));
    }
}
