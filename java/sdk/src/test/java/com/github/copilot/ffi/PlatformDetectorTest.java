/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlatformDetectorTest {

    @Test
    void detectOsMapsSupportedNames() {
        withSystemProperty("os.name", "Mac OS X", () -> assertEquals("darwin", PlatformDetector.detectOs()));
        withSystemProperty("os.name", "Darwin", () -> assertEquals("darwin", PlatformDetector.detectOs()));
        withSystemProperty("os.name", "Windows 11", () -> assertEquals("win32", PlatformDetector.detectOs()));
        withSystemProperty("os.name", "Linux", () -> assertEquals("linux", PlatformDetector.detectOs()));
    }

    @Test
    void detectOsThrowsForUnsupportedSystem() {
        withSystemProperty("os.name", "Solaris",
                () -> assertThrows(IllegalStateException.class, PlatformDetector::detectOs));
    }

    @Test
    void detectArchMapsSupportedAliases() {
        withSystemProperty("os.arch", "amd64", () -> assertEquals("x64", PlatformDetector.detectArch()));
        withSystemProperty("os.arch", "x86_64", () -> assertEquals("x64", PlatformDetector.detectArch()));
        withSystemProperty("os.arch", "x64", () -> assertEquals("x64", PlatformDetector.detectArch()));
        withSystemProperty("os.arch", "aarch64", () -> assertEquals("arm64", PlatformDetector.detectArch()));
        withSystemProperty("os.arch", "arm64", () -> assertEquals("arm64", PlatformDetector.detectArch()));
    }

    @Test
    void detectArchThrowsForUnsupportedArchitecture() {
        withSystemProperty("os.arch", "ppc64",
                () -> assertThrows(IllegalStateException.class, PlatformDetector::detectArch));
    }

    @Test
    void detectLinuxLibcParsesGlibcInterpPath() throws Exception {
        byte[] glibcProbe = buildElf64ProbeWithInterp("/lib64/ld-linux-x86-64.so.2");
        assertEquals(PlatformDetector.LinuxLibc.GLIBC, PlatformDetector.detectLinuxLibc(glibcProbe));
    }

    @Test
    void detectLinuxLibcParsesMuslInterpPath() throws Exception {
        byte[] muslProbe = buildElf64ProbeWithInterp("/lib/ld-musl-x86_64.so.1");
        assertEquals(PlatformDetector.LinuxLibc.MUSL, PlatformDetector.detectLinuxLibc(muslProbe));
    }

    @Test
    void detectLinuxLibcOnLinuxReturnsRecognizedValue() {
        withSystemProperty("os.name", "Linux", () -> {
            PlatformDetector.LinuxLibc libc = PlatformDetector.detectLinuxLibc();
            assertTrue(libc == PlatformDetector.LinuxLibc.GLIBC || libc == PlatformDetector.LinuxLibc.MUSL
                    || libc == PlatformDetector.LinuxLibc.UNKNOWN);
        });
    }

    @Test
    void detectLinuxLibcReturnsUnknownOutsideLinux() {
        withSystemProperty("os.name", "Windows 11",
                () -> assertEquals(PlatformDetector.LinuxLibc.UNKNOWN, PlatformDetector.detectLinuxLibc()));
    }

    @Test
    void detectClassifierReturnsClassifierForCurrentLinuxLibc() {
        PlatformDetector.LinuxLibc libc = PlatformDetector.detectLinuxLibc();
        String expected = libc == PlatformDetector.LinuxLibc.MUSL ? "linuxmusl-x64" : "linux-x64";

        withSystemProperties("Linux", "amd64", () -> assertEquals(expected, PlatformDetector.detectClassifier()));
    }

    @Test
    void detectClassifierAllowListCoversAllSupportedValues() {
        Set<String> expected = Set.of("linux-x64", "linux-arm64", "linuxmusl-x64", "linuxmusl-arm64", "darwin-x64",
                "darwin-arm64", "win32-x64", "win32-arm64");
        assertEquals(expected, PlatformDetector.supportedClassifiers());

        Set<String> resolved = new LinkedHashSet<>();
        resolved.add(PlatformDetector.detectClassifier("linux", "x64", PlatformDetector.LinuxLibc.GLIBC));
        resolved.add(PlatformDetector.detectClassifier("linux", "arm64", PlatformDetector.LinuxLibc.GLIBC));
        resolved.add(PlatformDetector.detectClassifier("linux", "x64", PlatformDetector.LinuxLibc.MUSL));
        resolved.add(PlatformDetector.detectClassifier("linux", "arm64", PlatformDetector.LinuxLibc.MUSL));
        resolved.add(PlatformDetector.detectClassifier("darwin", "x64", PlatformDetector.LinuxLibc.UNKNOWN));
        resolved.add(PlatformDetector.detectClassifier("darwin", "arm64", PlatformDetector.LinuxLibc.UNKNOWN));
        resolved.add(PlatformDetector.detectClassifier("win32", "x64", PlatformDetector.LinuxLibc.UNKNOWN));
        resolved.add(PlatformDetector.detectClassifier("win32", "arm64", PlatformDetector.LinuxLibc.UNKNOWN));

        assertEquals(expected, resolved);
    }

    @Test
    void detectClassifierFailsFastForUnsupportedTuple() {
        assertThrows(IllegalStateException.class,
                () -> PlatformDetector.detectClassifier("darwin", "mips64", PlatformDetector.LinuxLibc.UNKNOWN));
    }

    @Test
    void detectClassifierFailsForUnsupportedCurrentPlatform() {
        withSystemProperties("Solaris", "amd64",
                () -> assertThrows(IllegalStateException.class, PlatformDetector::detectClassifier));
    }

    @Test
    void detectLinuxLibcReturnsUnknownWhenElfParsingFails() {
        byte[] invalidProbe = new byte[64];
        Arrays.fill(invalidProbe, (byte) 1);

        assertThrows(IOException.class, () -> PlatformDetector.detectLinuxLibc(invalidProbe));
    }

    @Test
    void detectLinuxLibcReturnsUnknownForTruncatedProgramHeader(@TempDir Path tempDir) throws IOException {
        byte[] malformedProbe = buildElf64ProbeWithInterp("/lib64/ld-linux-x86-64.so.2");
        writeLe64(malformedProbe, 32, malformedProbe.length - 1);
        writeLe16(malformedProbe, 54, 1);
        Path executable = tempDir.resolve("malformed-elf");
        Files.write(executable, malformedProbe);

        assertEquals(PlatformDetector.LinuxLibc.UNKNOWN, PlatformDetector.detectLinuxLibc(executable));
    }

    private static void withSystemProperties(String osName, String osArch, Runnable action) {
        String previousOsName = System.getProperty("os.name");
        String previousOsArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", osName);
            System.setProperty("os.arch", osArch);
            action.run();
        } finally {
            restoreProperty("os.name", previousOsName);
            restoreProperty("os.arch", previousOsArch);
        }
    }

    private static void withSystemProperty(String key, String value, Runnable action) {
        String previousValue = System.getProperty(key);
        try {
            System.setProperty(key, value);
            action.run();
        } finally {
            restoreProperty(key, previousValue);
        }
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static byte[] buildElf64ProbeWithInterp(String interpreterPath) {
        byte[] interpBytes = interpreterPath.getBytes(StandardCharsets.UTF_8);
        byte[] probe = new byte[512];

        probe[0] = 0x7F;
        probe[1] = 'E';
        probe[2] = 'L';
        probe[3] = 'F';
        probe[4] = 2;
        probe[5] = 1;

        int phoff = 64;
        int phentsize = 56;
        int phnum = 1;
        int interpOffset = 256;
        int interpSize = interpBytes.length + 1;

        writeLe64(probe, 32, phoff);
        writeLe16(probe, 54, phentsize);
        writeLe16(probe, 56, phnum);

        int pHeader = phoff;
        writeLe32(probe, pHeader, 3);
        writeLe64(probe, pHeader + 8, interpOffset);
        writeLe64(probe, pHeader + 32, interpSize);

        System.arraycopy(interpBytes, 0, probe, interpOffset, interpBytes.length);
        probe[interpOffset + interpBytes.length] = 0;
        return probe;
    }

    private static void writeLe16(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >>> 8) & 0xFF);
    }

    private static void writeLe32(byte[] buffer, int offset, int value) {
        buffer[offset] = (byte) (value & 0xFF);
        buffer[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        buffer[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        buffer[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    private static void writeLe64(byte[] buffer, int offset, long value) {
        for (int i = 0; i < 8; i++) {
            buffer[offset + i] = (byte) ((value >>> (8 * i)) & 0xFF);
        }
    }
}
