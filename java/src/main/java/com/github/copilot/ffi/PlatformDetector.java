/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.ffi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Detects the current platform's OS, architecture, and (on Linux) C library
 * variant, and derives the classifier string used to locate the correct
 * {@code runtime.node} native binary.
 *
 * <p>
 * The 8 supported classifiers are: {@code linux-x64}, {@code linux-arm64},
 * {@code linuxmusl-x64}, {@code linuxmusl-arm64}, {@code darwin-x64},
 * {@code darwin-arm64}, {@code win32-x64}, {@code win32-arm64}.
 */
public final class PlatformDetector {

    private static final int ELF_HEADER_PROBE_BYTES = 2048;
    private static final int ELF_MAGIC_0 = 0x7F;
    private static final int ELF_MAGIC_1 = 'E';
    private static final int ELF_MAGIC_2 = 'L';
    private static final int ELF_MAGIC_3 = 'F';
    private static final int ELF_CLASS_32 = 1;
    private static final int ELF_CLASS_64 = 2;
    private static final int ELF_DATA_LITTLE_ENDIAN = 1;
    private static final int ELF_DATA_BIG_ENDIAN = 2;
    private static final int PT_INTERP = 3;

    /** Linux C library variant detected via ELF PT_INTERP parsing. */
    public enum LinuxLibc {
        /** GNU/glibc dynamic linker detected. */
        GLIBC,
        /** musl libc dynamic linker detected. */
        MUSL,
        /** PT_INTERP found but not recognised as glibc or musl. */
        UNKNOWN,
        /** Not running on Linux; ELF parsing is not applicable. */
        NOT_APPLICABLE
    }

    private PlatformDetector() {
    }

    /**
     * Returns the OS identifier for the current platform.
     *
     * @return {@code "darwin"}, {@code "linux"}, or {@code "win32"}
     * @throws IllegalStateException
     *             if the OS is not recognised
     */
    public static String detectOs() {
        return detectOs(System.getProperty("os.name", ""));
    }

    /**
     * Returns the OS identifier for the given {@code os.name} value.
     *
     * @param osName
     *            the value of the {@code os.name} system property
     * @return {@code "darwin"}, {@code "linux"}, or {@code "win32"}
     * @throws IllegalStateException
     *             if the value is not recognised
     */
    static String detectOs(String osName) {
        String normalized = osName.toLowerCase(Locale.ROOT);
        if (normalized.contains("mac") || normalized.contains("darwin")) {
            return "darwin";
        }
        if (normalized.contains("win")) {
            return "win32";
        }
        if (normalized.contains("linux")) {
            return "linux";
        }
        throw new IllegalStateException("Unsupported os.name: " + osName);
    }

    /**
     * Returns the architecture identifier for the current platform.
     *
     * @return {@code "x64"} or {@code "arm64"}
     * @throws IllegalStateException
     *             if the architecture is not recognised
     */
    public static String detectArch() {
        return detectArch(System.getProperty("os.arch", ""));
    }

    /**
     * Returns the architecture identifier for the given {@code os.arch} value.
     *
     * @param osArch
     *            the value of the {@code os.arch} system property
     * @return {@code "x64"} or {@code "arm64"}
     * @throws IllegalStateException
     *             if the value is not recognised
     */
    static String detectArch(String osArch) {
        String normalized = osArch.toLowerCase(Locale.ROOT).replace('-', '_');
        if (normalized.equals("amd64") || normalized.equals("x86_64") || normalized.equals("x64")) {
            return "x64";
        }
        if (normalized.equals("aarch64") || normalized.equals("arm64")) {
            return "arm64";
        }
        throw new IllegalStateException("Unsupported os.arch: " + osArch);
    }

    /**
     * Detects the Linux C library variant by parsing the ELF {@code PT_INTERP}
     * segment of {@code /proc/self/exe}.
     *
     * @return {@link LinuxLibc#NOT_APPLICABLE} when not running on Linux,
     *         {@link LinuxLibc#MUSL} when the musl dynamic linker is detected,
     *         {@link LinuxLibc#GLIBC} when the glibc dynamic linker is detected, or
     *         {@link LinuxLibc#UNKNOWN} when parsing fails or the interpreter path
     *         is not recognised
     */
    public static LinuxLibc detectLinuxLibc() {
        if (!"linux".equals(detectOs())) {
            return LinuxLibc.NOT_APPLICABLE;
        }
        try {
            String interpreter = readElfPtInterp(Path.of("/proc/self/exe"));
            if (interpreter.contains("/ld-musl-")) {
                return LinuxLibc.MUSL;
            }
            if (interpreter.contains("/ld-linux-")) {
                return LinuxLibc.GLIBC;
            }
            return LinuxLibc.UNKNOWN;
        } catch (IOException ex) {
            return LinuxLibc.UNKNOWN;
        }
    }

    /**
     * Returns the classifier string for the current platform, combining OS,
     * architecture, and (on Linux) C library variant.
     *
     * @return one of the 8 supported classifier strings, e.g. {@code "linux-x64"}
     * @throws IllegalStateException
     *             if the platform is not recognised or not supported
     */
    public static String detectClassifier() {
        String os = detectOs();
        String arch = detectArch();
        if (!"linux".equals(os)) {
            String classifier = os + "-" + arch;
            validateClassifier(classifier);
            return classifier;
        }
        LinuxLibc libc = detectLinuxLibc();
        String classifier = (libc == LinuxLibc.MUSL ? "linuxmusl-" : "linux-") + arch;
        validateClassifier(classifier);
        return classifier;
    }

