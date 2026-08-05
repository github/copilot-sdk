package com.github.copilot.spike.platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for platform detection spike.
 */
public final class PlatformDetectionSpikeMain {
    private static final Logger LOGGER = Logger.getLogger(PlatformDetectionSpikeMain.class.getName());

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
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Unable to parse PT_INTERP from /proc/self/exe: " + ex.getMessage(), ex);
            }
        }
    }
}
