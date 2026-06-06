package org.example.config;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import org.example.common.DataScopeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * 数据权限处理器 — 根据 DataScopeHelper 上下文为 sys_user 查询追加 WHERE 条件
 */
public class DataScopeHandler implements DataPermissionHandler {

    private static final Logger log = LoggerFactory.getLogger(DataScopeHandler.class);

    private static final String USER_MAPPER_PREFIX = "org.example.mapper.UserMapper.";

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        if (mappedStatementId == null || !mappedStatementId.startsWith(USER_MAPPER_PREFIX)) {
            return where;
        }

        DataScopeHelper.DataScopeContext ctx = DataScopeHelper.get();
        if (ctx == null || ctx.skipFiltering()) {
            return where;
        }

        Expression extra = switch (ctx.scope()) {
            case 3 -> buildDeptFilter(ctx.deptId());
            case 4 -> buildDeptAndChildrenFilter(ctx.deptAndChildIds());
            case 5 -> buildSelfFilter(ctx.userId());
            default -> null;
        };

        if (extra == null) {
            return where;
        }

        log.debug("Data scope applied: scope={}, mappedStatementId={}", ctx.scope(), mappedStatementId);
        if (where == null) {
            return extra;
        }
        return new AndExpression(where, extra);
    }

    private Expression buildDeptFilter(Long deptId) {
        if (deptId == null) return null;
        return new EqualsTo(new Column("dept_id"), new LongValue(deptId));
    }

    private Expression buildDeptAndChildrenFilter(java.util.Set<Long> ids) {
        if (ids == null || ids.isEmpty()) return null;
        ExpressionList<Expression> list = new ExpressionList<>(
                ids.stream().map(LongValue::new).collect(Collectors.toList()));
        return new InExpression(new Column("dept_id"), list);
    }

    private Expression buildSelfFilter(Long userId) {
        if (userId == null) return null;
        return new EqualsTo(new Column("id"), new LongValue(userId));
    }
}
