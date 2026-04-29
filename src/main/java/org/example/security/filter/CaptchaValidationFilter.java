package org.example.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.CaptchaService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * 验证码验证过滤器
 * 拦截OAuth2令牌请求（password模式），验证验证码
 */
@Slf4j
@Component
public class CaptchaValidationFilter extends OncePerRequestFilter {

    private final CaptchaService captchaService;
    private final ObjectMapper objectMapper;

    public CaptchaValidationFilter(CaptchaService captchaService) {
        this.captchaService = captchaService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 只拦截 POST /oauth2/token 请求
        if (!"/oauth2/token".equals(request.getRequestURI())
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取grant_type参数
        String grantType = request.getParameter("grant_type");
        // 只拦截password模式的令牌请求
        if (!"password".equals(grantType)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取验证码参数
        String captchaKey = request.getParameter("captcha_key");
        String captchaCode = request.getParameter("captcha_code");

        log.info("验证码验证 - captchaKey: {}, captchaCode: {}, URI: {}",
                captchaKey, captchaCode, request.getRequestURI());

        // 验证码验证
        if (captchaKey == null || captchaCode == null
                || !captchaService.validateCaptcha(captchaKey, captchaCode)) {
            log.warn("验证码验证失败 - captchaKey: {}, captchaCode: {}", captchaKey, captchaCode);
            sendErrorResponse(response, "验证码错误或已过期");
            return;
        }

        log.info("验证码验证通过");
        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorBody = Map.of(
                "error", "invalid_captcha",
                "error_description", message
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
