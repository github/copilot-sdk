/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFile } from "node:fs/promises";
import path from "node:path";
import type {
  ChatCompletion,
  ChatCompletionChunk,
  ChatCompletionMessage,
  ChatCompletionMessageFunctionToolCall,
} from "openai/resources/chat/completions";
import { afterEach, beforeEach, expect, test, vi } from "vitest";
import yaml from "yaml";
import { type NormalizedData, ReplayingCapiProxy } from "./replayingCapiProxy";

type NormalizedMessage =
  NormalizedData["conversations"][number]["messages"][number];

const snapshotPath = path.join(
  import.meta.dirname,
  "..",
  "snapshots",
  "subagent_hooks",
  "should_invoke_pretooluse_and_posttooluse_hooks_for_sub_agent_tool_calls.yaml",
);
const stored = yaml.parse(
  await readFile(snapshotPath, "utf8"),
) as NormalizedData;
const original = stored.conversations[3].messages;
const [waiting, notification, readAgent, toolResult, finalAnswer] =
  original.slice(5);
const earlyReply = { ...waiting, tool_calls: readAgent.tool_calls };
const rawNotification: NormalizedMessage = {
  role: "user",
  content:
    '<system_notification>\nAgent "read-file" (explore) has finished processing and is now idle. ' +
    'Use read_agent with agent_id "fa1ad5a2-aef9-4cd1-996d-85295154e583" to read the results, ' +
    "or write_agent to send follow-up messages.\n</system_notification>",
};
const shortIdleNotification: NormalizedMessage = {
  role: "user",
  content:
    '<system_notification>\nAgent "read-file" (explore) has finished processing and is now idle.\n</system_notification>',
};
const shortReadAgent: NormalizedMessage = {
  role: "assistant",
  tool_calls: [
    {
      id: "toolcall_2",
      type: "function",
      function: {
        name: "read_agent",
        arguments: '{"agent_id":"read-file","since_turn":0}',
      },
    },
  ],
};
const notifications = [
  { wording: "verbose", notification: rawNotification, readAgent },
  {
    wording: "short idle",
    notification: shortIdleNotification,
    readAgent: shortReadAgent,
  },
];

beforeEach(() => {
  vi.stubEnv("GITHUB_ACTIONS", "true");
});

afterEach(() => {
  vi.unstubAllEnvs();
});

async function readReply(response: Response, streaming: boolean) {
  if (!streaming) {
    return ((await response.json()) as ChatCompletion).choices[0].message;
  }
  let content = "";
  const toolCalls: ChatCompletionMessageFunctionToolCall[] = [];
  for (const line of (await response.text()).split("\n")) {
    if (!line.startsWith("data: ") || line === "data: [DONE]") continue;
    const chunk = JSON.parse(line.slice(6)) as ChatCompletionChunk;
    for (const choice of chunk.choices) {
      content += choice.delta.content ?? "";
      for (const call of choice.delta.tool_calls ?? []) {
        const tool = (toolCalls[call.index] ??= {
          id: "",
          type: "function",
          function: { name: "", arguments: "" },
        });
        tool.id += call.id ?? "";
        tool.function.name += call.function?.name ?? "";
        tool.function.arguments += call.function?.arguments ?? "";
      }
    }
  }
  return {
    content: content || null,
    tool_calls: toolCalls.length ? toolCalls : undefined,
  };
}

function expectReply(
  actual: Pick<ChatCompletionMessage, "content" | "tool_calls">,
  expected: NormalizedMessage,
) {
  expect(actual.content).toBe(expected.content ?? null);
  expect(actual.tool_calls).toEqual(expected.tool_calls);
}

