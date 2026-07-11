import type { JSONSchema7 } from "json-schema";
import { describe, expect, it } from "vitest";

import {
    collectDefinitionCollections,
    collectExperimentalOnlyRpcReferencedDefinitionNames,
    collectReachableDefinitionNames,
    findSharedSchemaDefinitions,
    getEnumValueDescriptions,
    inlineExternalSchemaDefinitions,
    isIntegerSchemaBoundedToInt32,
    rewriteSharedDefinitionReferences,
} from "../../scripts/codegen/utils.ts";

describe("shared schema definition codegen utilities", () => {
    it("detects integer schemas bounded to the 32-bit signed range", () => {
        expect(
            isIntegerSchemaBoundedToInt32({
                type: "integer",
                minimum: -2147483648,
                maximum: 2147483647,
            })
        ).toBe(true);
        expect(
            isIntegerSchemaBoundedToInt32({
                type: "integer",
                minimum: 0,
                maximum: 100,
            })
        ).toBe(true);
        expect(isIntegerSchemaBoundedToInt32({ type: "integer", maximum: 100 })).toBe(false);
        expect(isIntegerSchemaBoundedToInt32({ type: "integer", minimum: 0 })).toBe(false);
        expect(
            isIntegerSchemaBoundedToInt32({
                type: "integer",
                minimum: -2147483649,
                maximum: 100,
            })
        ).toBe(false);
        expect(
            isIntegerSchemaBoundedToInt32({
                type: "integer",
                minimum: 0,
                maximum: 2147483648,
            })
        ).toBe(false);
        expect(
            isIntegerSchemaBoundedToInt32({
                type: "integer",
                minimum: 0.5,
                maximum: 100,
            })
        ).toBe(false);
        expect(
            isIntegerSchemaBoundedToInt32({
                type: "integer",
                minimum: 0,
                maximum: 100.5,
            })
        ).toBe(false);
    });

    it("extracts non-empty enum value descriptions from schema extensions", () => {
        expect(
            getEnumValueDescriptions({
                type: "string",
                enum: ["start", "stop"],
                "x-enumDescriptions": {
                    start: " Start the operation. ",
                    stop: "",
                    ignored: 42,
                },
            } as JSONSchema7)
        ).toEqual({ start: "Start the operation." });

        expect(getEnumValueDescriptions({ type: "string", enum: ["start"] })).toBeUndefined();
    });

    it("rewrites reachable identical shared definitions without enum-only assumptions", () => {
        const sessionSchema: JSONSchema7 = {
            definitions: {
                SessionEvent: {
                    anyOf: [
                        {
                            type: "object",
                            required: ["type", "data"],
                            properties: {
                                type: { const: "session.start" },
                                data: {
                                    type: "object",
                                    required: ["payload", "reasoningSummary"],
                                    properties: {
                                        payload: { $ref: "#/definitions/SharedPayload" },
                                        reasoningSummary: {
                                            $ref: "#/definitions/ReasoningSummary",
                                        },
                                    },
                                },
                            },
                        },
                    ],
                },
                ReasoningSummary: {
                    type: "string",
                    enum: ["concise", "detailed"],
                    description: "Reasoning summary mode used for model calls.",
                    "x-enumDescriptions": {
                        concise: "Use concise session reasoning summaries.",
                        detailed: "Use detailed session reasoning summaries.",
                    },
                },
                SharedPayload: {
                    type: "object",
                    required: ["leaf"],
                    properties: {
                        leaf: { $ref: "#/definitions/SharedLeaf" },
                    },
                },
                SharedLeaf: {
                    type: "object",
                    required: ["value"],
                    properties: {
                        value: { type: "string" },
                    },
                },
                BrokenParent: {
                    type: "object",
                    properties: {
                        leaf: { $ref: "#/definitions/BrokenLeaf" },
                    },
                },
                BrokenLeaf: {
                    type: "string",
                    enum: ["session"],
                },
                UnusedShared: {
                    type: "object",
                    properties: {
                        value: { type: "string" },
                    },
                },
            },
        };
        const apiSchema = {
            definitions: {
                ReasoningSummary: {
                    type: "string",
                    enum: ["concise", "detailed"],
                    description: "Reasoning summary mode to request for supported model clients.",
                    "x-enumDescriptions": {
                        concise: "Request concise model reasoning summaries.",
                        detailed: "Request detailed model reasoning summaries.",
                    },
                },
                SharedPayload: {
                    type: "object",
                    required: ["leaf"],
                    properties: {
                        leaf: { $ref: "#/$defs/SharedLeaf" },
                    },
                },
                SharedLeaf: {
                    type: "object",
                    required: ["value"],
                    properties: {
                        value: { type: "string" },
                    },
                },
                BrokenParent: {
                    type: "object",
                    properties: {
                        leaf: { $ref: "#/definitions/BrokenLeaf" },
                    },
                },
                BrokenLeaf: {
                    type: "string",
                    enum: ["api"],
                },
                UnusedShared: {
                    type: "object",
                    properties: {
                        value: { type: "string" },
                    },
                },
            },
            $defs: {
                SharedLeaf: {
                    type: "object",
                    required: ["value"],
                    properties: {
                        value: { type: "string" },
                    },
                },
            },
            server: {
                test: {
                    rpcMethod: "test.shared",
                    params: {
                        type: "object",
                        properties: {
                            broken: { $ref: "#/definitions/BrokenParent" },
                            payload: { $ref: "#/definitions/SharedPayload" },
                            reasoningSummary: { $ref: "#/definitions/ReasoningSummary" },
                            unused: { $ref: "#/definitions/UnusedShared" },
                        },
                    },
                    result: { type: "null" },
                },
            },
        };

        const shared = findSharedSchemaDefinitions(
            apiSchema as Record<string, unknown>,
            sessionSchema as unknown as Record<string, unknown>
        );
        expect([...shared].sort()).toEqual([
            "ReasoningSummary",
            "SharedLeaf",
            "SharedPayload",
            "UnusedShared",
        ]);

        const reachable = collectReachableDefinitionNames(
            sessionSchema as unknown as Record<string, unknown>
        );
        for (const name of [...shared]) {
            if (!reachable.has(name)) shared.delete(name);
        }

        const rewritten = rewriteSharedDefinitionReferences(
            apiSchema,
            shared,
            "session-events.schema.json"
        ) as typeof apiSchema;

        expect(rewritten.definitions).not.toHaveProperty("ReasoningSummary");
        expect(rewritten.definitions).not.toHaveProperty("SharedPayload");
        expect(rewritten.definitions).not.toHaveProperty("SharedLeaf");
        expect(rewritten.definitions).toHaveProperty("BrokenParent");
        expect(rewritten.definitions).toHaveProperty("UnusedShared");
        expect(rewritten.server.test.params.properties.reasoningSummary.$ref).toBe(
            "session-events.schema.json#/definitions/ReasoningSummary"
        );
        expect(rewritten.server.test.params.properties.payload.$ref).toBe(
            "session-events.schema.json#/definitions/SharedPayload"
        );
        expect(rewritten.server.test.params.properties.broken.$ref).toBe(
            "#/definitions/BrokenParent"
        );
        expect(rewritten.server.test.params.properties.unused.$ref).toBe(
            "#/definitions/UnusedShared"
        );
    });

    it("inlines direct external refs with transitive definitions", () => {
        const sessionSchema: JSONSchema7 = {
            definitions: {
                SessionEvent: {
                    anyOf: [{ $ref: "#/definitions/SessionStartEvent" }],
                },
                SessionStartEvent: {
                    type: "object",
                    required: ["type", "data"],
                    properties: {
                        type: { const: "session.start" },
                        data: { $ref: "#/definitions/SessionStartData" },
                    },
                },
                SessionStartData: {
                    type: "object",
                    required: ["reasoningSummary", "shutdownType"],
                    properties: {
                        reasoningSummary: { $ref: "#/definitions/ReasoningSummary" },
                        shutdownType: { $ref: "#/definitions/ShutdownType" },
                    },
                },
                ReasoningSummary: {
                    type: "string",
                    enum: ["concise", "detailed"],
                },
                ShutdownType: {
                    type: "string",
                    enum: ["session"],
                },
            },
        };
        const apiSchema = {
            definitions: {
                EventsReadResult: {
                    type: "object",
                    required: ["events"],
                    properties: {
                        events: {
                            type: "array",
                            items: {
                                $ref: "session-events.schema.json#/definitions/SessionEvent",
                            },
                        },
                    },
                },
                ShutdownType: {
                    type: "string",
                    enum: ["api"],
                },
            },
        };

        const { schema: inlined, inlinedDefinitionNames } = inlineExternalSchemaDefinitions(
            apiSchema,
            sessionSchema as unknown as Record<string, unknown>,
            "session-events.schema.json",
            { conflictingDefinitionNamePrefix: "SessionEvents" }
        );

        expect([...inlinedDefinitionNames].sort()).toEqual([
            "ReasoningSummary",
            "SessionEvent",
            "SessionEventsShutdownType",
            "SessionStartData",
            "SessionStartEvent",
        ]);
        const inlinedDefinitions = inlined.definitions as Record<string, any>;
        expect(inlinedDefinitions.EventsReadResult.properties.events.items.$ref).toBe(
            "#/definitions/SessionEvent"
        );
        expect(inlinedDefinitions.SessionStartData.properties.reasoningSummary.$ref).toBe(
            "#/definitions/ReasoningSummary"
        );
        expect(inlinedDefinitions.SessionStartData.properties.shutdownType.$ref).toBe(
            "#/definitions/SessionEventsShutdownType"
        );
        expect(inlinedDefinitions.ShutdownType.enum).toEqual(["api"]);
        expect(inlinedDefinitions.SessionEventsShutdownType.enum).toEqual(["session"]);
    });

    it("collects only definitions referenced exclusively by experimental RPC methods", () => {
        const apiSchema = {
            definitions: {
                ExperimentalLeaf: {
                    type: "object",
                    properties: {
                        value: { type: "string" },
                    },
                },
                ExperimentalResult: {
                    type: "object",
                    properties: {
                        leaf: { $ref: "#/definitions/ExperimentalLeaf" },
                    },
                },
                ExperimentalSharedResult: {
                    type: "object",
                    properties: {
                        leaf: { $ref: "#/definitions/SharedLeaf" },
                    },
                },
                SharedLeaf: {
                    type: "object",
                    properties: {
                        value: { type: "string" },
                    },
                },
                StableResult: {
                    type: "object",
                    properties: {
                        leaf: { $ref: "#/definitions/SharedLeaf" },
                    },
                },
            },
            server: {
                stable: {
                    rpcMethod: "stable",
                    params: null,
                    result: { $ref: "#/definitions/StableResult" },
                },
                experimental: {
                    rpcMethod: "experimental",
                    params: null,
                    result: { $ref: "#/definitions/ExperimentalResult" },
                    stability: "experimental",
                },
                experimentalShared: {
                    rpcMethod: "experimental.shared",
                    params: null,
                    result: { $ref: "#/definitions/ExperimentalSharedResult" },
                    stability: "experimental",
                },
            },
        };

        const referenced = collectExperimentalOnlyRpcReferencedDefinitionNames(
            Object.values(apiSchema.server),
            collectDefinitionCollections(apiSchema as Record<string, unknown>)
        );

        expect([...referenced].sort()).toEqual([
            "ExperimentalLeaf",
            "ExperimentalResult",
            "ExperimentalSharedResult",
        ]);
    });
});
