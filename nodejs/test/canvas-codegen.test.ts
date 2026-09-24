/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import {
    applySchemaRevision,
    loadCanvasSchemaRevisions,
    schemaFingerprint,
    type SchemaRevision,
} from "../../scripts/codegen/canvas-schema.ts";

const previousMethod = { params: null, result: { type: "null" } };
const approvedMethod = { params: null, result: { $ref: "#/definitions/Acknowledgement" } };
const revision: SchemaRevision = {
    replacements: [
        {
            path: ["server", "register"],
            beforeSha256: schemaFingerprint(previousMethod),
            value: approvedMethod,
        },
        {
            path: ["definitions", "Acknowledgement"],
            beforeSha256: null,
            value: { type: "object", properties: { contractVersion: { const: 1 } } },
        },
    ],
};

function releasedSchema() {
    return {
        server: {
            register: structuredClone(previousMethod),
            ping: { result: { type: "string" } },
        },
        definitions: {},
    };
}

describe("canvas schema revisions", () => {
    it("applies the exact reviewed changes without modifying release inputs", () => {
        const released = releasedSchema();
        const original = structuredClone(released);
        const actual = applySchemaRevision(released, revision);

        expect(actual.server.register).toEqual(approvedMethod);
        expect(actual.server.ping).toEqual(released.server.ping);
        expect(actual.definitions).toHaveProperty("Acknowledgement");
        expect(released).toEqual(original);
    });

    it("accepts an already matching release without modifying it", () => {
        const applied = applySchemaRevision(releasedSchema(), revision);

        expect(applySchemaRevision(applied, revision)).toEqual(applied);
    });

    it("does not depend on schema object key order", () => {
        expect(schemaFingerprint({ b: 2, a: 1 })).toBe(schemaFingerprint({ a: 1, b: 2 }));
    });

    it("refuses to overwrite a changed released method", () => {
        const released = releasedSchema();
        released.server.register.result.type = "object";

        expect(() => applySchemaRevision(released, revision)).toThrow(
            "Canvas schema revision mismatch at server/register"
        );
    });

    it("refuses an unexpected definition even when an earlier replacement succeeded", () => {
        const released = {
            ...releasedSchema(),
            definitions: { ...releasedSchema().definitions, Acknowledgement: { type: "string" } },
        };
        const original = structuredClone(released);

        expect(() => applySchemaRevision(released, revision)).toThrow(
            "Canvas schema revision mismatch at definitions/Acknowledgement"
        );
        expect(released).toEqual(original);
    });

    it("refuses a missing parent instead of creating a new API namespace", () => {
        expect(() => applySchemaRevision({ definitions: {} }, revision)).toThrow(
            "Missing canvas schema parent: server/register"
        );
    });

    it.each([
        { path: [] },
        { path: ["__proto__", "polluted"] },
        { path: ["constructor", "prototype", "polluted"] },
    ])("refuses an invalid schema path $path", ({ path }) => {
        expect(() =>
            applySchemaRevision(
                {},
                {
                    replacements: [{ path, beforeSha256: null, value: true }],
                }
            )
        ).toThrow("Invalid canvas schema path");
    });

    it("projects only launch admission, with no session or event changes", async () => {
        const canvas = await loadCanvasSchemaRevisions();

        expect(canvas.api.replacements.map(({ path }) => path.join("/"))).toEqual([
            "definitions/ExtensionLaunchProfile",
            "definitions/ExtensionLaunchProviderRegistrationResult",
            "definitions/ExtensionLaunchProviderResolveRequest",
            "definitions/ExtensionLaunchProviderResolveResult",
            "definitions/ExtensionSource",
            "server/registerExtensionLaunchProvider",
            "clientGlobal/extensionLaunchProvider/resolve",
        ]);
        expect(canvas).not.toHaveProperty("sessionEvents");
        expect(canvas.api).not.toHaveProperty("insertions");
    });
});
