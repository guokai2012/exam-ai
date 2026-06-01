package com.exam.ai.document.service;

import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;

/**
 * DocumentFileService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface DocumentFileService {

    /**
     * StoredDocument 记录对象，封装当前业务流程中的不可变数据。
     * @param originalFilename 业务参数，参与当前方法的校验、查询或状态变更。
     * @param storedFilename 业务参数，参与当前方法的校验、查询或状态变更。
     * @param fileType 业务参数，参与当前方法的校验、查询或状态变更。
     * @param fileSize 业务参数，参与当前方法的校验、查询或状态变更。
     * @param sha256 业务参数，参与当前方法的校验、查询或状态变更。
     * @param storagePath 业务参数，参与当前方法的校验、查询或状态变更。
     * @param extractedText 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    public record StoredDocument(
            String originalFilename,
            String storedFilename,
            String fileType,
            Long fileSize,
            String sha256,
            String storagePath,
            String extractedText
    ) {
    }

    StoredDocument store(MultipartFile file);

    void validate(MultipartFile file);

    String sanitizeFilename(String filename);

    String extension(String filename);

    String extractText(Path path, String fileType);

    String sha256(Path path);
}
