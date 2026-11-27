package com.example.demo.security;

import com.example.demo.common.BusinessException;
import com.example.demo.common.redis.SharedStateStore;
import com.example.demo.vo.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * 2 数四则算术图形验证码服务。
 *
 * <p>服务端直接生成带干扰元素的 SVG，不依赖容器字体库；状态存入 {@link SharedStateStore}
 * 并强制一次性消费，防止对同一个 {@code captchaId} 暴力重试。</p>
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(3);
    private static final String KEY_PREFIX = "captcha:math:";
    private static final String[] INK_COLORS = {"#20332c", "#34775a", "#4e9b78", "#4d645b"};
    private static final String[] LINE_COLORS = {"#a8d5ba", "#b9dfe9", "#d7e8bd", "#8fb8a3"};

    private final SharedStateStore stateStore;
    private final SecureRandom secureRandom = new SecureRandom();

    public CaptchaResponse generate() {
        ArithmeticChallenge challenge = nextChallenge();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        stateStore.put(KEY_PREFIX + captchaId, String.valueOf(challenge.answer()), CAPTCHA_TTL);
        String svg = renderSvg(challenge.expression());
        String dataUrl = "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new CaptchaResponse(captchaId, dataUrl, CAPTCHA_TTL.toSeconds());
    }

    /** 一次性消费图形验证码：无论答对还是答错，校验一次立刻删除。 */
    public void verifyAndConsume(String captchaId, String rawAnswer) {
        if (captchaId == null || captchaId.isBlank() || rawAnswer == null || rawAnswer.isBlank()) {
            throw new BusinessException(10001, "图形验证码无效或已过期，请刷新重试");
        }
        String key = KEY_PREFIX + captchaId.trim();
        Optional<String> expected = stateStore.get(key);
        stateStore.delete(key);
        if (expected.isEmpty() || !expected.get().equals(rawAnswer.trim())) {
            throw new BusinessException(10001, "图形验证码无效或已过期，请刷新重试");
        }
    }

    ArithmeticChallenge nextChallenge() {
        int operator = secureRandom.nextInt(4);
        return switch (operator) {
            case 0 -> {
                int a = 1 + secureRandom.nextInt(20);
                int b = 1 + secureRandom.nextInt(20);
                yield new ArithmeticChallenge(a + " + " + b + " = ?", a + b);
            }
            case 1 -> {
                int a = 5 + secureRandom.nextInt(26);
                int b = 1 + secureRandom.nextInt(a);
                yield new ArithmeticChallenge(a + " - " + b + " = ?", a - b);
            }
            case 2 -> {
                int a = 2 + secureRandom.nextInt(8);
                int b = 2 + secureRandom.nextInt(8);
                yield new ArithmeticChallenge(a + " × " + b + " = ?", a * b);
            }
            default -> {
                int b = 2 + secureRandom.nextInt(8);
                int q = 1 + secureRandom.nextInt(9);
                int a = b * q;
                yield new ArithmeticChallenge(a + " ÷ " + b + " = ?", q);
            }
        };
    }

    private String renderSvg(String expression) {
        StringBuilder svg = new StringBuilder(640);
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"152\" height=\"46\" viewBox=\"0 0 152 46\">");
        svg.append("<defs><linearGradient id=\"bg\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">")
                .append("<stop offset=\"0%\" stop-color=\"#eef7ec\"/>")
                .append("<stop offset=\"100%\" stop-color=\"#fff3c7\"/>")
                .append("</linearGradient></defs>");
        svg.append("<rect width=\"152\" height=\"46\" rx=\"10\" fill=\"url(#bg)\"/>");

        for (int i = 0; i < 4; i++) {
            int x1 = secureRandom.nextInt(40);
            int y1 = 6 + secureRandom.nextInt(34);
            int x2 = 110 + secureRandom.nextInt(42);
            int y2 = 6 + secureRandom.nextInt(34);
            String color = LINE_COLORS[secureRandom.nextInt(LINE_COLORS.length)];
            svg.append("<line x1=\"").append(x1).append("\" y1=\"").append(y1)
                    .append("\" x2=\"").append(x2).append("\" y2=\"").append(y2)
                    .append("\" stroke=\"").append(color).append("\" stroke-width=\"1.4\" opacity=\"0.8\"/>");
        }

        for (int i = 0; i < 18; i++) {
            int cx = 6 + secureRandom.nextInt(140);
            int cy = 6 + secureRandom.nextInt(34);
            String color = LINE_COLORS[secureRandom.nextInt(LINE_COLORS.length)];
            svg.append("<circle cx=\"").append(cx).append("\" cy=\"").append(cy)
                    .append("\" r=\"1.2\" fill=\"").append(color).append("\" opacity=\"0.75\"/>");
        }

        int startX = 14;
        int stepX = Math.max(11, 126 / Math.max(expression.length(), 1));
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == ' ') {
                continue;
            }
            int x = startX + i * stepX;
            int y = 29 + secureRandom.nextInt(5) - 2;
            int rotate = secureRandom.nextInt(15) - 7;
            String color = INK_COLORS[secureRandom.nextInt(INK_COLORS.length)];
            svg.append("<text x=\"").append(x).append("\" y=\"").append(y)
                    .append("\" fill=\"").append(color)
                    .append("\" font-family=\"monospace\" font-size=\"18\" font-weight=\"700\"")
                    .append(" transform=\"rotate(").append(rotate).append(' ').append(x).append(' ').append(y).append(")\">")
                    .append(escapeXml(ch))
                    .append("</text>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private String escapeXml(char ch) {
        return switch (ch) {
            case '&' -> "&amp;";
            case '<' -> "&lt;";
            case '>' -> "&gt;";
            default -> String.valueOf(ch);
        };
    }

    record ArithmeticChallenge(String expression, int answer) {
    }
}
