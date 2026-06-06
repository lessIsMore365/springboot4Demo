package org.example.config;

import org.example.mapper.RoleMapper;
import org.example.mapper.SysDeptMapper;
import org.example.mapper.UserMapper;
import org.example.mapper.UserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    private final JsonMapper jsonMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    public WebMvcConfig(JsonMapper jsonMapper, UserMapper userMapper,
                        UserRoleMapper userRoleMapper, RoleMapper roleMapper,
                        SysDeptMapper deptMapper) {
        this.jsonMapper = jsonMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(
                new DataScopeInterceptor(userMapper, userRoleMapper, roleMapper, deptMapper))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/register", "/api/auth/captcha/**",
                        "/api/auth/health", "/api/payment/notify/**");
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof JacksonJsonHttpMessageConverter) {
                log.info("Replacing JacksonJsonHttpMessageConverter with custom JsonMapper");
                converters.set(i, new JacksonJsonHttpMessageConverter(jsonMapper));
            }
        }
    }
}
