package org.example.java21demo;

import lombok.extern.slf4j.Slf4j;
import org.example.java21demo.model.UserContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Scoped Value 演示 - Java 23 预览特性 (JEP 481)
 * ScopedValue 是 ThreadLocal 的轻量替代，专为虚拟线程设计，不可变、有作用域
 * 需要 --enable-preview 编译和运行
 */
@Slf4j
@Service
@SuppressWarnings("preview")
public class ScopedValueDemoService {

    // 定义 ScopedValue 实例（通常为 static final）
    public static final ScopedValue<UserContext> CURRENT_USER = ScopedValue.newInstance();
    public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    // ==================== 1. 基本用法 ====================

    public Map<String, Object> basicUsage() {
        UserContext user = new UserContext(1000L, "admin", "ROLE_ADMIN");

        // ScopedValue.where().run() 模式：在作用域内值可用，离开作用域后不可用
        return ScopedValue.where(CURRENT_USER, user).call(() -> {
            UserContext bound = CURRENT_USER.get();
            boolean isBound = CURRENT_USER.isBound();

            return Map.of(
                    "boundUserId", bound.userId(),
                    "boundUsername", bound.username(),
                    "isBound", isBound,
                    "pattern", "ScopedValue.where(SV, value).call(() -> { SV.get() })"
            );
        });
    }

    // ==================== 2. 证明作用域隔离 ====================

    public Map<String, Object> scopeIsolation() {
        ScopedValue.where(CURRENT_USER, new UserContext(1L, "user1", "ROLE_USER")).run(() -> {
            log.info("作用域内: CURRENT_USER.isBound={}", CURRENT_USER.isBound());

            // 嵌套作用域 - 内层覆盖外层
            ScopedValue.where(CURRENT_USER, new UserContext(2L, "user2", "ROLE_ADMIN")).run(() -> {
                log.info("嵌套作用域内: user={}", CURRENT_USER.get().username());
            });

            log.info("离开嵌套后: user={}", CURRENT_USER.get().username());
        });

        // 作用域外 - 值不可用
        boolean outsideBound = CURRENT_USER.isBound();

        return Map.of(
                "outsideBound", outsideBound,
                "note", "离开 ScopedValue.where().run() 后，值自动解绑（isBound=false）",
                "nestingNote", "嵌套 where() 会覆盖值，离开嵌套后恢复外层值"
        );
    }

    // ==================== 3. 请求上下文模式（模拟 Web Filter） ====================

    public Map<String, Object> requestContextDemo(UserContext user) {
        return ScopedValue.where(CURRENT_USER, user)
                .where(TRACE_ID, "trace-" + System.currentTimeMillis())
                .call(() -> {
                    // 模拟深层调用链
                    String level1 = processInServiceLayer();
                    String level2 = processInRepositoryLayer();

                    return Map.of(
                            "user", CURRENT_USER.get(),
                            "traceId", TRACE_ID.get(),
                            "serviceLayerResult", level1,
                            "repositoryLayerResult", level2,
                            "pattern", "Filter 设置 ScopedValue → Controller → Service → Repository 任意深度都可访问"
                    );
                });
    }

    private String processInServiceLayer() {
        log.info("Service 层: traceId={}, user={}", TRACE_ID.get(), CURRENT_USER.get().username());
        return "Service 层已处理 (user=" + CURRENT_USER.get().username() + ")";
    }

    private String processInRepositoryLayer() {
        log.info("Repository 层: traceId={}, user={}", TRACE_ID.get(), CURRENT_USER.get().username());
        return "Repository 层已处理 (traceId=" + TRACE_ID.get() + ")";
    }

    // ==================== 4. 与 ThreadLocal 对比 ====================

    private static final ThreadLocal<String> threadLocalUser = new ThreadLocal<>();

    public Map<String, Object> compareWithThreadLocal() {
        threadLocalUser.set("thread-local-user");

        return ScopedValue.where(CURRENT_USER, new UserContext(999L, "scoped-user", "ROLE_USER")).call(() -> {

            long start = System.nanoTime();
            for (int i = 0; i < 100_000; i++) {
                String svUser = CURRENT_USER.get().username();
            }
            long svTime = System.nanoTime() - start;

            start = System.nanoTime();
            for (int i = 0; i < 100_000; i++) {
                String tlUser = threadLocalUser.get();
            }
            long tlTime = System.nanoTime() - start;

            threadLocalUser.remove();  // 必须手动清理！

            return Map.of(
                    "scopedValueReadNs", svTime,
                    "threadLocalReadNs", tlTime,
                    "scopedValueValue", CURRENT_USER.get().username(),
                    "note", "ScopedValue 不可变（安全）、自动清理（无泄漏），" +
                           "ThreadLocal 需手动 remove()（易泄漏）"
            );
        });
    }

