package com.github.copilot.spike.platform;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for platform detection spike on win32-x64.
 */
public final class PlatformDetectionSpikeMain {
    private static final Logger LOGGER = Logger.getLogger(PlatformDetectionSpikeMain.class.getName());

    private PlatformDetectionSpikeMain() {
    }

    public static void main(String[] args) {
        configureLogging();

        String os = PlatformDetector.detectOs();
        String arch = PlatformDetector.detectArch();
        PlatformDetector.LinuxLibc libc = PlatformDetector.detectLinuxLibc();
        String classifier = PlatformDetector.detectClassifier();

        LOGGER.log(Level.INFO, "Detected os={0}", os);
        LOGGER.log(Level.INFO, "Detected arch={0}", arch);
        LOGGER.log(Level.INFO, "Detected linuxLibc={0}", libc);
        LOGGER.log(Level.INFO, "Detected classifier={0}", classifier);

        if ("win32-x64".equals(classifier)) {
            LOGGER.log(Level.INFO, "PASS: classifier matches expected win32-x64 target");
        } else {
            LOGGER.log(Level.WARNING, "UNEXPECTED: classifier mismatch for this spike, got {0}", classifier);
        }

        LOGGER.log(Level.INFO, "--- Spike result ---");
        LOGGER.log(Level.INFO, "Classifier for native binary selection: {0}", classifier);
        LOGGER.log(Level.INFO, "Resource path would be: native/{0}/runtime.node", classifier);
    }

    private static void configureLogging() {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        for (Handler handler : root.getHandlers()) {
            handler.setLevel(Level.INFO);
        }
    }
}
