package org.example.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.service.CaptchaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 验证码控制器
 * 提供点击汉字顺序验证码的生成和验证接口
 */
@RestController
@RequestMapping("/api/auth/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 获取点击汉字顺序验证码
     * 返回 Base64 PNG 图片、提示文字、字符数量、图片尺寸
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCaptcha() {
        CaptchaService.CaptchaData captcha = captchaService.generateCaptcha();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "captchaKey", captcha.captchaKey(),
                        "captchaImage", captcha.captchaImage(),
                        "promptText", captcha.promptText(),
                        "charCount", captcha.charCount(),
                        "imageWidth", captcha.imageWidth(),
                        "imageHeight", captcha.imageHeight(),
                        "expireIn", captcha.expireIn()
                ),
                "message", "验证码获取成功"
        ));
    }

    /**
     * 文本验证验证码
     * 用户输入汉字序列进行验证（兼容 OAuth2 密码登录流程）
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

    /**
     * 坐标验证验证码
     * 前端收集用户点击坐标后提交验证
     */
    @PostMapping("/verify-position")
    public ResponseEntity<Map<String, Object>> verifyCaptchaByPosition(
            @Valid @RequestBody CaptchaPositionVerifyRequest request) {

        List<CaptchaService.ClickPosition> positions = request.getPositions().stream()
                .map(p -> new CaptchaService.ClickPosition(p.getX(), p.getY()))
                .toList();

        boolean valid = captchaService.validateCaptchaByPosition(
                request.getCaptchaKey(), positions);

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

    @Data
    public static class CaptchaPositionVerifyRequest {
        @NotBlank(message = "验证码key不能为空")
        private String captchaKey;

        @NotEmpty(message = "点击坐标不能为空")
        private List<ClickPoint> positions;
    }

    @Data
    public static class ClickPoint {
        private int x;
        private int y;
    }
}
