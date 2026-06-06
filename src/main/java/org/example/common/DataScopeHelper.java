package org.example.common;

import java.util.Set;

/**
 * 数据权限上下文持有者 — ThreadLocal 隔离
 */
public final class DataScopeHelper {

    private DataScopeHelper() {}

    private static final ThreadLocal<DataScopeContext> CONTEXT = new ThreadLocal<>();

    public static void set(DataScopeContext ctx) {
        CONTEXT.set(ctx);
    }

    public static DataScopeContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * @param scope           有效数据范围 1-5
     * @param userId          当前用户 ID
     * @param deptId          当前用户部门 ID（scope 3/4 使用）
     * @param deptAndChildIds 部门及子部门 ID 集合（scope 4 使用）
     */
    public record DataScopeContext(int scope, Long userId, Long deptId, Set<Long> deptAndChildIds) {
        public boolean skipFiltering() {
            return scope == 1;
        }
    }
}
