package com.exam.ai.auth.service;

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

/**
 * CaptchaService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface CaptchaService {

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public CaptchaResponse create();
    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param captchaId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param captchaCode 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void verify(String captchaId, String captchaCode);
}
