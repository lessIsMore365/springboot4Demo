package org.example.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.example.annotation.Log;
import org.example.entity.SysOperLog;
import org.example.mapper.SysOperLogMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 操作日志 AOP 切面，拦截 @Log 注解并记录操作日志。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysOperLogMapper operLogMapper;
    private static final ObjectMapper mapper = new ObjectMapper();

    /** 环绕通知：记录操作耗时、参数、结果、异常 */
    @Around("@annotation(logAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Log logAnnotation) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperLog operLog = new SysOperLog();
        operLog.setCreateTime(LocalDateTime.now());
        operLog.setStatus(0);

        try {
            // 请求信息
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                operLog.setOperUrl(request.getRequestURI());
                operLog.setOperIp(getClientIp(request));
                operLog.setRequestMethod(request.getMethod());
            }

            // 用户信息
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                operLog.setOperName(auth.getName());
            }

            // 注解信息
            operLog.setTitle(logAnnotation.title());
            operLog.setBusinessType(logAnnotation.businessType().name());
            operLog.setOperatorType(logAnnotation.operatorType().name());

            // 方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String className = signature.getDeclaringType().getSimpleName();
            operLog.setMethod(className + "." + signature.getName());

            // 请求参数
            if (logAnnotation.saveRequestData()) {
                operLog.setOperParam(truncate(toJson(joinPoint.getArgs()), 2000));
            }

            // 执行目标方法
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            operLog.setCostTime(cost);

            // 返回结果
            if (logAnnotation.saveResponseData()) {
                operLog.setJsonResult(truncate(toJson(result), 2000));
            }

            // 异步保存，不阻塞请求
            CompletableFuture.runAsync(() -> {
                try { operLogMapper.insert(operLog); } catch (Exception e) {
                    log.debug("保存操作日志失败: {}", e.getMessage());
                }
            });

            return result;
        } catch (Throwable e) {
            operLog.setStatus(1);
            operLog.setErrorMsg(truncate(e.getMessage(), 500));
            operLog.setCostTime(System.currentTimeMillis() - start);
            CompletableFuture.runAsync(() -> {
                try { operLogMapper.insert(operLog); } catch (Exception ex) {
                    log.debug("保存操作日志失败: {}", ex.getMessage());
                }
            });
            throw e;
        }
    }

    private String toJson(Object obj) {
        try { return mapper.writeValueAsString(obj); } catch (Exception e) { return String.valueOf(obj); }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null && ip.contains(",") ? ip.split(",")[0].trim() : ip;
    }
}
