/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link PlatformDetector}. */
class PlatformDetectorTest {

    @Test
    void detectOsMacOsX() {
        assertEquals("darwin", PlatformDetector.detectOs("Mac OS X"));
    }

    @Test
    void detectOsDarwin() {
        assertEquals("darwin", PlatformDetector.detectOs("Darwin"));
    }

    @Test
    void detectOsLinux() {
        assertEquals("linux", PlatformDetector.detectOs("Linux"));
    }

    @Test
    void detectOsWindowsLowercase() {
        assertEquals("win32", PlatformDetector.detectOs("Windows 10"));
    }

    @Test
    void detectOsWindowsServer() {
        assertEquals("win32", PlatformDetector.detectOs("Windows Server 2022"));
    }

    @Test
    void detectOsUnknownThrows() {
        assertThrows(IllegalStateException.class, () -> PlatformDetector.detectOs("SunOS"));
    }

    @Test
    void detectOsEmptyThrows() {
        assertThrows(IllegalStateException.class, () -> PlatformDetector.detectOs(""));
    }

    @Test
    void detectArchAmd64() {
        assertEquals("x64", PlatformDetector.detectArch("amd64"));
    }

    @Test
    void detectArchX86_64() {
        assertEquals("x64", PlatformDetector.detectArch("x86_64"));
    }

    @Test
    void detectArchX64() {
        assertEquals("x64", PlatformDetector.detectArch("x64"));
    }

    @Test
    void detectArchAarch64() {
        assertEquals("arm64", PlatformDetector.detectArch("aarch64"));
    }

    @Test
    void detectArchArm64() {
        assertEquals("arm64", PlatformDetector.detectArch("arm64"));
    }

    @Test
    void detectArchUnknownThrows() {
        assertThrows(IllegalStateException.class, () -> PlatformDetector.detectArch("i686"));
    }

    @Test
    void detectArchEmptyThrows() {
        assertThrows(IllegalStateException.class, () -> PlatformDetector.detectArch(""));
    }

    @Test
    void readElfPtInterpGlibc(@TempDir Path tmp) throws Exception {
        Path elf = tmp.resolve("glibc.elf");
        Files.write(elf, buildMinimalElf64("/lib64/ld-linux-x86-64.so.2"));
        String interp = PlatformDetector.readElfPtInterp(elf);
        assertEquals("/lib64/ld-linux-x86-64.so.2", interp);
    }

    @Test
    void readElfPtInterpMusl(@TempDir Path tmp) throws Exception {
        Path elf = tmp.resolve("musl.elf");
        Files.write(elf, buildMinimalElf64("/lib/ld-musl-x86_64.so.1"));
        String interp = PlatformDetector.readElfPtInterp(elf);
        assertEquals("/lib/ld-musl-x86_64.so.1", interp);
    }

    @Test
    void readElfPtInterpNotElfThrows(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("not-elf");
        Files.write(f, new byte[]{0x00, 0x01, 0x02, 0x03, 0x04});
        assertThrows(IOException.class, () -> PlatformDetector.readElfPtInterp(f));
    }

    @Test
    void readElfPtInterpTooSmallThrows(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("tiny");
        Files.write(f, new byte[]{0x7F, 'E', 'L', 'F', 0x02});
        assertThrows(IOException.class, () -> PlatformDetector.readElfPtInterp(f));
    }

    @Test
    void readElfPtInterpRejectsTruncatedProgramHeaderEntry(@TempDir Path tmp) throws Exception {
        byte[] elf = buildMinimalElf64("/lib64/ld-linux-x86-64.so.2");
        elf[54] = 8;
        elf[55] = 0;
        Path f = tmp.resolve("truncated-phdr.elf");
        Files.write(f, elf);
        IOException ex = assertThrows(IOException.class, () -> PlatformDetector.readElfPtInterp(f));
        assertTrue(ex.getMessage().contains("entry size"));
    }

    @ParameterizedTest
    @MethodSource("classifierCases")
    void detectClassifierFromTuple(String osName, String osArch, PlatformDetector.LinuxLibc libc, String expected) {
        assertEquals(expected, PlatformDetector.detectClassifier(osName, osArch, libc));
    }

    @ParameterizedTest
    @MethodSource("unsupportedClassifierCases")
    void detectClassifierRejectsUnsupportedTuples(String osName, String osArch, PlatformDetector.LinuxLibc libc) {
        assertThrows(IllegalStateException.class, () -> PlatformDetector.detectClassifier(osName, osArch, libc));
    }

