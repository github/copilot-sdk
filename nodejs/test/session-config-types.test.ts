/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { expectTypeOf, it } from "vitest";
import type { ResumeSessionConfig, SessionConfig } from "../src/index.js";

// Included in tsconfig.test.json so these public API assertions are compile-checked.
it("exposes refreshCustomInstructions only on the creation config", () => {
    expectTypeOf<SessionConfig>()
        .toHaveProperty("refreshCustomInstructions")
        .toEqualTypeOf<boolean | undefined>();
    expectTypeOf<ResumeSessionConfig>().not.toHaveProperty("refreshCustomInstructions");
});
