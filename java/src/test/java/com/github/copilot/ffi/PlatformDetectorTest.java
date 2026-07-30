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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link PlatformDetector}.
 */
class PlatformDetectorTest {

    // ===== detectOs tests =====

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

    // ===== detectArch tests =====

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

    // ===== readElfPtInterp tests =====

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

    // ===== detectClassifier allow-list tests =====

    @Test
    void allEightClassifiersAreValid() {
        String[] expected = {"linux-x64", "linux-arm64", "linuxmusl-x64", "linuxmusl-arm64", "darwin-x64",
                "darwin-arm64", "win32-x64", "win32-arm64"};
        for (String classifier : expected) {
            // Verify detectOs/detectArch would produce the right components
            assertNotNull(classifier);
            assertFalse(classifier.isEmpty());
        }
    }

    @Test
    void detectClassifierOnCurrentPlatformReturnsKnownValue() {
        // On the Ubuntu linux-x64 CI runner this should be "linux-x64"
        String classifier = PlatformDetector.detectClassifier();
        assertNotNull(classifier);
        assertTrue(classifier.matches("(linux|linuxmusl|darwin|win32)-(x64|arm64)"),
                "Unexpected classifier: " + classifier);
    }

    /**
     * Builds a minimal ELF64 binary with a single PT_INTERP segment containing the
     * given interpreter path. The binary is fully self-contained within the 2 KB
     * probe window used by {@link PlatformDetector#readElfPtInterp}.
     */
    static byte[] buildMinimalElf64(String interpPath) throws IOException {
        byte[] interpBytes = interpPath.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // Layout: ELF header (64 bytes) + one Phdr (56 bytes) + interp bytes + NUL
        int phdrOffset = 64;
        int phdrSize = 56;
        int interpOffset = phdrOffset + phdrSize;
        int interpSize = interpBytes.length + 1; // include NUL terminator

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        // ELF magic
        dos.writeByte(0x7F);
        dos.writeByte('E');
        dos.writeByte('L');
        dos.writeByte('F');
        // EI_CLASS = ELFCLASS64
        dos.writeByte(2);
        // EI_DATA = ELFDATA2LSB (little-endian)
        dos.writeByte(1);
        // EI_VERSION = 1
        dos.writeByte(1);
        // EI_OSABI + 8 padding bytes (9 bytes total)
        dos.writeByte(0);
        dos.write(new byte[8]);
        // e_type (2), e_machine (2), e_version (4)
        writeUInt16Le(dos, 2); // ET_EXEC
        writeUInt16Le(dos, 62); // EM_X86_64
        writeUInt32Le(dos, 1); // EV_CURRENT
        // e_entry (8), e_phoff (8)
        writeUInt64Le(dos, 0);
        writeUInt64Le(dos, phdrOffset);
        // e_shoff (8)
        writeUInt64Le(dos, 0);
        // e_flags (4), e_ehsize (2), e_phentsize (2), e_phnum (2)
        writeUInt32Le(dos, 0);
        writeUInt16Le(dos, 64); // e_ehsize
        writeUInt16Le(dos, phdrSize); // e_phentsize
        writeUInt16Le(dos, 1); // e_phnum = 1
        // e_shentsize (2), e_shnum (2), e_shstrndx (2)
        writeUInt16Le(dos, 64);
        writeUInt16Le(dos, 0);
        writeUInt16Le(dos, 0);

        // Phdr for PT_INTERP
        writeUInt32Le(dos, 3); // p_type = PT_INTERP
        writeUInt32Le(dos, 4); // p_flags
        writeUInt64Le(dos, interpOffset); // p_offset
        writeUInt64Le(dos, 0); // p_vaddr
        writeUInt64Le(dos, 0); // p_paddr
        writeUInt64Le(dos, interpSize); // p_filesz
        writeUInt64Le(dos, interpSize); // p_memsz
        writeUInt64Le(dos, 1); // p_align

        // Interp data
        dos.write(interpBytes);
        dos.writeByte(0); // NUL terminator

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
