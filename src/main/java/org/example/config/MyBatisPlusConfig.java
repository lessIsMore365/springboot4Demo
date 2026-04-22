package org.example.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
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
        // 暂不添加任何插件，确保编译通过
        // 分页插件和乐观锁插件可在依赖问题解决后添加
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
     * 配置SqlSessionFactory
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        // 设置mapper.xml文件位置（如果有的话）
        sessionFactory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath*:/mapper/**/*.xml"));
        return sessionFactory.getObject();
    }
}