    @Test
    void detectClassifierOnCurrentPlatformReturnsKnownValue() {
        String classifier = PlatformDetector.detectClassifier();
        assertNotNull(classifier);
        assertTrue(classifier.matches("(linux|linuxmusl|darwin|win32)-(x64|arm64)"),
                "Unexpected classifier: " + classifier);
    }

    private static Stream<Arguments> classifierCases() {
        return Stream.of(Arguments.of("Linux", "amd64", PlatformDetector.LinuxLibc.GLIBC, "linux-x64"),
                Arguments.of("Linux", "x86_64", PlatformDetector.LinuxLibc.MUSL, "linuxmusl-x64"),
                Arguments.of("Linux", "aarch64", PlatformDetector.LinuxLibc.GLIBC, "linux-arm64"),
                Arguments.of("Linux", "arm64", PlatformDetector.LinuxLibc.MUSL, "linuxmusl-arm64"),
                Arguments.of("Darwin", "x86_64", PlatformDetector.LinuxLibc.NOT_APPLICABLE, "darwin-x64"),
                Arguments.of("Mac OS X", "arm64", PlatformDetector.LinuxLibc.NOT_APPLICABLE, "darwin-arm64"),
                Arguments.of("Windows 11", "amd64", PlatformDetector.LinuxLibc.NOT_APPLICABLE, "win32-x64"),
                Arguments.of("Windows Server 2022", "aarch64", PlatformDetector.LinuxLibc.NOT_APPLICABLE,
                        "win32-arm64"));
    }

    private static Stream<Arguments> unsupportedClassifierCases() {
        return Stream.of(Arguments.of("Linux", "ppc64le", PlatformDetector.LinuxLibc.GLIBC),
                Arguments.of("Haiku", "x86_64", PlatformDetector.LinuxLibc.NOT_APPLICABLE));
    }

    /**
     * Builds a minimal ELF64 binary with a single PT_INTERP segment containing the
     * given interpreter path. The binary is fully self-contained within the 2 KB
     * probe window used by {@link PlatformDetector#readElfPtInterp}.
     */
    static byte[] buildMinimalElf64(String interpPath) throws IOException {
        byte[] interpBytes = interpPath.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int phdrOffset = 64;
        int phdrSize = 56;
        int interpOffset = phdrOffset + phdrSize;
        int interpSize = interpBytes.length + 1;

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        dos.writeByte(0x7F);
        dos.writeByte('E');
        dos.writeByte('L');
        dos.writeByte('F');
        dos.writeByte(2);
        dos.writeByte(1);
        dos.writeByte(1);
        dos.writeByte(0);
        dos.write(new byte[8]);
        writeUInt16Le(dos, 2);
        writeUInt16Le(dos, 62);
        writeUInt32Le(dos, 1);
        writeUInt64Le(dos, 0);
        writeUInt64Le(dos, phdrOffset);
        writeUInt64Le(dos, 0);
        writeUInt32Le(dos, 0);
        writeUInt16Le(dos, 64);
        writeUInt16Le(dos, phdrSize);
        writeUInt16Le(dos, 1);
        writeUInt16Le(dos, 64);
        writeUInt16Le(dos, 0);
        writeUInt16Le(dos, 0);

        writeUInt32Le(dos, 3);
        writeUInt32Le(dos, 4);
        writeUInt64Le(dos, interpOffset);
        writeUInt64Le(dos, 0);
        writeUInt64Le(dos, 0);
        writeUInt64Le(dos, interpSize);
        writeUInt64Le(dos, interpSize);
        writeUInt64Le(dos, 1);

        dos.write(interpBytes);
        dos.writeByte(0);

        dos.flush();
        return bos.toByteArray();
    }

    private static void writeUInt16Le(DataOutputStream dos, int v) throws IOException {
        dos.writeByte(v & 0xFF);
        dos.writeByte((v >> 8) & 0xFF);
    }

    private static void writeUInt32Le(DataOutputStream dos, long v) throws IOException {
        dos.writeByte((int) (v & 0xFF));
        dos.writeByte((int) ((v >> 8) & 0xFF));
        dos.writeByte((int) ((v >> 16) & 0xFF));
        dos.writeByte((int) ((v >> 24) & 0xFF));
    }

    private static void writeUInt64Le(DataOutputStream dos, long v) throws IOException {
        for (int i = 0; i < 8; i++) {
            dos.writeByte((int) (v & 0xFF));
            v >>= 8;
        }
    }
}
