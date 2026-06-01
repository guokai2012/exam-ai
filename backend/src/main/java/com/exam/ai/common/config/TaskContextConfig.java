package com.exam.ai.common.config;

import com.exam.ai.util.CurrentUserUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

/**
 * 任务上下文配置类，确保 Spring 托管异步任务能够继承当前用户上下文。
 */
@Configuration
public class TaskContextConfig {

    /**
     * 创建基于 TTL 的任务装饰器。
     * @return 能传播当前用户上下文的任务装饰器。
     */
    @Bean
    public TaskDecorator currentUserTaskDecorator() {
        return CurrentUserUtils::wrap;
    }
}
