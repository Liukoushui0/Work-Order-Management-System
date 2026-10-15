package com.serviceflow.common.util;

import java.nio.file.Path;
import java.util.Locale;

/**
 * 文件名与路径安全工具。
 *
 * 上传文件名必须做清洗，避免出现 "../"、绝对路径、控制字符等危险内容；
 * 下载时必须以"基准目录 + 规范化后仍位于目录内"双重校验，防止路径穿越。
 */
public final class FileNameUtils {

    private static final int MAX_NAME_LENGTH = 120;
    private static final String FALLBACK_NAME = "file";

    private FileNameUtils() {
    }

    /**
     * 清洗上传时的原始文件名：去掉目录部分、控制字符与危险符号，限制长度。
     */
    public static String sanitize(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return FALLBACK_NAME;
        }
        String name = originalName.trim();
        // 同时处理 Windows 与 Linux 的路径分隔符
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        name = name.replaceAll("[\\p{Cntrl}]", "")
                .replaceAll("[<>:\"|?*]", "_")
                .replace("..", "_")
                .trim();
        if (name.isEmpty() || ".".equals(name)) {
            return FALLBACK_NAME;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            String ext = extension(name);
            int keep = Math.max(1, MAX_NAME_LENGTH - ext.length() - 1);
            name = name.substring(0, keep) + (ext.isEmpty() ? "" : "." + ext);
        }
        return name;
    }

    /**
     * 取小写扩展名（不含点），没有扩展名返回空字符串。
     */
    public static String extension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 在基准目录下安全解析相对路径；任何越出基准目录的路径都返回 null。
     */
    public static Path resolveSafely(Path baseDir, String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        Path base = baseDir.toAbsolutePath().normalize();
        Path target = base.resolve(relativePath).normalize();
        if (!target.startsWith(base)) {
            return null;
        }
        return target;
    }
}
