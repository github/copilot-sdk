/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, vi } from "vitest";
import { withTestCleanup } from "./helpers/withTestCleanup.js";

describe("withTestCleanup", () => {
    it("returns the body result after every cleanup", async () => {
        const first = vi.fn();
        const second = vi.fn();
        await expect(withTestCleanup(async () => 42, first, second)).resolves.toBe(42);
        expect(first).toHaveBeenCalledOnce();
        expect(second).toHaveBeenCalledOnce();
    });

    it("preserves the original failure when cleanup succeeds", async () => {
        const failure = new Error("original assertion");
        const cleanup = vi.fn();
        await expect(
            withTestCleanup(async () => {
                throw failure;
            }, cleanup)
        ).rejects.toBe(failure);
        expect(cleanup).toHaveBeenCalledOnce();
    });

    it("preserves body and all teardown failures in order", async () => {
        const failure = new Error("original assertion");
        const first = new Error("server teardown");
        const second = new Error("directory teardown");
        await expect(
            withTestCleanup(
                async () => {
                    throw failure;
                },
                () => {
                    throw first;
                },
                async () => {
                    throw second;
                }
            )
        ).rejects.toMatchObject({
            errors: [failure, first, second],
            cause: failure,
        });
    });

    it("fails an otherwise-passing test when teardown fails, without skipping later cleanup", async () => {
        const failure = new Error("server teardown");
        const last = vi.fn();
        await expect(
            withTestCleanup(
                async () => undefined,
                () => {
                    throw failure;
                },
                last
            )
        ).rejects.toBe(failure);
        expect(last).toHaveBeenCalledOnce();
    });

    it("aggregates multiple teardown failures without inventing a body failure", async () => {
        const first = new Error("server teardown");
        const second = new Error("directory teardown");
        await expect(
            withTestCleanup(
                async () => undefined,
                () => {
                    throw first;
                },
                () => {
                    throw second;
                }
            )
        ).rejects.toMatchObject({ errors: [first, second] });
    });
});
