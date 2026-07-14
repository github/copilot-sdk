/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Parses JSON Server-Sent Events. Malformed events and `[DONE]` sentinels are
 * ignored so recorded provider streams can be normalized best-effort.
 */
export function parseSseEvents(
  body: string,
): Array<{ type: string } & Record<string, unknown>> {
  const events: Array<{ type: string } & Record<string, unknown>> = [];
  for (const block of body.split(/\r?\n\r?\n/)) {
    if (!block.trim()) continue;

    const dataLines = block
      .split(/\r?\n/)
      .filter((line) => line.startsWith("data:"))
      .map((line) => line.slice(5).replace(/^ /, ""));
    const payload = dataLines.join("\n");
    if (!payload || payload === "[DONE]") continue;

    try {
      const parsed = JSON.parse(payload) as Record<string, unknown>;
      if (typeof parsed.type === "string") {
        events.push(parsed as { type: string } & Record<string, unknown>);
      }
    } catch {
      // Ignore malformed events in a partially captured stream.
    }
  }
  return events;
}
