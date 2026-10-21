/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// Local-only by design: this matrix is not referenced by any repository vitest
// config or workflow.

import { fileURLToPath } from "node:url";

import { matrixTimeoutMs } from "./budget.ts";

export default {
    root: fileURLToPath(new URL("../../../../", import.meta.url)),
    cacheDir: fileURLToPath(new URL("./node_modules/.vite", import.meta.url)),
    test: {
        include: ["rust/tests/fixtures/connector-runtime/fixture.test.ts"],
        pool: "forks",
        testTimeout: matrixTimeoutMs,
    },
};