    // ==================== 5. orElse / orElseThrow ====================

    public Map<String, Object> fallbackMethods() {
        // 作用域外 - 使用 orElse 提供默认值
        UserContext defaultUser = CURRENT_USER.orElse(
                new UserContext(0L, "anonymous", "ROLE_GUEST")
        );

        // 作用域外 - orElseThrow
        String exceptionMsg;
        try {
            CURRENT_USER.orElseThrow(() -> new IllegalStateException("用户未登录"));
            exceptionMsg = "未抛出";
        } catch (IllegalStateException e) {
            exceptionMsg = e.getMessage();
        }

        return Map.of(
                "orElseDefaultUser", defaultUser,
                "orElseThrowResult", exceptionMsg,
                "isBoundOutside", CURRENT_USER.isBound()
        );
    }

    // ==================== 6. 多个 ScopedValue 绑定 ====================

    public Map<String, Object> multipleBindings() {
        return ScopedValue.where(CURRENT_USER, new UserContext(1L, "admin", "ROLE_ADMIN"))
                .where(TRACE_ID, "multi-trace-001")
                .call(() -> Map.of(
                        "user", CURRENT_USER.get(),
                        "traceId", TRACE_ID.get(),
                        "bindings", "同时绑定2个 ScopedValue，链式 .where().where() 或 where(SV1,v1,SV2,v2)"
                ));
    }

    // ==================== 7. 传统 ThreadLocal vs ScopedValue 深度对比 ====================

    private static final ThreadLocal<String> TL_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> TL_TRACE = new ThreadLocal<>();

    @SuppressWarnings("preview")
    public Map<String, Object> compareTraditionalVsScoped() {
        // ---- 传统做法：ThreadLocal ----
        long tlStart = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            TL_USER.set("user-" + i);
            TL_TRACE.set("trace-" + i);
            String u = TL_USER.get();
            String t = TL_TRACE.get();
            // 模拟使用
        }
        // 必须手动清理！忘记 remove() 会导致内存泄漏
        TL_USER.remove();
        TL_TRACE.remove();
        long tlNs = System.nanoTime() - tlStart;

        // ---- 新特性：ScopedValue ----
        long svStart = System.nanoTime();
        ScopedValue.where(CURRENT_USER, new UserContext(1L, "admin", "ROLE_ADMIN"))
                .where(TRACE_ID, "trace-001")
                .run(() -> {
                    for (int i = 0; i < 100_000; i++) {
                        UserContext u = CURRENT_USER.get();
                        String t = TRACE_ID.get();
                    }
                });
        long svNs = System.nanoTime() - svStart;
        // 无需清理 — 离开作用域自动解绑

        // ---- 内存泄漏演示 ----
        // 模拟：忘记 remove ThreadLocal → 可能导致内存泄漏
        String leakNote;
        try {
            for (int i = 0; i < 100; i++) {
                TL_USER.set("leak-" + i);
                // 忘记 remove() → 如果线程复用（如线程池），值一直存在
            }
            leakNote = "ThreadLocal 值仍然存在: " + TL_USER.get()
                    + "（线程复用会导致旧值残留）";
        } finally {
            TL_USER.remove();
        }

        return Map.of(
                "threadLocal", Map.of(
                        "approach", "ThreadLocal.set()/get() + 手动 remove()",
                        "code", "threadLocal.set(value);\n"
                                + "threadLocal.get();\n"
                                + "threadLocal.remove(); // 必须！",
                        "elapsedNs", tlNs,
                        "leakRisk", leakNote
                ),
                "scopedValue", Map.of(
                        "approach", "ScopedValue.where().run() 自动生命周期",
                        "code", "ScopedValue.where(SV, value).run(() -> {\n"
                                + "    var v = SV.get();\n"
                                + "    // 离开即解绑\n"
                                + "});",
                        "elapsedNs", svNs,
                        "leakRisk", "自动解绑，无泄漏风险"
                ),
                "keyDifferences", List.of(
                        "ThreadLocal: 线程生命周期绑定 → 线程池中易泄漏",
                        "ScopedValue: 词法作用域绑定 → 离开即消失",
                        "ThreadLocal: 可变 (set) → 任意位置可修改 → 难以追踪",
                        "ScopedValue: 不可变 → 只能在 where() 时设置一次",
                        "ThreadLocal: 子线程需 InheritableThreadLocal → 另有成本",
                        "ScopedValue: 虚拟线程友好 → 无 Inheritable 问题"
                )
        );
    }
}
