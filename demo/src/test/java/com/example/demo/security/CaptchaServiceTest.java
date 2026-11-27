package com.example.demo.security;

import com.example.demo.common.BusinessException;
import com.example.demo.common.redis.LocalSharedStateStore;
import com.example.demo.vo.CaptchaResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaServiceTest {
    private static final Pattern EXPRESSION_PATTERN = Pattern.compile("^(\\d+) ([+\\-×÷]) (\\d+) = \\?$");

    @Test
    void shouldGenerateSafeNonNegativeExactArithmeticChallenges() {
        CaptchaService service = new CaptchaService(new LocalSharedStateStore());

        for (int i = 0; i < 1000; i++) {
            CaptchaService.ArithmeticChallenge challenge = service.nextChallenge();
            Matcher matcher = EXPRESSION_PATTERN.matcher(challenge.expression());
            assertTrue(matcher.matches(), "Unexpected expression: " + challenge.expression());
            int a = Integer.parseInt(matcher.group(1));
            String op = matcher.group(2);
            int b = Integer.parseInt(matcher.group(3));

            assertTrue(challenge.answer() >= 0, "Answer must be non-negative");
            switch (op) {
                case "+" -> assertEquals(a + b, challenge.answer());
                case "-" -> {
                    assertTrue(a >= b);
                    assertEquals(a - b, challenge.answer());
                }
                case "×" -> assertEquals(a * b, challenge.answer());
                case "÷" -> {
                    assertNotEquals(0, b);
                    assertEquals(0, a % b);
                    assertEquals(a / b, challenge.answer());
                }
                default -> throw new AssertionError("Unknown operator: " + op);
            }
        }
    }

    @Test
    void shouldRenderSvgAndConsumeCaptchaOnlyOnce() {
        LocalSharedStateStore store = new LocalSharedStateStore();
        CaptchaService service = new CaptchaService(store);

        CaptchaResponse response = service.generate();
        assertTrue(response.imageDataUrl().startsWith("data:image/svg+xml;base64,"));
        String decodedSvg = new String(
                Base64.getDecoder().decode(response.imageDataUrl().substring("data:image/svg+xml;base64,".length())),
                StandardCharsets.UTF_8);
        assertTrue(decodedSvg.startsWith("<svg "));
        assertTrue(decodedSvg.endsWith("</svg>"));

        String expectedAnswer = store.get("captcha:math:" + response.captchaId()).orElseThrow();
        assertDoesNotThrow(() -> service.verifyAndConsume(response.captchaId(), expectedAnswer));

        BusinessException replay = assertThrows(BusinessException.class,
                () -> service.verifyAndConsume(response.captchaId(), expectedAnswer));
        assertEquals(10001, replay.getCode());
    }

    @Test
    void shouldInvalidateCaptchaImmediatelyOnWrongAnswer() {
        LocalSharedStateStore store = new LocalSharedStateStore();
        CaptchaService service = new CaptchaService(store);
        CaptchaResponse response = service.generate();
        String expectedAnswer = store.get("captcha:math:" + response.captchaId()).orElseThrow();

        BusinessException wrong = assertThrows(BusinessException.class,
                () -> service.verifyAndConsume(response.captchaId(), "999999"));
        assertEquals(10001, wrong.getCode());

        BusinessException secondTry = assertThrows(BusinessException.class,
                () -> service.verifyAndConsume(response.captchaId(), expectedAnswer));
        assertEquals(10001, secondTry.getCode());
    }
}
