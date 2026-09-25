/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { resolve } from "node:path";
import { describe, expect, it } from "vitest";
import { createCanvas, type CanvasDeclaration } from "../src/canvas.js";

describe("createCanvas", () => {
    it.each(["icons/counter.png", resolve("icons", "counter.png")])(
        "preserves the icon path %s in the wire declaration",
        (icon) => {
            const action = { name: "increment", description: "Increment the counter" };
            const declaration: CanvasDeclaration = {
                id: "counter",
                displayName: "Counter",
                description: "Count things",
                icon,
                inputSchema: { type: "object" },
                actions: [action],
            };
            const open = () => ({ url: "https://example.test/counter" });
            const handler = () => ({ count: 1 });
            const canvas = createCanvas({
                ...declaration,
                icon,
                actions: [{ ...action, handler }],
                open,
            });

            expect(canvas.declaration.icon).toBe(icon);
            expect(JSON.parse(JSON.stringify(canvas.declaration))).toEqual(declaration);
            expect(canvas.open).toBe(open);
            expect(canvas.actionHandlers.get("increment")).toBe(handler);
        }
    );

    it("omits an unspecified icon from the wire declaration", () => {
        const canvas = createCanvas({
            id: "counter",
            displayName: "Counter",
            description: "Count things",
            open: () => ({ url: "https://example.test/counter" }),
        });

        expect(canvas.declaration.icon).toBeUndefined();
        expect(JSON.parse(JSON.stringify(canvas.declaration))).toEqual({
            id: "counter",
            displayName: "Counter",
            description: "Count things",
        });
    });
});
