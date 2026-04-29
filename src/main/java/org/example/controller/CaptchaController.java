package org.example.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.service.CaptchaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 验证码控制器
 * 提供图形验证码的生成和验证接口
 */
@RestController
@RequestMapping("/api/auth/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 获取验证码
     * <p>
     * 返回Base64编码的PNG图片和验证码key。
     * 前端需要将captchaKey和用户输入的验证码一起提交到登录/注册接口。
     * </p>
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCaptcha() {
        CaptchaService.CaptchaData captcha = captchaService.generateCaptcha();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "captchaKey", captcha.captchaKey(),
                        "captchaImage", captcha.captchaImage(),
                        "expireIn", captcha.expireIn()
                ),
                "message", "验证码获取成功"
        ));
    }

    /**
     * 验证验证码
     * <p>
     * 用于前端手动验证用户输入的验证码是否正确（无需调用登录接口）。
     * </p>
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyCaptcha(
            @Valid @RequestBody CaptchaVerifyRequest request) {
        boolean valid = captchaService.validateCaptcha(
                request.getCaptchaKey(), request.getCaptchaCode());

        if (valid) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "验证码验证通过"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "验证码错误或已过期"
            ));
        }
    }

    @Data
    public static class CaptchaVerifyRequest {
        @NotBlank(message = "验证码key不能为空")
        private String captchaKey;

        @NotBlank(message = "验证码不能为空")
        private String captchaCode;
    }
}
