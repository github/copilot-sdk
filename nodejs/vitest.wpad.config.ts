import { statSync } from "node:fs";
import { isAbsolute } from "node:path";
import { defineConfig } from "vitest/config";
import base from "./vitest.config.js";
import { CAPI_PROXY_BUNDLE } from "./test/e2e/harness/proxyBundleContext.js";
import { requireWpadNamespace } from "./test/e2e/network/wpadNamespace.js";

requireWpadNamespace();
const bundle = process.env.COPILOT_SDK_WPAD_PROXY_BUNDLE;
if (!bundle || !isAbsolute(bundle) || !statSync(bundle).isFile()) {
    throw new Error("WPAD replay proxy must be built before entering the namespace");
}

export default defineConfig({
    ...base,
    test: {
        ...base.test,
        include: ["test/e2e/network/dns_wpad.e2e.test.ts"],
        exclude: base.test?.exclude?.filter(
            (pattern: string) => !pattern.includes("dns_wpad.e2e.test.ts")
        ),
        globalSetup: [],
        provide: { [CAPI_PROXY_BUNDLE]: bundle },
    },
});
