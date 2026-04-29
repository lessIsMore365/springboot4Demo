package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.CaptchaService;
import org.example.service.RedisService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现类
 * 使用Java 2D API生成图形验证码，Redis存储验证码
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisService redisService;

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRE_SECONDS = 300; // 5分钟有效期
    private static final int WIDTH = 130;
    private static final int HEIGHT = 48;
    private static final int CODE_LENGTH = 4;
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final Random RANDOM = new Random();

    @Override
    public CaptchaData generateCaptcha() {
        Thread currentThread = Thread.currentThread();
        log.info("生成验证码 - 当前线程: {}, 是否虚拟线程: {}",
                currentThread, currentThread.isVirtual());

        // 生成随机验证码
        String code = generateCode();

        // 生成验证码图片
        String base64Image = generateCaptchaImage(code);

        // 生成唯一key并存入Redis
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        redisService.setWithExpire(redisKey, code, CAPTCHA_EXPIRE_SECONDS, TimeUnit.SECONDS);

        log.info("验证码生成成功 - key: {}, code: {}", captchaKey, code);

        return new CaptchaData(captchaKey, base64Image, CAPTCHA_EXPIRE_SECONDS);
    }

    @Override
    public boolean validateCaptcha(String captchaKey, String captchaCode) {
        Thread currentThread = Thread.currentThread();
        log.info("验证验证码 - key: {}, 当前线程: {}, 是否虚拟线程: {}",
                captchaKey, currentThread, currentThread.isVirtual());

        if (captchaKey == null || captchaCode == null) {
            log.warn("验证码参数为空");
            return false;
        }

        String redisKey = CAPTCHA_KEY_PREFIX + captchaKey;
        String storedCode = redisService.get(redisKey, String.class);

        if (storedCode == null) {
            log.warn("验证码不存在或已过期 - key: {}", captchaKey);
            return false;
        }

        // 验证后立即删除（一次性使用）
        redisService.delete(redisKey);

        boolean valid = storedCode.equalsIgnoreCase(captchaCode.trim());
        log.info("验证码验证结果: {} - key: {}", valid, captchaKey);
        return valid;
    }

    /**
     * 生成随机验证码字符串
     */
    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    /**
     * 生成验证码图片（Base64编码）
     */
    private String generateCaptchaImage(String code) {
        // 创建BufferedImage
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            // 开启抗锯齿
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 填充背景色（随机浅色）
            Color bgColor = getRandomLightColor();
            g2d.setColor(bgColor);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            // 绘制边框
            g2d.setColor(new Color(180, 180, 180));
            g2d.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

            // 绘制干扰线（2-3条）
            int lineCount = 2 + RANDOM.nextInt(2);
            for (int i = 0; i < lineCount; i++) {
                g2d.setColor(getRandomColor());
                g2d.setStroke(new BasicStroke(1.0f + RANDOM.nextFloat()));
                int x1 = RANDOM.nextInt(WIDTH / 3);
                int y1 = RANDOM.nextInt(HEIGHT);
                int x2 = WIDTH - RANDOM.nextInt(WIDTH / 3);
                int y2 = RANDOM.nextInt(HEIGHT);
                g2d.drawLine(x1, y1, x2, y2);
            }

            // 绘制噪点
            int noiseCount = 30 + RANDOM.nextInt(20);
            for (int i = 0; i < noiseCount; i++) {
                g2d.setColor(getRandomColor());
                int x = RANDOM.nextInt(WIDTH);
                int y = RANDOM.nextInt(HEIGHT);
                g2d.fillRect(x, y, 1, 1);
            }

            // 绘制验证码字符
            char[] chars = code.toCharArray();
            int charWidth = WIDTH / (CODE_LENGTH + 1);
            int charHeight = HEIGHT - 10;

            for (int i = 0; i < chars.length; i++) {
                // 随机字体
                Font font = new Font(
                        getRandomFontName(),
                        Font.BOLD,
                        22 + RANDOM.nextInt(8)
                );
                g2d.setFont(font);

                // 随机颜色
                g2d.setColor(getRandomColor());

                // 随机旋转和偏移
                double angle = (RANDOM.nextDouble() - 0.5) * 0.4; // -0.2 ~ 0.2弧度
                AffineTransform old = g2d.getTransform();

                int x = charWidth * (i + 1) - 5 + RANDOM.nextInt(4);
                int y = charHeight - 3 + RANDOM.nextInt(8);
                g2d.rotate(angle, x, y);
                g2d.drawString(String.valueOf(chars[i]), x, y);

                g2d.setTransform(old);
            }

            // 额外干扰：随机弧线
            g2d.setColor(new Color(150, 150, 150, 80));
            g2d.setStroke(new BasicStroke(0.5f));
            for (int i = 0; i < 2; i++) {
                int x1 = RANDOM.nextInt(WIDTH / 2);
                int y1 = RANDOM.nextInt(HEIGHT);
                int x2 = x1 + RANDOM.nextInt(WIDTH / 2);
                int y2 = y1 + RANDOM.nextInt(HEIGHT / 2) - HEIGHT / 4;
                g2d.drawArc(x1, Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1) * 2,
                        0, 180);
            }

            // 转换Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);

        } catch (Exception e) {
            log.error("生成验证码图片失败", e);
            // 返回一个简单的纯文本Base64编码作为降级
            return createFallbackCaptcha(code);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * 降级方案：生成简化版验证码图片
     */
    private String createFallbackCaptcha(String code) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 28));
            FontMetrics fm = g2d.getFontMetrics();
            int x = (WIDTH - fm.stringWidth(code)) / 2;
            int y = (HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(code, x, y);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e2) {
            log.error("降级方案也失败", e2);
            return "";
        }
    }

    private Color getRandomColor() {
        int r = 50 + RANDOM.nextInt(180);
        int g = 50 + RANDOM.nextInt(180);
        int b = 50 + RANDOM.nextInt(180);
        return new Color(r, g, b);
    }

    private Color getRandomLightColor() {
        int base = 230;
        return new Color(
                base + RANDOM.nextInt(26),
                base + RANDOM.nextInt(26),
                base + RANDOM.nextInt(26)
        );
    }

    private String getRandomFontName() {
        String[] fonts = {"Arial", "Verdana", "Courier New", "Times New Roman", "Georgia"};
        return fonts[RANDOM.nextInt(fonts.length)];
    }
}
