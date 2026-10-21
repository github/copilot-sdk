/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { it } from "../../../../nodejs/node_modules/vitest/dist/index.js";

import { matrixTimeoutMs } from "./budget.ts";

it(
    "exercises the real public Rust SDK Connector contract",
    async () => {
        await import("./fixture.ts");
    },
    matrixTimeoutMs,
);
