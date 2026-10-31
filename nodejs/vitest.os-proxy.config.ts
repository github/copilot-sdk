import { defineConfig } from "vitest/config";
import base from "./vitest.config.js";
import { requireDisposableProxyRunner } from "./test/e2e/network/osProxySettings.js";

requireDisposableProxyRunner();

export default defineConfig({
    ...base,
    test: {
        ...base.test,
        include: ["test/e2e/network/os_proxy.e2e.test.ts"],
        exclude: base.test?.exclude?.filter(
            (pattern: string) => !pattern.includes("os_proxy.e2e.test.ts")
        ),
    },
});
