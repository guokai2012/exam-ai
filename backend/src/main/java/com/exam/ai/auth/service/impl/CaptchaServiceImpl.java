package com.exam.ai.auth.service.impl;

import com.exam.ai.auth.service.CaptchaService;
import com.exam.ai.auth.vo.CaptchaResponse;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.common.config.SecurityProperties;
import com.exam.ai.security.RedisKeys;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * CaptchaServiceImpl 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private final StringRedisTemplate redisTemplate;
    private final SecurityProperties properties;
    private final SecureRandom random = new SecureRandom();

    /**
     * 构造 CaptchaServiceImpl 实例并注入运行所需依赖。
     * @param redisTemplate 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param properties 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public CaptchaServiceImpl(StringRedisTemplate redisTemplate, SecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public CaptchaResponse create() {
        int left = random.nextInt(9) + 1;
        int right = random.nextInt(9) + 1;
        boolean plus = random.nextBoolean();
        int answer = plus ? left + right : Math.max(left, right) - Math.min(left, right);
        String expression = plus
                ? left + " + " + right + " = ?"
                : Math.max(left, right) + " - " + Math.min(left, right) + " = ?";
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(RedisKeys.captcha(captchaId), String.valueOf(answer), properties.getCaptchaTtl());
        return new CaptchaResponse(captchaId, "data:image/png;base64," + draw(expression));
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param captchaId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param captchaCode 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void verify(String captchaId, String captchaCode) {
        String key = RedisKeys.captcha(captchaId);
        String answer = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (answer == null || !answer.equals(captchaCode.trim())) {
            throw BusinessException.unauthorized();
        }
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param text 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private String draw(String text) {
        try {
            BufferedImage image = new BufferedImage(160, 56, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(248, 250, 252));
            graphics.fillRect(0, 0, 160, 56);
            for (int i = 0; i < 8; i++) {
                graphics.setColor(new Color(160 + random.nextInt(70), 160 + random.nextInt(70), 160 + random.nextInt(70)));
                graphics.drawLine(random.nextInt(160), random.nextInt(56), random.nextInt(160), random.nextInt(56));
            }
            graphics.setColor(new Color(31, 41, 55));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            graphics.drawString(text, 18, 38);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to draw captcha", ex);
        }
    }
}
