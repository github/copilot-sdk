import { describe, expect, it } from "vitest";

import {
    isOmittableRequest,
    readLegacyParameters,
    readLegacyUntyped,
    validateLegacyRequests,
    validateLegacyUntypedMarkers,
} from "../../scripts/codegen/legacy-parameters.ts";
import { legacyRequestSchema } from "./legacy-parameters-fixture.ts";

describe("x-legacy-parameters", () => {
    it("leaves unmarked requests to their existing generation", () => {
        expect(readLegacyParameters(legacyRequestSchema(0).params, "sample.plan")).toBeUndefined();
    });

    it("keeps the annotation fixed across two successive optional additions", () => {
        const one = readLegacyParameters(legacyRequestSchema(1).params, "sample.plan");
        const two = readLegacyParameters(legacyRequestSchema(2).params, "sample.plan");
        expect(one?.legacy).toEqual(["contract", "source", "scope"]);
        expect(two?.legacy).toEqual(one?.legacy);
        expect(one?.additions).toEqual(["policySessionId"]);
        expect(two?.additions).toEqual(["policySessionId", "traceId"]);
        expect([...(two?.required ?? [])]).toEqual(["contract", "source"]);
    });

    it("excludes implicit session properties from both sets", () => {
        const parsed = readLegacyParameters(
            legacyRequestSchema(1, "session").params,
            "session.sample.plan",
            {
                implicit: ["sessionId"],
            }
        );
        expect(parsed?.legacy).toEqual(["contract", "source", "scope"]);
        expect(parsed?.additions).toEqual(["policySessionId"]);
        expect(parsed?.required.has("sessionId")).toBe(false);
    });

    it.each([
        ["a non-array annotation", "contract", {}, "expected an array"],
        ["duplicate names", ["contract", "contract", "source"], {}, "duplicate"],
        ["an unknown property", ["contract", "source", "missing"], {}, "unknown property missing"],
        ["an omitted required input", ["contract", "scope"], {}, "required property source"],
        [
            "no added property",
            ["contract", "source", "scope", "policySessionId"],
            {},
            "at least one property",
        ],
        ["a nullable request", ["contract", "source", "scope"], { nullable: true }, "nullable"],
    ] as const)("rejects %s", (_name, legacy, options, message) => {
        const { params } = legacyRequestSchema(1);
        params["x-legacy-parameters"] = legacy;
        expect(() => readLegacyParameters(params, "sample.plan", options)).toThrow(message);
    });

    it("rejects an implicit session property listed as a legacy parameter", () => {
        const { params } = legacyRequestSchema(1, "session");
        params["x-legacy-parameters"] = ["sessionId", "contract", "source"];
        expect(() =>
            readLegacyParameters(params, "session.sample.plan", { implicit: ["sessionId"] })
        ).toThrow("implicit property sessionId");
    });

    it("validates every section for generators with no extra projection", () => {
        type Methods = Record<string, { rpcMethod: string; params: unknown }>;
        const validate = (sections: Record<string, Methods>) =>
            validateLegacyRequests(
                sections,
                (node) => Object.values(node),
                (method) => method.params,
                () => false
            );
        const session = legacyRequestSchema(1, "session").params;
        expect(() =>
            validate({ session: { plan: { rpcMethod: "session.sample.plan", params: session } } })
        ).not.toThrow();

        const client = legacyRequestSchema(1).params;
        expect(() =>
            validate({ clientSession: { plan: { rpcMethod: "sample.plan", params: client } } })
        ).toThrow(
            "Invalid x-legacy-parameters for sample.plan: only server and session requests are supported"
        );

        const invalid = legacyRequestSchema(1).params;
        invalid["x-legacy-parameters"] = ["contract", "scope"];
        expect(() =>
            validate({ server: { plan: { rpcMethod: "sample.plan", params: invalid } } })
        ).toThrow("required property source must be a legacy parameter");
    });

    it("rejects optional requests before nullable ones", () => {
        const { params } = legacyRequestSchema(1);
        expect(() =>
            readLegacyParameters(params, "sample.plan", { optional: true, nullable: true })
        ).toThrow(
            "Invalid x-legacy-parameters for sample.plan: optional requests cannot declare legacy parameters"
        );
        expect(isOmittableRequest({ anyOf: [{ not: {} }, params] })).toBe(true);
        expect(isOmittableRequest(params)).toBe(false);
    });

    it("accepts only a true x-legacy-untyped property marker", () => {
        expect(readLegacyUntyped({ type: "array" }, "Plan.choices")).toBe(false);
        expect(readLegacyUntyped({ "x-legacy-untyped": true }, "Plan.choices")).toBe(true);
        expect(() => readLegacyUntyped({ "x-legacy-untyped": "yes" }, "Plan.choices")).toThrow(
            "Plan.choices: x-legacy-untyped must be true"
        );
    });
});

describe("x-legacy-untyped markers", () => {
    const document = (marker: unknown) => ({
        definitions: {
            Plan: {
                type: "object",
                properties: {
                    nested: {
                        type: "object",
                        properties: { choices: { type: "array", "x-legacy-untyped": marker } },
                    },
                },
            },
        },
    });

    it("accepts true and unmarked properties anywhere in the document", () => {
        expect(() => validateLegacyUntypedMarkers(document(true))).not.toThrow();
        expect(() =>
            validateLegacyUntypedMarkers({ definitions: { Plan: { type: "object" } } })
        ).not.toThrow();
    });

    it.each([false, "true", 1, null])(
        "rejects a malformed %j marker with its property path",
        (marker) => {
            expect(() => validateLegacyUntypedMarkers(document(marker), "api.schema.json")).toThrow(
                /api\.schema\.json\/definitions\/Plan.*\.choices: x-legacy-untyped must be true/
            );
        }
    );
});
