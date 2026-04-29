package org.example.service;

/**
 * 验证码服务接口
 * 提供图形验证码生成和验证功能
 */
public interface CaptchaService {

    /**
     * 生成验证码
     * @return 验证码数据（含key、base64图片、过期时间）
     */
    CaptchaData generateCaptcha();

    /**
     * 验证验证码
     * @param captchaKey 验证码key
     * @param captchaCode 用户输入的验证码
     * @return 是否验证通过（验证后自动删除，一次性使用）
     */
    boolean validateCaptcha(String captchaKey, String captchaCode);

    /**
     * 验证码数据类型
     */
    record CaptchaData(
            String captchaKey,
            String captchaImage,
            long expireIn
    ) {}
}
