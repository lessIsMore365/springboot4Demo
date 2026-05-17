package org.example.service.impl;

import org.example.service.CaptchaService;
import org.example.service.CaptchaService.CaptchaData;
import org.example.service.CaptchaService.ClickPosition;
import org.example.service.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("点击汉字顺序验证码服务测试")
class CaptchaServiceImplTest {

    @Mock
    private RedisService redisService;

    private CaptchaServiceImpl captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaServiceImpl(redisService);
    }

    // ==================== 生成验证码测试 ====================

    @Test
    @DisplayName("生成验证码返回完整字段")
    void testGenerateCaptcha() {
        when(redisService.setWithExpire(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        CaptchaData data = captchaService.generateCaptcha();

        assertNotNull(data.captchaKey());
        assertFalse(data.captchaKey().isEmpty());
        assertNotNull(data.captchaImage());
        assertTrue(data.captchaImage().startsWith("data:image/png;base64,"));
        assertNotNull(data.promptText());
        assertTrue(data.promptText().startsWith("请依次点击："));
        assertEquals(2, data.charCount());
        assertEquals(350, data.imageWidth());
        assertEquals(180, data.imageHeight());
        assertEquals(300, data.expireIn());

        // 验证 Redis 存储被调用
        verify(redisService).setWithExpire(startsWith("captcha:"), anyString(),
                eq(300L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("多次生成验证码 key 不重复")
    void testGenerateCaptchaUniqueKeys() {
        when(redisService.setWithExpire(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        CaptchaData data1 = captchaService.generateCaptcha();
        CaptchaData data2 = captchaService.generateCaptcha();

        assertNotEquals(data1.captchaKey(), data2.captchaKey());
    }

    // ==================== 文本验证测试 ====================

    @Test
    @DisplayName("文本验证 - 正确汉字序列验证通过")
    void testValidateCaptchaCorrect() {
        // 模拟 Redis 存储的数据：词语"春天"
        String json = "{\"phrase\":\"春天\",\"chars\":["
                + "{\"char\":\"春\",\"x\":80,\"y\":90,\"target\":true,\"order\":0},"
                + "{\"char\":\"天\",\"x\":220,\"y\":100,\"target\":true,\"order\":1},"
                + "{\"char\":\"海\",\"x\":150,\"y\":60,\"target\":false}"
                + "]}";
        when(redisService.get("captcha:test-key", String.class)).thenReturn(json);
        when(redisService.delete("captcha:test-key")).thenReturn(true);

        assertTrue(captchaService.validateCaptcha("test-key", "春天"));
    }

    @Test
    @DisplayName("文本验证 - 错误汉字序列验证不通过")
    void testValidateCaptchaIncorrect() {
        String json = "{\"phrase\":\"春天\",\"chars\":["
                + "{\"char\":\"春\",\"x\":80,\"y\":90,\"target\":true,\"order\":0},"
                + "{\"char\":\"天\",\"x\":220,\"y\":100,\"target\":true,\"order\":1}"
                + "]}";
        when(redisService.get("captcha:test-key", String.class)).thenReturn(json);
        when(redisService.delete("captcha:test-key")).thenReturn(true);

        assertFalse(captchaService.validateCaptcha("test-key", "大海"));
    }

    @Test
    @DisplayName("文本验证 - 验证后立即删除（一次性使用）")
    void testValidateCaptchaOneTimeUse() {
        String json = "{\"phrase\":\"春天\",\"chars\":[]}";
        when(redisService.get("captcha:test-key", String.class)).thenReturn(json);
        when(redisService.delete("captcha:test-key")).thenReturn(true);

        captchaService.validateCaptcha("test-key", "春天");

        verify(redisService).delete("captcha:test-key");
    }

    @Test
    @DisplayName("文本验证 - key 为 null 返回 false")
    void testValidateCaptchaNullKey() {
        assertFalse(captchaService.validateCaptcha(null, "春天"));
        verify(redisService, never()).get(anyString(), any());
    }

    @Test
    @DisplayName("文本验证 - code 为 null 返回 false")
    void testValidateCaptchaNullCode() {
        assertFalse(captchaService.validateCaptcha("test-key", null));
        verify(redisService, never()).get(anyString(), any());
    }

    @Test
    @DisplayName("文本验证 - 验证码已过期返回 false")
    void testValidateCaptchaExpired() {
        when(redisService.get("captcha:test-key", String.class)).thenReturn(null);

        assertFalse(captchaService.validateCaptcha("test-key", "春天"));
    }

    // ==================== 坐标验证测试 ====================

    @Test
    @DisplayName("坐标验证 - 正确坐标和顺序验证通过")
    void testValidateCaptchaByPositionCorrect() {
        String json = "{\"phrase\":\"春天\",\"chars\":["
                + "{\"char\":\"天\",\"x\":220,\"y\":100,\"target\":true,\"order\":1},"
                + "{\"char\":\"海\",\"x\":150,\"y\":60,\"target\":false},"
                + "{\"char\":\"春\",\"x\":80,\"y\":90,\"target\":true,\"order\":0}"
                + "]}";
        when(redisService.get("captcha:test-key", String.class)).thenReturn(json);
        when(redisService.delete("captcha:test-key")).thenReturn(true);

        List<ClickPosition> positions = List.of(
                new ClickPosition(82, 92),   // 点击"春"（容差内）
                new ClickPosition(218, 102)  // 点击"天"（容差内）
        );

        assertTrue(captchaService.validateCaptchaByPosition("test-key", positions));
    }

    @Test
    @DisplayName("坐标验证 - 点击位置偏离太远不通过")
    void testValidateCaptchaByPositionTooFar() {
        String json = "{\"phrase\":\"春天\",\"chars\":["
                + "{\"char\":\"春\",\"x\":80,\"y\":90,\"target\":true,\"order\":0},"
                + "{\"char\":\"天\",\"x\":220,\"y\":100,\"target\":true,\"order\":1}"
                + "]}";
        when(redisService.get("captcha:test-key", String.class)).thenReturn(json);
        when(redisService.delete("captcha:test-key")).thenReturn(true);

        List<ClickPosition> positions = List.of(
                new ClickPosition(82, 92),    // 正确
                new ClickPosition(300, 160)   // 太远（距离 > 40px）
        );

        assertFalse(captchaService.validateCaptchaByPosition("test-key", positions));
    }

    @Test
    @DisplayName("坐标验证 - 点击顺序错误不通过")
    void testValidateCaptchaByPositionWrongOrder() {
        String json = "{\"phrase\":\"春天\",\"chars\":["
                + "{\"char\":\"春\",\"x\":80,\"y\":90,\"target\":true,\"order\":0},"
                + "{\"char\":\"天\",\"x\":220,\"y\":100,\"target\":true,\"order\":1}"
                + "]}";
        when(redisService.get("captcha:test-key", String.class)).thenReturn(json);
        when(redisService.delete("captcha:test-key")).thenReturn(true);

        // 先点"天"再点"春"（顺序错误）
        List<ClickPosition> positions = List.of(
                new ClickPosition(218, 102),  // 先点"天"
                new ClickPosition(82, 92)     // 后点"春"
        );

        assertFalse(captchaService.validateCaptchaByPosition("test-key", positions));
    }

    @Test
    @DisplayName("坐标验证 - 点击次数不匹配不通过")
    void testValidateCaptchaByPositionWrongCount() {
        String json = "{\"phrase\":\"春天\",\"chars\":["
                + "{\"char\":\"春\",\"x\":80,\"y\":90,\"target\":true,\"order\":0},"
                + "{\"char\":\"天\",\"x\":220,\"y\":100,\"target\":true,\"order\":1}"
                + "]}";
        when(redisService.get("captcha:test-key", String.class)).thenReturn(json);
        when(redisService.delete("captcha:test-key")).thenReturn(true);

        // 只点了一次，但需要点两次
        List<ClickPosition> positions = List.of(
                new ClickPosition(82, 92)
        );

        assertFalse(captchaService.validateCaptchaByPosition("test-key", positions));
    }

    @Test
    @DisplayName("坐标验证 - key 为 null 返回 false")
    void testValidateCaptchaByPositionNullKey() {
        assertFalse(captchaService.validateCaptchaByPosition(null,
                List.of(new ClickPosition(10, 10))));
    }

    @Test
    @DisplayName("坐标验证 - positions 为空返回 false")
    void testValidateCaptchaByPositionEmptyPositions() {
        assertFalse(captchaService.validateCaptchaByPosition("test-key", List.of()));
    }

    @Test
    @DisplayName("坐标验证 - positions 为 null 返回 false")
    void testValidateCaptchaByPositionNullPositions() {
        assertFalse(captchaService.validateCaptchaByPosition("test-key", null));
    }

    // ==================== 容差边界测试 ====================

    @Test
    @DisplayName("坐标验证 - 正好在容差边界（40px）通过")
    void testValidateCaptchaByPositionAtToleranceEdge() {
        String json = "{\"phrase\":\"春天\",\"chars\":["
                + "{\"char\":\"春\",\"x\":80,\"y\":90,\"target\":true,\"order\":0},"
                + "{\"char\":\"天\",\"x\":220,\"y\":100,\"target\":true,\"order\":1}"
                + "]}";
        when(redisService.get("captcha:test-key", String.class)).thenReturn(json);
        when(redisService.delete("captcha:test-key")).thenReturn(true);

        // (80, 50) → 目标(80, 90) → 距离 = sqrt(0 + 40²) = 40（正好在容差边界）
        List<ClickPosition> positions = List.of(
                new ClickPosition(80, 50),
                new ClickPosition(220, 100)
        );

        assertTrue(captchaService.validateCaptchaByPosition("test-key", positions));
    }

    @Test
    @DisplayName("坐标验证 - 略超容差边界不通过")
    void testValidateCaptchaByPositionJustOutsideTolerance() {
        String json = "{\"phrase\":\"春天\",\"chars\":["
                + "{\"char\":\"春\",\"x\":80,\"y\":90,\"target\":true,\"order\":0},"
                + "{\"char\":\"天\",\"x\":220,\"y\":100,\"target\":true,\"order\":1}"
                + "]}";
        when(redisService.get("captcha:test-key", String.class)).thenReturn(json);
        when(redisService.delete("captcha:test-key")).thenReturn(true);

        // (80, 131) → 目标(80, 90) → 距离 = sqrt(0 + 41²) = 41 > 40
        List<ClickPosition> positions = List.of(
                new ClickPosition(80, 131),
                new ClickPosition(220, 100)
        );

        assertFalse(captchaService.validateCaptchaByPosition("test-key", positions));
    }
}
