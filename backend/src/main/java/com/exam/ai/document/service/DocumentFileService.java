package com.exam.ai.document.service;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.config.DocumentProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentFileService {

    private static final Set<String> ALLOWED_TYPES = Set.of("md", "pdf", "doc", "docx");

    private final DocumentProperties properties;
    private final Tika tika = new Tika();

    public DocumentFileService(DocumentProperties properties) {
        this.properties = properties;
    }

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
            String extractedText = extractText(target, fileType);
            return new StoredDocument(originalFilename, storedFilename, fileType, file.getSize(), sha256, target.toString(), extractedText);
        } catch (IOException ex) {
            throw BusinessException.badRequest("文件保存或解析失败");
        }
    }

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
            throw BusinessException.badRequest("仅支持 md、pdf、doc、docx 文件");
        }
    }

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

    public String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            throw BusinessException.badRequest("文件缺少扩展名");
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public String extractText(Path path, String fileType) {
        try {
            if ("md".equals(fileType)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
            return tika.parseToString(path);
        } catch (Exception ex) {
            throw BusinessException.badRequest("文档内容提取失败");
        }
    }

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

    private static final class OutputStreamDiscard extends java.io.OutputStream {
        private static final OutputStreamDiscard INSTANCE = new OutputStreamDiscard();

        @Override
        public void write(int b) {
        }
    }
}
