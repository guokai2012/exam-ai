package com.exam.ai.common.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * DocumentProperties 类，承载当前分层中的业务职责。
 */
@ConfigurationProperties(prefix = "app.document")
public class DocumentProperties {

    private Path storagePath = Path.of("storage/documents");
    private DataSize maxSize = DataSize.ofMegabytes(20);

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public Path getStoragePath() {
        return storagePath;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param storagePath 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setStoragePath(Path storagePath) {
        this.storagePath = storagePath;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DataSize getMaxSize() {
        return maxSize;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param maxSize 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setMaxSize(DataSize maxSize) {
        this.maxSize = maxSize;
    }
}