for (const timing of ["before", "after"] as const) {
  for (const streaming of [false, true]) {
    test.each(notifications)(
      `replays $wording notification ${timing} the parent reply, streaming=${streaming}`,
      async ({ notification, readAgent }) => {
        const earlyReply = { ...waiting, tool_calls: readAgent.tool_calls };
        const proxy = new ReplayingCapiProxy(
          "http://127.0.0.1:1",
          snapshotPath,
          import.meta.dirname,
        );
        const url = await proxy.start();
        const messages = original.slice(0, 5);
        const request = () =>
          fetch(`${url}/chat/completions`, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({
              model: stored.models[0],
              messages,
              stream: streaming,
            }),
          });
        try {
          if (timing === "before") messages.push(notification);
          const first = await request();
          expect(first.status, await first.clone().text()).toBe(200);
          expectReply(
            await readReply(first, streaming),
            timing === "before" ? earlyReply : waiting,
          );
          messages.push(timing === "before" ? earlyReply : waiting);

          if (timing === "after") {
            messages.push(notification);
            const second = await request();
            expect(second.status, await second.clone().text()).toBe(200);
            expectReply(await readReply(second, streaming), readAgent);
            messages.push(readAgent);
          }

          messages.push(toolResult);
          const final = await request();
          expect(final.status, await final.clone().text()).toBe(200);
          expectReply(await readReply(final, streaming), finalAnswer);
        } finally {
          await proxy.stop(true);
        }
      },
    );
  }

  test.each(notifications)(
    `rejects invalid $wording notifications and tool histories with ${timing} completion`,
    async ({ notification, readAgent }) => {
      const earlyReply = { ...waiting, tool_calls: readAgent.tool_calls };
      const proxy = new ReplayingCapiProxy(
        "http://127.0.0.1:1",
        snapshotPath,
        import.meta.dirname,
      );
      const url = await proxy.start();
      const prefix = [
        ...original.slice(0, 5),
        ...(timing === "after" ? [waiting] : []),
      ];
      const continuation = [
        ...prefix,
        notification,
        timing === "before" ? earlyReply : readAgent,
      ];
      const malformed = [
        [...prefix, notification, notification],
        [
          ...prefix,
          {
            ...notification,
            content: notification.content!.replace("read-file", "other-agent"),
          },
        ],
        [
          ...prefix.filter((message) => message.tool_call_id !== "toolcall_1"),
          notification,
        ],
        continuation,
        [
          ...continuation,
          {
            ...toolResult,
            content: toolResult.content!.replace(
              "Hello from subagent test!",
              "Wrong file contents!",
            ),
          },
        ],
        [...continuation, toolResult, notification],
        ...["has failed", "was cancelled"].map((state) => [
          ...prefix,
          {
            ...shortIdleNotification,
            content: shortIdleNotification.content!.replace(
              "has finished processing and is now idle",
              state,
            ),
          },
        ]),
        ...["failed", "cancelled"].map((status) => [
          ...continuation,
          {
            ...toolResult,
            content: toolResult.content!.replace(
              "status: completed",
              `status: ${status}`,
            ),
          },
        ]),
        [
          ...prefix,
          notification,
          {
            ...(timing === "before" ? earlyReply : readAgent),
            tool_calls: shortReadAgent.tool_calls!.map((call) => ({
              ...call,
              function: {
                ...call.function,
                arguments: '{"agent_id":"read-file","since_turn":1}',
              },
            })),
          },
          toolResult,
        ],
      ];
      const stderr = vi.spyOn(process.stderr, "write").mockReturnValue(true);
      const consoleError = vi
        .spyOn(console, "error")
        .mockImplementation(() => {});
      try {
        for (const messages of malformed) {
          const response = await fetch(`${url}/chat/completions`, {
            method: "POST",
            headers: { "content-type": "application/json" },
            body: JSON.stringify({ model: stored.models[0], messages }),
          });
          expect(response.status, await response.text()).toBe(500);
        }
      } finally {
        stderr.mockRestore();
        consoleError.mockRestore();
        await proxy.stop(true);
      }
    },
  );
}

test("timing and wording alternatives retain the full result and final continuation", () => {
  expect(stored.conversations).toHaveLength(7);
  expect(stored.conversations[4].messages).toEqual([
    ...original.slice(0, 5),
    notification,
    earlyReply,
    toolResult,
    finalAnswer,
  ]);
  for (const [originalIndex, alternativeIndex] of [
    [3, 5],
    [4, 6],
  ]) {
    expect(stored.conversations[alternativeIndex].messages).toEqual(
      stored.conversations[originalIndex].messages.map((message) => {
        if (
          message.role === "user" &&
          message.content === notification.content
        ) {
          return shortIdleNotification;
        }
        if (
          message.tool_calls?.some(
            (call) => call.function?.name === "read_agent",
          )
        ) {
          return { ...message, tool_calls: shortReadAgent.tool_calls };
        }
        return message;
      }),
    );
  }
});
