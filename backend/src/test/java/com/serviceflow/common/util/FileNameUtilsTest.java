package com.serviceflow.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文件名清洗与路径安全测试（防止上传/下载阶段的路径穿越与非法字符）。
 */
class FileNameUtilsTest {

    @Test
    @DisplayName("清洗掉目录部分，只保留文件名")
    void shouldStripDirectoryPart() {
        assertThat(FileNameUtils.sanitize("../../etc/passwd")).isEqualTo("passwd");
        assertThat(FileNameUtils.sanitize("C:\\Windows\\System32\\cmd.exe")).isEqualTo("cmd.exe");
        assertThat(FileNameUtils.sanitize("/var/log/app.log")).isEqualTo("app.log");
    }

    @Test
    @DisplayName("替换危险字符并去掉控制字符")
    void shouldRemoveDangerousCharacters() {
        assertThat(FileNameUtils.sanitize("a<b>c:d\"e|f?g*h.txt")).isEqualTo("a_b_c_d_e_f_g_h.txt");
        assertThat(FileNameUtils.sanitize("fault\u0000photo.png")).isEqualTo("faultphoto.png");
    }

    @Test
    @DisplayName("空文件名或纯路径分隔符时给出兜底名")
    void shouldFallbackForEmptyName() {
        assertThat(FileNameUtils.sanitize(null)).isEqualTo("file");
        assertThat(FileNameUtils.sanitize("   ")).isEqualTo("file");
        assertThat(FileNameUtils.sanitize("/")).isEqualTo("file");
    }

    @Test
    @DisplayName("超长文件名被截断且保留扩展名")
    void shouldTruncateLongName() {
        String longName = "a".repeat(300) + ".png";

        String sanitized = FileNameUtils.sanitize(longName);

        assertThat(sanitized.length()).isLessThanOrEqualTo(120);
        assertThat(sanitized).endsWith(".png");
    }

    @Test
    @DisplayName("扩展名解析为小写，无扩展名返回空串")
    void shouldParseExtension() {
        assertThat(FileNameUtils.extension("Report.PDF")).isEqualTo("pdf");
        assertThat(FileNameUtils.extension("noextension")).isEmpty();
        assertThat(FileNameUtils.extension("trailingdot.")).isEmpty();
        assertThat(FileNameUtils.extension(null)).isEmpty();
    }

    @Test
    @DisplayName("基准目录之外的目标路径一律拒绝（防目录穿越）")
    void shouldRejectPathsOutsideBaseDir() {
        Path base = Paths.get("E:/storage/attachments");

        assertThat(FileNameUtils.resolveSafely(base, "ticket-1/2026-10/abc.png"))
                .isEqualTo(base.resolve("ticket-1/2026-10/abc.png").toAbsolutePath().normalize());
        assertThat(FileNameUtils.resolveSafely(base, "../outside.txt")).isNull();
        assertThat(FileNameUtils.resolveSafely(base, "ticket-1/../../outside.txt")).isNull();
        assertThat(FileNameUtils.resolveSafely(base, "/etc/passwd")).isNull();
        assertThat(FileNameUtils.resolveSafely(base, "..\\outside.txt")).isNull();
        assertThat(FileNameUtils.resolveSafely(base, "  ")).isNull();
        assertThat(FileNameUtils.resolveSafely(base, null)).isNull();
    }
}
