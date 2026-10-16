/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotClient } from "../src/index.js";

// Validate host-supplied policies before creating a session.
export async function validateContentExclusionPolicies(client: CopilotClient) {
    const policiesJson = JSON.stringify([
        {
            rules: [
                {
                    paths: ["secrets/**"],
                    source: { name: "Example", type: "Organization" },
                    futureRuleField: { enabled: true },
                },
            ],
            last_updated_at: "2026-10-05T12:00:00Z",
            scope: "all",
            futurePolicyField: { nullable: null },
        },
    ]);
    const result = await client.rpc.contentExclusion.validatePolicies({ policiesJson });
    console.log("Validated content-exclusion policies:", JSON.stringify(result.policies));
    return result;
}
