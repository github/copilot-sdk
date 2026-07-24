package com.github.copilot.spike.platform;

import com.sun.jna.Library;
import com.sun.jna.Native;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for platform detection spike on linux-x64.
 *
 * Demonstrates that we can read /proc/self/exe, parse the ELF PT_INTERP
 * segment, and determine whether the JVM is linked against glibc or musl.
 */
public final class PlatformDetectionSpikeMain {
    private static final Logger LOGGER = Logger.getLogger(PlatformDetectionSpikeMain.class.getName());

    private interface PermissionProbeLibrary extends Library {
        int permission_probe_value();
    }

    private PlatformDetectionSpikeMain() {
    }

    public static void main(String[] args) {
        String os = PlatformDetector.detectOs();
        String arch = PlatformDetector.detectArch();
        PlatformDetector.LinuxLibc libc = PlatformDetector.detectLinuxLibc();
        String classifier = PlatformDetector.detectClassifier();

        LOGGER.log(Level.INFO, "Detected os={0}", os);
        LOGGER.log(Level.INFO, "Detected arch={0}", arch);
        LOGGER.log(Level.INFO, "Detected linuxLibc={0}", libc);
        LOGGER.log(Level.INFO, "Detected classifier={0}", classifier);

        if ("linux".equals(os)) {
            try {
                String interp = PlatformDetector.readElfPtInterp(Path.of("/proc/self/exe"));
                LOGGER.log(Level.INFO, "ELF PT_INTERP={0}", interp);

                // Verify expectations for linux-x64 glibc
                if (interp.contains("/ld-linux-x86-64.so.2")) {
                    LOGGER.log(Level.INFO, "PASS: PT_INTERP matches expected glibc x86_64 dynamic linker");
                } else if (interp.contains("/ld-musl-x86_64.so.1")) {
                    LOGGER.log(Level.INFO, "PASS: PT_INTERP matches expected musl x86_64 dynamic linker");
                } else {
                    LOGGER.log(Level.WARNING, "UNEXPECTED: PT_INTERP does not match known linker paths: {0}", interp);
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Unable to parse PT_INTERP from /proc/self/exe: " + ex.getMessage(), ex);
            }
        } else {
            LOGGER.log(Level.INFO, "Not on Linux — skipping ELF PT_INTERP parsing");
        }

        LOGGER.log(Level.INFO, "--- Spike result ---");
        LOGGER.log(Level.INFO, "Classifier for native binary selection: {0}", classifier);
        LOGGER.log(Level.INFO, "Resource path would be: native/{0}/runtime.node", classifier);

        if (args.length == 1) {
            verifySharedLibraryLoadsWithoutExecutePermission(Path.of(args[0]));
        } else {
            LOGGER.log(Level.INFO, "No shared-library path supplied; skipping the mode-0644 JNA load probe");
        }
    }

    private static void verifySharedLibraryLoadsWithoutExecutePermission(Path libraryPath) {
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(libraryPath);
            boolean executable = permissions.contains(PosixFilePermission.OWNER_EXECUTE)
                    || permissions.contains(PosixFilePermission.GROUP_EXECUTE)
                    || permissions.contains(PosixFilePermission.OTHERS_EXECUTE);
            if (executable) {
                throw new IllegalStateException("Permission probe library unexpectedly has an execute bit: " + permissions);
            }

            PermissionProbeLibrary library = Native.load(
                    libraryPath.toAbsolutePath().toString(), PermissionProbeLibrary.class);
            int value = library.permission_probe_value();
            if (value != 42) {
                throw new IllegalStateException("Permission probe returned " + value + " instead of 42");
            }
            LOGGER.log(Level.INFO, "PASS: JNA loaded and invoked a shared library with permissions {0}", permissions);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to inspect shared-library permissions", ex);
        }
    }
}
