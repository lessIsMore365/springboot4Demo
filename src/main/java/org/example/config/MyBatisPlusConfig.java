package org.example.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.time.LocalDateTime;

/**
 * MyBatis Plus 配置类
 * 配置分页插件、乐观锁插件和自动填充
 */
@Configuration
@EnableTransactionManagement
public class MyBatisPlusConfig {

    /**
     * 配置MyBatis Plus拦截器
     * 包含分页插件和乐观锁插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 数据权限拦截器（基于 data_scope + dept_id 自动过滤 sys_user 查询）
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new DataScopeHandler()));
        // 添加乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    /**
     * 自动填充处理器
     * 自动设置createTime和updateTime字段
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 插入时自动填充
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                // 设置创建时间
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                // 设置更新时间
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

                // 设置逻辑删除默认值（如果字段存在）
                if (metaObject.hasSetter("deleted")) {
                    this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
                }

                // 设置版本号默认值（如果字段存在）
                if (metaObject.hasSetter("version")) {
                    this.strictInsertFill(metaObject, "version", Integer.class, 1);
                }
            }

            /**
             * 更新时自动填充
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                // 设置更新时间
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }

    /**
     * 慢 SQL 拦截器 Bean
     * 同时作为 MyBatis Plugin 注册，并供监控服务注入查询统计
     */
    @Bean
    public SlowSqlInterceptor slowSqlInterceptor() {
        return new SlowSqlInterceptor();
    }

    /**
     * 配置SqlSessionFactory
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource,
                                                SlowSqlInterceptor slowSqlInterceptor,
                                                MybatisPlusInterceptor mybatisPlusInterceptor) throws Exception {
        MybatisSqlSessionFactoryBean sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setPlugins(slowSqlInterceptor, mybatisPlusInterceptor);
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/**/*.xml"));
        return sessionFactory.getObject();
    }
}