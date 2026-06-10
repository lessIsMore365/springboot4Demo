package org.example.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.core.RedisKeyNamespace;
import org.example.redis.service.RedisCache;
import org.example.service.CaptchaService;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.*;
import java.util.List;

/**
 * 点击汉字顺序验证码服务实现
 * 在一张图片上随机散布多个汉字，用户需按提示顺序依次点击正确的汉字
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private final RedisCache redisCache;
    private final ObjectMapper objectMapper;

    private static final Duration CAPTCHA_TTL = Duration.ofSeconds(300);
    private static final int IMAGE_WIDTH = 350;
    private static final int IMAGE_HEIGHT = 180;
    private static final int TOP_AREA_HEIGHT = 40;
    private static final int CLICK_TOLERANCE = 40;
    private static final int MIN_CHAR_SIZE = 32;
    private static final int MAX_CHAR_SIZE = 40;
    private static final Random RANDOM = new Random();

    private static final String[] WORD_BANK = {
            "春天", "大海", "蓝天", "星星", "花朵", "阳光", "月亮", "清风",
            "白云", "高山", "流水", "彩虹", "雪花", "森林", "草原", "沙漠",
            "银河", "桃花", "竹林", "秋风", "朝霞", "夕阳", "繁星", "飞鸟",
            "游鱼", "蝴蝶", "蜜蜂", "苹果", "葡萄", "西瓜", "茉莉", "玫瑰"
    };

    private static final String[] DISTRACTOR_CHARS = {
            "木", "石", "火", "土", "金", "玉", "叶", "雨", "露", "霜",
            "雾", "龙", "凤", "鹤", "鹿", "马", "虎", "狼", "鹰", "雁",
            "舟", "桥", "路", "塔", "楼", "亭", "阁", "窗", "门", "灯"
    };

    public CaptchaServiceImpl(RedisCache redisCache) {
        this.redisCache = redisCache;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CaptchaData generateCaptcha() {
        // 随机选词
        String phrase = WORD_BANK[RANDOM.nextInt(WORD_BANK.length)];
        String char1 = String.valueOf(phrase.charAt(0));
        String char2 = String.valueOf(phrase.charAt(1));

        // 随机选 2~3 个干扰汉字
        int distractorCount = 2 + RANDOM.nextInt(2);
        Set<String> used = new HashSet<>(Set.of(char1, char2));
        List<String> distractorChars = new ArrayList<>();
        while (distractorChars.size() < distractorCount) {
            String dc = DISTRACTOR_CHARS[RANDOM.nextInt(DISTRACTOR_CHARS.length)];
            if (used.add(dc)) {
                distractorChars.add(dc);
            }
        }

        // 构建所有字符信息
        List<CharInfo> allChars = new ArrayList<>();
        allChars.add(new CharInfo(char1, true, 0));
        allChars.add(new CharInfo(char2, true, 1));
        for (String dc : distractorChars) {
            allChars.add(new CharInfo(dc, false, -1));
        }
        Collections.shuffle(allChars, RANDOM);

        // 随机布局（避免重叠）
        layoutChars(allChars);

        // 生成图片
        String base64Image = generateCaptchaImage(phrase, allChars);

        // 存入 Redis（ObjectMapper 序列化）
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        CaptchaStoreData storeData = new CaptchaStoreData(phrase,
                allChars.stream().map(c -> new CharStoreData(c.character, c.x, c.y, c.target, c.order)).toList());
        redisCache.put(RedisKeyNamespace.CAPTCHA, captchaKey, storeData, CAPTCHA_TTL);

        log.info("验证码生成成功 - key: {}, 词语: {}, 总字数: {}", captchaKey, phrase, allChars.size());

        return new CaptchaData(
                captchaKey, base64Image,
                "请依次点击：" + phrase,
                2, IMAGE_WIDTH, IMAGE_HEIGHT, CAPTCHA_TTL.toSeconds()
        );
    }

    @Override
    public boolean validateCaptcha(String captchaKey, String captchaCode) {
        if (captchaKey == null || captchaCode == null) {
            return false;
        }

        var stored = redisCache.get(RedisKeyNamespace.CAPTCHA, captchaKey, CaptchaStoreData.class);
        if (stored.isEmpty()) {
            log.warn("验证码不存在或已过期 - key: {}", captchaKey);
            return false;
        }

        // 验证后立即删除（一次性使用）
        redisCache.evict(RedisKeyNamespace.CAPTCHA, captchaKey);

        CaptchaStoreData data = stored.get();
        boolean valid = data.phrase().equals(captchaCode.trim());
        log.info("验证码文本验证: {} - key: {}", valid ? "通过" : "不通过", captchaKey);
        return valid;
    }

    @Override
    public boolean validateCaptchaByPosition(String captchaKey, List<ClickPosition> positions) {
        if (captchaKey == null || positions == null || positions.isEmpty()) {
            return false;
        }

        var stored = redisCache.get(RedisKeyNamespace.CAPTCHA, captchaKey, CaptchaStoreData.class);
        if (stored.isEmpty()) {
            log.warn("验证码不存在或已过期 - key: {}", captchaKey);
            return false;
        }

        redisCache.evict(RedisKeyNamespace.CAPTCHA, captchaKey);

        CaptchaStoreData data = stored.get();
        // 提取目标字符（按 order 排序）
        List<CharStoreData> targetChars = data.chars().stream()
                .filter(c -> c.target)
                .sorted(Comparator.comparingInt(c -> c.order))
                .toList();

        if (positions.size() != targetChars.size()) {
            log.warn("点击次数({})与目标字符数({})不匹配", positions.size(), targetChars.size());
            return false;
        }

        for (int i = 0; i < targetChars.size(); i++) {
            CharStoreData tc = targetChars.get(i);
            ClickPosition click = positions.get(i);
            double dist = Math.sqrt(Math.pow(click.x() - tc.x, 2) + Math.pow(click.y() - tc.y, 2));
            if (dist > CLICK_TOLERANCE) {
                log.info("第{}个点击偏离目标(距离{}px > {}px)", i + 1, String.format("%.1f", dist), CLICK_TOLERANCE);
                return false;
            }
        }

        log.info("验证码坐标验证通过 - key: {}", captchaKey);
        return true;
    }

    // ==================== 图片生成 ====================

    private String generateCaptchaImage(String phrase, List<CharInfo> allChars) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 背景渐变
            g2d.setPaint(new GradientPaint(0, 0, new Color(245, 248, 255),
                    0, IMAGE_HEIGHT, new Color(230, 238, 250)));
            g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

            // 顶部提示区域
            g2d.setColor(new Color(55, 55, 55, 210));
            g2d.fillRect(0, 0, IMAGE_WIDTH, TOP_AREA_HEIGHT);
            g2d.setColor(Color.WHITE);
            Font promptFont = new Font(getChineseFontName(), Font.PLAIN, 16);
            g2d.setFont(promptFont);
            String promptText = "请依次点击：" + phrase;
            FontMetrics fm = g2d.getFontMetrics();
            int py = (TOP_AREA_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();
            g2d.drawString(promptText, (IMAGE_WIDTH - fm.stringWidth(promptText)) / 2, py);

            // 噪点
            for (int i = 0; i < 80; i++) {
                g2d.setColor(new Color(160 + RANDOM.nextInt(96), 160 + RANDOM.nextInt(96),
                        160 + RANDOM.nextInt(96), 80 + RANDOM.nextInt(100)));
                g2d.fillOval(RANDOM.nextInt(IMAGE_WIDTH),
                        TOP_AREA_HEIGHT + RANDOM.nextInt(IMAGE_HEIGHT - TOP_AREA_HEIGHT), 2, 2);
            }

            // 贝塞尔干扰曲线
            g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int lineCount = 3 + RANDOM.nextInt(3);
            for (int i = 0; i < lineCount; i++) {
                g2d.setColor(new Color(110 + RANDOM.nextInt(80), 110 + RANDOM.nextInt(80),
                        140 + RANDOM.nextInt(60), 100));
                int y1 = TOP_AREA_HEIGHT + RANDOM.nextInt(IMAGE_HEIGHT - TOP_AREA_HEIGHT);
                int y2 = TOP_AREA_HEIGHT + RANDOM.nextInt(IMAGE_HEIGHT - TOP_AREA_HEIGHT);
                g2d.draw(new CubicCurve2D.Double(
                        RANDOM.nextInt(IMAGE_WIDTH / 3), y1,
                        IMAGE_WIDTH / 3 + RANDOM.nextInt(IMAGE_WIDTH / 3),
                        TOP_AREA_HEIGHT + RANDOM.nextInt(IMAGE_HEIGHT - TOP_AREA_HEIGHT),
                        IMAGE_WIDTH / 3 + RANDOM.nextInt(IMAGE_WIDTH / 3),
                        TOP_AREA_HEIGHT + RANDOM.nextInt(IMAGE_HEIGHT - TOP_AREA_HEIGHT),
                        IMAGE_WIDTH * 2 / 3 + RANDOM.nextInt(IMAGE_WIDTH / 3), y2));
            }

            // 绘制汉字
            String fontName = getChineseFontName();
            for (CharInfo ci : allChars) {
                int fontSize = MIN_CHAR_SIZE + RANDOM.nextInt(MAX_CHAR_SIZE - MIN_CHAR_SIZE + 1);
                Font font = new Font(fontName, Font.BOLD, fontSize);
                g2d.setFont(font);

                g2d.setColor(ci.target
                        ? new Color(30 + RANDOM.nextInt(60), 60 + RANDOM.nextInt(80), 160 + RANDOM.nextInt(60))
                        : new Color(80 + RANDOM.nextInt(70), 80 + RANDOM.nextInt(70), 80 + RANDOM.nextInt(70)));

                double angle = (RANDOM.nextDouble() - 0.5) * 0.4;
                AffineTransform old = g2d.getTransform();
                g2d.rotate(angle, ci.x, ci.y);
                g2d.drawString(ci.character, ci.x - fontSize / 2, ci.y + fontSize / 3);
                g2d.setTransform(old);
            }

            // 边框
            g2d.setColor(new Color(180, 180, 200));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRect(0, 0, IMAGE_WIDTH - 1, IMAGE_HEIGHT - 1);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            log.error("生成验证码图片失败", e);
            return createFallbackCaptcha(phrase);
        } finally {
            g2d.dispose();
        }
    }

    private String createFallbackCaptcha(String phrase) {
        try {
            BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
            g2d.setColor(new Color(55, 55, 55));
            g2d.fillRect(0, 0, IMAGE_WIDTH, TOP_AREA_HEIGHT);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font(getChineseFontName(), Font.PLAIN, 16));
            g2d.drawString("请依次点击：" + phrase, 10, 28);
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font(getChineseFontName(), Font.BOLD, 36));
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(phrase, (IMAGE_WIDTH - fm.stringWidth(phrase)) / 2,
                    (IMAGE_HEIGHT + TOP_AREA_HEIGHT) / 2 + fm.getAscent() / 2);
            g2d.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e2) {
            log.error("降级验证码也失败", e2);
            return "";
        }
    }

    // ==================== 布局算法 ====================

    private void layoutChars(List<CharInfo> allChars) {
        int contentLeft = 10;
        int contentRight = IMAGE_WIDTH - 10;
        int contentTop = TOP_AREA_HEIGHT + 10;
        int contentBottom = IMAGE_HEIGHT - 10;

        List<Rectangle> placed = new ArrayList<>();

        for (CharInfo ci : allChars) {
            int charSize = MIN_CHAR_SIZE + RANDOM.nextInt(MAX_CHAR_SIZE - MIN_CHAR_SIZE + 1);
            ci.size = charSize;
            boolean ok = false;

            for (int attempt = 0; attempt < 50; attempt++) {
                int x = contentLeft + charSize / 2
                        + RANDOM.nextInt(contentRight - contentLeft - charSize);
                int y = contentTop + charSize / 2
                        + RANDOM.nextInt(contentBottom - contentTop - charSize);

                Rectangle bounds = new Rectangle(x - charSize / 2 - 8, y - charSize / 2 - 8,
                        charSize + 16, charSize + 16);
                if (placed.stream().noneMatch(p -> p.intersects(bounds))) {
                    ci.x = x;
                    ci.y = y;
                    placed.add(bounds);
                    ok = true;
                    break;
                }
            }

            if (!ok) {
                ci.x = contentLeft + charSize / 2
                        + RANDOM.nextInt(contentRight - contentLeft - charSize);
                ci.y = contentTop + charSize / 2
                        + RANDOM.nextInt(contentBottom - contentTop - charSize);
            }
        }
    }

    // ==================== 工具方法 ====================

    private String getChineseFontName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return "PingFang SC";
        } else if (os.contains("linux")) {
            return "Noto Sans CJK SC";
        }
        return "Microsoft YaHei";
    }

    // ==================== 内部类 ====================

    private static class CharInfo {
        final String character;
        final boolean target;
        final int order;
        int x, y, size;

        CharInfo(String character, boolean target, int order) {
            this.character = character;
            this.target = target;
            this.order = order;
        }
    }

    /** 验证码存储数据（Redis 缓存用） */
    public record CaptchaStoreData(String phrase, List<CharStoreData> chars) {}

    /** 单字存储数据 */
    public record CharStoreData(String character, int x, int y, boolean target, int order) {}
}
