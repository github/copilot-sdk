import type { JSONSchema7 } from "json-schema";
import { describe, expect, it } from "vitest";

import {
    collectReachableDefinitionNames,
    findSharedSchemaDefinitions,
    inlineExternalSchemaDefinitions,
    rewriteSharedDefinitionReferences,
} from "../../scripts/codegen/utils.ts";

describe("shared schema definition codegen utilities", () => {
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
});
