package org.example.service;

import java.util.List;

/**
 * 验证码服务接口
 * 提供点击汉字顺序验证码生成和验证功能
 */
public interface CaptchaService {

    /**
     * 生成验证码
     * @return 验证码数据（含key、base64图片、提示文字、过期时间）
     */
    CaptchaData generateCaptcha();

    /**
     * 文本验证 - 用户输入汉字顺序
     * @param captchaKey 验证码key
     * @param captchaCode 用户输入的汉字序列
     * @return 是否验证通过（验证后自动删除，一次性使用）
     */
    boolean validateCaptcha(String captchaKey, String captchaCode);

    /**
     * 坐标验证 - 用户点击坐标序列
     * @param captchaKey 验证码key
     * @param positions 点击坐标列表（按点击顺序）
     * @return 是否验证通过
     */
    boolean validateCaptchaByPosition(String captchaKey, List<ClickPosition> positions);

    /**
     * 点击坐标
     */
    record ClickPosition(int x, int y) {}

    /**
     * 验证码数据
     */
    record CaptchaData(
            String captchaKey,
            String captchaImage,     // Base64 PNG: data:image/png;base64,...
            String promptText,       // "请依次点击：春天"
            int charCount,           // 目标汉字数
            int imageWidth,          // 图片宽度
            int imageHeight,         // 图片高度
            long expireIn            // 过期秒数
    ) {}
}
