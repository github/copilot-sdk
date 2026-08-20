import { defineConfig } from "vitest/config";

const integrationTestTimeout = process.platform === "win32" ? 60000 : 30000;

export default defineConfig({
    test: {
        globals: true,
        environment: "node",
        testTimeout: integrationTestTimeout,
        hookTimeout: integrationTestTimeout,
        teardownTimeout: 10000,
        isolate: true, // Run each test file in isolation
        pool: "forks", // Use process forking for better isolation
        // Exclude our ad-hoc test files that aren't vitest-based
        exclude: [
            "**/node_modules/**",
            "**/dist/**",
            "**/*.d.ts",
            "**/basic-test.ts", // Old manual test
        ],
    },
});
