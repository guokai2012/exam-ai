package com.exam.ai.document.service.impl;

import com.exam.ai.common.config.DocumentProperties;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.service.DocumentFileService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * DocumentFileServiceImpl 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Service
public class DocumentFileServiceImpl implements DocumentFileService {

    private static final Set<String> ALLOWED_TYPES = Set.of("pdf");

    private final DocumentProperties properties;

    /**
     * 构造 DocumentFileServiceImpl 实例并注入运行所需依赖。
     * @param properties 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentFileServiceImpl(DocumentProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param file 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public StoredDocument store(MultipartFile file) {
        validate(file);
        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        String fileType = extension(originalFilename);
        String storedFilename = UUID.randomUUID().toString().replace("-", "") + "." + fileType;
        Path storageRoot = properties.getStoragePath().toAbsolutePath().normalize();
        Path target = storageRoot.resolve(storedFilename).normalize();
        if (!target.startsWith(storageRoot)) {
            throw BusinessException.badRequest("文件路径非法");
        }
        try {
            Files.createDirectories(storageRoot);
            file.transferTo(target);
            String sha256 = sha256(target);
            return new StoredDocument(originalFilename, storedFilename, fileType, file.getSize(), sha256, target.toString());
        } catch (IOException ex) {
            throw BusinessException.badRequest("PDF 文件保存失败");
        }
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param file 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择文件");
        }
        if (file.getSize() > properties.getMaxSize().toBytes()) {
            throw BusinessException.badRequest("文件超过大小限制");
        }
        String filename = sanitizeFilename(file.getOriginalFilename());
        String ext = extension(filename);
        if (!ALLOWED_TYPES.contains(ext)) {
            throw BusinessException.badRequest("仅支持 PDF 文件");
        }
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param filename 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public String sanitizeFilename(String filename) {
        String fallback = "document";
        if (filename == null || filename.isBlank()) {
            return fallback;
        }
        String normalized = Normalizer.normalize(filename, Normalizer.Form.NFKC)
                .replace("\\", "/");
        String basename = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\r\\n\\t]", "")
                .replaceAll("[<>:\"|?*]", "_")
                .trim();
        return basename.isBlank() ? fallback : basename;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param filename 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw BusinessException.badRequest("文件缺少扩展名");
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param path 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path);
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                digestInput.transferTo(OutputStreamDiscard.INSTANCE);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException ex) {
            throw BusinessException.badRequest("文件摘要计算失败");
        }
    }

    /**
     * 摘要计算专用丢弃输出流，用于消费输入流但不产生额外文件内容。
     */
    private static final class OutputStreamDiscard extends java.io.OutputStream {
        private static final OutputStreamDiscard INSTANCE = new OutputStreamDiscard();

        /**
         * 执行当前业务步骤，并返回调用方需要的处理结果。
         * @param b 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
         * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
         */
        @Override
        public void write(int b) {
        }
    }
}