    private static void validateClassifier(String classifier) {
        switch (classifier) {
            case "linux-x64" :
            case "linux-arm64" :
            case "linuxmusl-x64" :
            case "linuxmusl-arm64" :
            case "darwin-x64" :
            case "darwin-arm64" :
            case "win32-x64" :
            case "win32-arm64" :
                return;
            default :
                throw new IllegalStateException("Unsupported platform classifier: " + classifier);
        }
    }

    /**
     * Reads the ELF {@code PT_INTERP} segment (dynamic linker path) from the given
     * executable path.
     *
     * @param executablePath
     *            path to the ELF executable to inspect
     * @return the null-terminated interpreter string, without the trailing NUL
     * @throws IOException
     *             if the file cannot be read or is not a recognised ELF binary with
     *             a {@code PT_INTERP} segment within the probe window
     */
    static String readElfPtInterp(Path executablePath) throws IOException {
        byte[] probe = readPrefix(executablePath, ELF_HEADER_PROBE_BYTES);
        int size = probe.length;
        if (size < 64) {
            throw new IOException("ELF probe too small: " + size + " bytes");
        }
        if ((probe[0] & 0xFF) != ELF_MAGIC_0 || (probe[1] & 0xFF) != ELF_MAGIC_1 || (probe[2] & 0xFF) != ELF_MAGIC_2
                || (probe[3] & 0xFF) != ELF_MAGIC_3) {
            throw new IOException("Not an ELF executable: " + executablePath);
        }

        int elfClass = probe[4] & 0xFF;
        int elfData = probe[5] & 0xFF;
        if (elfData != ELF_DATA_LITTLE_ENDIAN && elfData != ELF_DATA_BIG_ENDIAN) {
            throw new IOException("Unsupported ELF data encoding: " + elfData);
        }
        boolean littleEndian = elfData == ELF_DATA_LITTLE_ENDIAN;

        long phoff;
        int phentsize;
        int phnum;
        if (elfClass == ELF_CLASS_64) {
            phoff = readUInt64(probe, 32, littleEndian);
            phentsize = readUInt16(probe, 54, littleEndian);
            phnum = readUInt16(probe, 56, littleEndian);
        } else if (elfClass == ELF_CLASS_32) {
            phoff = readUInt32(probe, 28, littleEndian);
            phentsize = readUInt16(probe, 42, littleEndian);
            phnum = readUInt16(probe, 44, littleEndian);
        } else {
            throw new IOException("Unsupported ELF class: " + elfClass);
        }

        if (phoff < 0 || phoff >= size) {
            throw new IOException("Program header table offset outside probe window: " + phoff);
        }
        if (phentsize <= 0 || phnum <= 0) {
            throw new IOException("Invalid ELF program header metadata: phentsize=" + phentsize + ", phnum=" + phnum);
        }

        for (int i = 0; i < phnum; i++) {
            long baseLong = phoff + ((long) i * phentsize);
            if (baseLong < 0 || baseLong > Integer.MAX_VALUE) {
                break;
            }
            int base = (int) baseLong;
            if (base + phentsize > size) {
                break;
            }

            long pType = readUInt32(probe, base, littleEndian);
            if (pType != PT_INTERP) {
                continue;
            }

            long pOffset;
            long pFileSize;
            if (elfClass == ELF_CLASS_64) {
                pOffset = readUInt64(probe, base + 8, littleEndian);
                pFileSize = readUInt64(probe, base + 32, littleEndian);
            } else {
                pOffset = readUInt32(probe, base + 4, littleEndian);
                pFileSize = readUInt32(probe, base + 16, littleEndian);
            }

            if (pOffset < 0 || pFileSize <= 0 || pOffset > Integer.MAX_VALUE || pFileSize > Integer.MAX_VALUE) {
                throw new IOException("Invalid PT_INTERP bounds");
            }

            int start = (int) pOffset;
            int end = start + (int) pFileSize;
            if (end > size) {
                throw new IOException("PT_INTERP extends past probe window; increase probe size");
            }

            int nulIndex = start;
            while (nulIndex < end && probe[nulIndex] != 0) {
                nulIndex++;
            }
            if (nulIndex == start) {
                throw new IOException("Empty PT_INTERP segment");
            }
            return new String(probe, start, nulIndex - start, StandardCharsets.UTF_8);
        }

        throw new IOException("ELF PT_INTERP segment not found");
    }

    private static byte[] readPrefix(Path path, int maxBytes) throws IOException {
        byte[] buffer = new byte[maxBytes];
        int total = 0;
        try (InputStream in = Files.newInputStream(path)) {
            while (total < maxBytes) {
                int read = in.read(buffer, total, maxBytes - total);
                if (read < 0) {
                    break;
                }
                total += read;
            }
        }
        byte[] resized = new byte[total];
        System.arraycopy(buffer, 0, resized, 0, total);
        return resized;
    }

    private static int readUInt16(byte[] data, int offset, boolean littleEndian) {
        int b0 = data[offset] & 0xFF;
        int b1 = data[offset + 1] & 0xFF;
        return littleEndian ? (b0 | (b1 << 8)) : ((b0 << 8) | b1);
    }

    private static long readUInt32(byte[] data, int offset, boolean littleEndian) {
        long b0 = data[offset] & 0xFFL;
        long b1 = data[offset + 1] & 0xFFL;
        long b2 = data[offset + 2] & 0xFFL;
        long b3 = data[offset + 3] & 0xFFL;
        if (littleEndian) {
            return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
        }
        return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
    }

    private static long readUInt64(byte[] data, int offset, boolean littleEndian) {
        long result = 0L;
        if (littleEndian) {
            for (int i = 7; i >= 0; i--) {
                result = (result << 8) | (data[offset + i] & 0xFFL);
            }
            return result;
        }
        for (int i = 0; i < 8; i++) {
            result = (result << 8) | (data[offset + i] & 0xFFL);
        }
        return result;
    }
}
