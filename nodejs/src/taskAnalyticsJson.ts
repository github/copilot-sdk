/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { TrajectoryJsonValue } from "./generated/rpc.js";

/** @internal */
export const rawHistoryResponse = Symbol("rawHistoryResponse");

/**
 * Parses archive JSON without rounding integers outside JavaScript's safe range.
 * Such integers become bigint values, supported by taskAnalytics.classifyTrajectory.
 * Pass an array directly as request events, or wrap a single ATIF document in an array.
 * @experimental
 */
export async function parseTrajectoryJson(text: string): Promise<TrajectoryJsonValue> {
    const { parse, parseNumberAndBigInt } = await import("lossless-json");
    return parse(text, undefined, (token) => {
        const value = parseNumberAndBigInt(token);
        return typeof value === "bigint" && Number.isSafeInteger(Number(value))
            ? Number(value)
            : value;
    }) as TrajectoryJsonValue;
}
