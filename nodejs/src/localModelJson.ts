/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { Message, type ResponseMessage } from "vscode-jsonrpc/node";
import type { LocalModelDescriptor, LocalModelsSnapshot } from "./generated/rpc.js";
import { parseTrajectoryJson, rawHistoryResponse } from "./taskAnalyticsJson.js";

/** @internal */
export function createLocalModelJsonCodec() {
    const pending = new Map<
        string | number | null,
        { method: string; json: typeof import("lossless-json") }
    >();
    const snapshots = new Set([
        "localModels.configure",
        "localModels.inspect",
        "localModels.watch",
    ]);
    const modelRequests = new Set([
        "localModels.prepare",
        "localModels.setup",
        "localModels.deleteModel",
    ]);

    return {
        async encode(message: Message): Promise<string> {
            const method = Message.isRequest(message) ? message.method : undefined;
            const rawHistory =
                method === "session.getMessages" &&
                (message as { params?: { [rawHistoryResponse]?: boolean } }).params?.[
                    rawHistoryResponse
                ];
            const analytics = method === "taskAnalytics.classifyTrajectory";
            // Warm the decoder before sending: EOF may otherwise overtake its first import.
            const json =
                method &&
                (snapshots.has(method) || modelRequests.has(method) || rawHistory || analytics)
                    ? await import("lossless-json")
                    : undefined;
            const text =
                method && (modelRequests.has(method) || analytics) && json
                    ? json.stringify(
                          message,
                          analytics
                              ? (_key, value) => {
                                    const nativeJson = JSON as typeof JSON & {
                                        isRawJSON?: (value: unknown) => boolean;
                                    };
                                    return nativeJson.isRawJSON?.(value)
                                        ? json.parse(JSON.stringify(value))
                                        : value;
                                }
                              : undefined
                      )!
                    : JSON.stringify(message);
            if (
                Message.isRequest(message) &&
                json &&
                (snapshots.has(message.method) ||
                    message.method === "localModels.prepare" ||
                    rawHistory)
            ) {
                pending.set(message.id, { method: message.method, json });
            }
            return text;
        },
        async decode(text: string): Promise<Message> {
            const message = JSON.parse(text) as Message;
            if (!Message.isResponse(message)) return message;
            const request = pending.get(message.id);
            pending.delete(message.id);
            if (!request || !("result" in message)) return message;

            if (request.method === "session.getMessages") {
                return (await parseTrajectoryJson(text)) as unknown as ResponseMessage;
            }

            const {
                method,
                json: { parse, parseNumberAndBigInt },
            } = request;
            const precise = parse(text, undefined, (token) => {
                const value = parseNumberAndBigInt(token);
                return typeof value === "bigint" && Number.isSafeInteger(Number(value))
                    ? Number(value)
                    : value;
            }) as ResponseMessage;
            // Only opaque descriptor fields gain bigint values. Typed sizes, counters,
            // IDs, and all other RPC methods retain their existing number semantics.
            if (method === "localModels.prepare") {
                const result = message.result as LocalModelDescriptor;
                const source = precise.result as LocalModelDescriptor;
                result.metadata = source.metadata;
                result.serveOptions = source.serveOptions;
            } else {
                const result = message.result as LocalModelsSnapshot;
                const source = precise.result as LocalModelsSnapshot;
                for (const [index, row] of result.state.machine.rows.entries()) {
                    row.descriptor.metadata = source.state.machine.rows[index].descriptor.metadata;
                    row.descriptor.serveOptions =
                        source.state.machine.rows[index].descriptor.serveOptions;
                }
            }
            return message;
        },
    };
}
