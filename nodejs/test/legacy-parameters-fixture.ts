import type { JSONSchema7 } from "json-schema";

import type { ApiSchema, RpcMethod } from "../../scripts/codegen/utils.ts";

/**
 * A server request that opts into `x-legacy-parameters`, published first with
 * `contract`, `source` and `scope`, then extended by `additions` optional properties
 * (`policySessionId`, then `traceId`) without changing the annotation.
 */
export function legacyRequestSchema(additions: 0 | 1 | 2, scope: "server" | "session" = "server") {
    const added: Record<string, JSONSchema7> = {};
    if (additions >= 1) added.policySessionId = { type: "string" };
    if (additions >= 2) added.traceId = { type: "string" };
    const params: JSONSchema7 & Record<string, unknown> = {
        title: "SamplePlanRequest",
        type: "object",
        properties: {
            ...(scope === "session" ? { sessionId: { type: "string" } } : {}),
            contract: { type: "string" },
            source: { type: "string" },
            scope: { type: "string" },
            ...added,
        },
        required: [...(scope === "session" ? ["sessionId"] : []), "contract", "source"],
        additionalProperties: false,
    };
    if (additions > 0) params["x-legacy-parameters"] = ["contract", "source", "scope"];
    const methods: Record<string, RpcMethod> = {
        plan: {
            rpcMethod: `${scope === "session" ? "session." : ""}sample.plan`,
            params,
            result: { type: "object", properties: { ok: { type: "boolean" } }, required: ["ok"] },
        },
    };
    const schema: ApiSchema = { [scope]: { sample: methods } };
    return { schema, params };
}
