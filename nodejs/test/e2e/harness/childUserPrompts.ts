/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import type { CopilotRequestContext } from "../../../src/index.js";
import { isObject } from "../../../../test/harness/modelProtocolAdapterShared.js";

export function childUserPrompts(
    requestUrl: string,
    requestBody: string,
    context: Pick<CopilotRequestContext, "parentAgentId">
): string[] {
    if (!context.parentAgentId) {
        return [];
    }
    const isResponses = new URL(requestUrl).pathname.endsWith("/responses");
    const body = JSON.parse(requestBody) as {
        messages?: unknown[];
        input?: string | unknown[];
    };
    const messages = isResponses
        ? typeof body.input === "string"
            ? [{ role: "user", content: body.input }]
            : (body.input ?? [])
        : (body.messages ?? []);
    return messages.flatMap((message) => {
        if (!isObject(message) || message.role !== "user") return [];
        if (typeof message.content === "string") return [message.content];
        if (!Array.isArray(message.content)) return [];
        return message.content
            .filter(
                (part): part is { type: "text" | "input_text"; text: string } =>
                    isObject(part) &&
                    part.type === (isResponses ? "input_text" : "text") &&
                    typeof part.text === "string"
            )
            .map((part) => part.text);
    });
}
