package com.exam.ai.document.service;

import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

/**
 * DocumentFileService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface DocumentFileService {

    /**
     * StoredDocument 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
     * @param originalFilename 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param storedFilename 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param fileType 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param fileSize 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param sha256 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param storagePath 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    public record StoredDocument(
            String originalFilename,
            String storedFilename,
            String fileType,
            Long fileSize,
            String sha256,
            String storagePath
    ) {
    }

    StoredDocument store(MultipartFile file);

    void validate(MultipartFile file);

    String sanitizeFilename(String filename);

    String extension(String filename);

    String sha256(Path path);
}
