package com.serviceflow.service;

import com.serviceflow.common.enums.ErrorCode;
import com.serviceflow.common.exception.BusinessException;
import com.serviceflow.common.util.FileNameUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 本地文件存储：负责附件落盘、读取与删除，并统一执行扩展名、大小与路径安全校验。
 *
 * 存储目录通过 app.upload.dir 配置（默认 ./storage/attachments，已被 .gitignore 排除），
 * 文件按 年-月 分目录，存储名为 UUID，不保留用户可控的原始文件名。
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int SHA256_BUFFER = 8192;

    private final ConfigService configService;
    private final Path rootDir;

    public FileStorageService(ConfigService configService,
                              @Value("${app.upload.dir:./storage/attachments}") String uploadDir) {
        this.configService = configService;
        this.rootDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(rootDir);
            log.info("附件存储目录：{}", rootDir);
        } catch (IOException ex) {
            throw new IllegalStateException("无法创建附件存储目录：" + rootDir, ex);
        }
    }

    public Path rootDir() {
        return rootDir;
    }

    /**
     * 允许的扩展名白名单（来自 sys_config）。
     */
    public List<String> allowedExtensions() {
        String value = configService.getString("attachment.allowed_extensions",
                "jpg,jpeg,png,gif,pdf,txt,log,zip,docx,xlsx");
        return List.of(value.split(",")).stream()
                .map(item -> item.trim().toLowerCase(Locale.ROOT))
                .filter(item -> !item.isEmpty())
                .toList();
    }

    /**
     * 单个附件大小上限（字节）。
     */
    public long maxSizeBytes() {
        int maxMb = configService.getInt("attachment.max_size_mb", 20);
        return (long) Math.max(maxMb, 1) * 1024 * 1024;
    }

    /**
     * 校验并保存上传文件，返回相对存储路径与 SHA-256 摘要。
     */
    public StoredFile store(MultipartFile file, Long ticketId) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }
        String originalName = FileNameUtils.sanitize(file.getOriginalFilename());
        String ext = FileNameUtils.extension(originalName);
        if (ext.isEmpty() || !allowedExtensions().contains(ext)) {
            throw BusinessException.of(ErrorCode.FILE_TYPE_NOT_ALLOWED,
                    "不支持的文件类型：" + (ext.isEmpty() ? "未知" : ext));
        }
        if (file.getSize() > maxSizeBytes()) {
            throw BusinessException.of(ErrorCode.FILE_TOO_LARGE,
                    "文件大小超出限制（上限 " + (maxSizeBytes() / 1024 / 1024) + "MB）");
        }

        String month = LocalDate.now().format(MONTH_FORMATTER);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path targetDir = rootDir.resolve("ticket-" + ticketId).resolve(month);
        Path target = FileNameUtils.resolveSafely(rootDir, "ticket-" + ticketId + "/" + month + "/" + storedName);
        if (target == null) {
            throw BusinessException.of(ErrorCode.PARAM_INVALID, "非法的存储路径");
        }
        try {
            Files.createDirectories(targetDir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            String relativePath = rootDir.relativize(target).toString().replace('\\', '/');
            return new StoredFile(originalName, storedName, relativePath, ext, file.getSize(),
                    file.getContentType(), sha256(target));
        } catch (IOException ex) {
            throw BusinessException.of(ErrorCode.INTERNAL_ERROR, "附件保存失败，请重试");
        }
    }

    /**
     * 解析下载路径，越出存储目录的路径一律拒绝。
     */
    public Path resolveForRead(String relativePath) {
        Path path = FileNameUtils.resolveSafely(rootDir, relativePath);
        if (path == null) {
            throw BusinessException.of(ErrorCode.ACCESS_DENIED, "非法的附件路径");
        }
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw BusinessException.of(ErrorCode.ATTACHMENT_NOT_FOUND);
        }
        return path;
    }

    /**
     * 删除物理文件；失败只记录日志，不影响元数据的逻辑删除。
     */
    public void delete(String relativePath) {
        try {
            Path path = FileNameUtils.resolveSafely(rootDir, relativePath);
            if (path != null && Files.exists(path)) {
                Files.delete(path);
            }
        } catch (IOException ex) {
            log.warn("删除附件物理文件失败：path={} error={}", relativePath, ex.getMessage());
        }
    }

    private String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[SHA256_BUFFER];
            try (InputStream in = Files.newInputStream(path)) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : digest.digest()) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            log.warn("计算文件摘要失败：{}", ex.getMessage());
            return null;
        }
    }

    /**
     * 落盘结果。
     */
    public record StoredFile(String originalName, String storedName, String relativePath, String extension,
                             long size, String contentType, String sha256) {
    }
}
