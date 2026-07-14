/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { mkdtemp, rm, writeFile } from "node:fs/promises";
import os from "node:os";
import path from "node:path";
import type { ChatCompletion } from "openai/resources/chat/completions";
import { afterEach, beforeEach, describe, expect, test } from "vitest";
import yaml from "yaml";
import {
  aggregateAnthropicSseToMessage,
  anthropicMessageResponseToChatCompletion,
  anthropicMessagesRequestToChatCompletion,
  chatCompletionResponseToAnthropicMessage,
  chatCompletionResponseToAnthropicSseChunks,
} from "./anthropicMessagesAdapter";
import {
  aggregateResponsesApiSseToResponse,
  chatCompletionResponseToResponsesApiMessage,
  chatCompletionResponseToResponsesApiSseChunks,
  responsesApiRequestToChatCompletion,
  responsesApiResponseToChatCompletion,
} from "./responsesApiAdapter";
import {
  NormalizedData,
  ReplayBackend,
  ReplayingCapiProxy,
} from "./replayingCapiProxy";

const completionWithTool: ChatCompletion = {
  id: "completion-1",
  object: "chat.completion",
  created: 123,
  model: "test-model",
  choices: [
    {
      index: 0,
      message: {
        role: "assistant",
        content: "Calling a tool",
        refusal: null,
        tool_calls: [
          {
            id: "call-1",
            type: "function",
            function: {
              name: "lookup",
              arguments: '{"value":42}',
            },
          },
        ],
      },
      logprobs: null,
      finish_reason: "tool_calls",
    },
  ],
  usage: {
    prompt_tokens: 10,
    completion_tokens: 5,
    total_tokens: 15,
  },
};

describe("Anthropic Messages adapter", () => {
  test("normalizes system, images, tools, and tool results", () => {
    const result = JSON.parse(
      anthropicMessagesRequestToChatCompletion(
        JSON.stringify({
          model: "test-model",
          system: [{ type: "text", text: "Be helpful" }],
          messages: [
            {
              role: "user",
              content: [
                { type: "text", text: "Inspect this" },
                {
                  type: "image",
                  source: {
                    type: "base64",
                    media_type: "image/png",
                    data: "AQID",
                  },
                },
              ],
            },
            {
              role: "assistant",
              content: [
                {
                  type: "tool_use",
                  id: "call-1",
                  name: "lookup",
                  input: { value: 42 },
                },
              ],
            },
            {
              role: "user",
              content: [
                {
                  type: "tool_result",
                  tool_use_id: "call-1",
                  content: "found",
                },
              ],
            },
          ],
          tools: [
            {
              name: "lookup",
              description: "Find a value",
              input_schema: { type: "object" },
            },
          ],
          stream: true,
        }),
      ),
    ) as {
      messages: Array<Record<string, unknown>>;
      tools: Array<Record<string, unknown>>;
      stream: boolean;
    };

    expect(result.messages).toEqual([
      { role: "system", content: "Be helpful" },
      {
        role: "user",
        content: [
          { type: "text", text: "Inspect this" },
          {
            type: "image_url",
            image_url: { url: "data:image/png;base64,AQID" },
          },
        ],
      },
      {
        role: "assistant",
        content: null,
        tool_calls: [
          {
            id: "call-1",
            type: "function",
            function: {
              name: "lookup",
              arguments: '{"value":42}',
            },
          },
        ],
      },
      { role: "tool", tool_call_id: "call-1", content: "found" },
    ]);
    expect(result.tools).toHaveLength(1);
    expect(result.stream).toBe(true);
  });

  test("round-trips JSON and streaming responses", () => {
    const message =
      chatCompletionResponseToAnthropicMessage(completionWithTool);
    expect(message.stop_reason).toBe("tool_use");
    expect(message.content.map((block) => block.type)).toEqual([
      "text",
      "tool_use",
    ]);

    const request = JSON.stringify({
      model: "test-model",
      messages: [{ role: "user", content: "Hello" }],
    });
    const fromJson = anthropicMessageResponseToChatCompletion(
      request,
      JSON.stringify(message),
    );
    expect(fromJson.choices[0].message.tool_calls).toHaveLength(1);

    const stream =
      chatCompletionResponseToAnthropicSseChunks(completionWithTool).join("");
    expect(stream).toContain("event: message_start");
    expect(stream).toContain("event: message_stop");
    const aggregated = aggregateAnthropicSseToMessage(stream);
    expect(aggregated).not.toBeNull();
    expect(aggregated!.content).toEqual(message.content);
  });

  test("combines CAPI multi-choice assistant messages", () => {
    const message = chatCompletionResponseToAnthropicMessage({
      ...completionWithTool,
      choices: [
        completionWithTool.choices[0],
        {
          index: 1,
          message: {
            role: "assistant",
            content: null,
            refusal: null,
            tool_calls: [
              {
                id: "call-2",
                type: "function",
                function: { name: "inspect", arguments: '{"path":"file.txt"}' },
              },
            ],
          },
          logprobs: null,
          finish_reason: "tool_calls",
        },
      ],
    });

    expect(
      message.content
        .filter((block) => block.type === "tool_use")
        .map((block) => block.name),
    ).toEqual(["lookup", "inspect"]);
  });
});

describe("OpenAI Responses adapter", () => {
  test("normalizes instructions, binary content, and function history", () => {
    const result = JSON.parse(
      responsesApiRequestToChatCompletion(
        JSON.stringify({
          model: "test-model",
          instructions: "Be helpful",
          input: [
            {
              type: "message",
              role: "user",
              content: [
                { type: "input_text", text: "Inspect this" },
                {
                  type: "input_image",
                  image_url: "data:image/png;base64,AQID",
                },
              ],
            },
            {
              type: "function_call",
              call_id: "call-1",
              name: "lookup",
              arguments: '{"value":42}',
            },
            {
              type: "function_call_output",
              call_id: "call-1",
              output: "found",
            },
          ],
          tools: [
            {
              type: "function",
              name: "lookup",
              parameters: { type: "object" },
            },
          ],
        }),
      ),
    ) as {
      messages: Array<Record<string, unknown>>;
      tools: Array<Record<string, unknown>>;
    };

    expect(result.messages).toEqual([
      { role: "system", content: "Be helpful" },
      {
        role: "user",
        content: [
          { type: "text", text: "Inspect this" },
          {
            type: "image_url",
            image_url: {
              url: "data:image/png;base64,AQID",
            },
          },
        ],
      },
      {
        role: "assistant",
        content: null,
        tool_calls: [
          {
            id: "call-1",
            type: "function",
            function: {
              name: "lookup",
              arguments: '{"value":42}',
            },
          },
        ],
      },
      { role: "tool", tool_call_id: "call-1", content: "found" },
    ]);
    expect(result.tools).toHaveLength(1);
  });

  test("round-trips JSON and streaming responses", () => {
    const response =
      chatCompletionResponseToResponsesApiMessage(completionWithTool);
    expect(response.output.map((item) => item.type)).toEqual([
      "message",
      "function_call",
    ]);

    const request = JSON.stringify({ model: "test-model", input: "Hello" });
    const fromJson = responsesApiResponseToChatCompletion(
      request,
      JSON.stringify(response),
    );
    expect(fromJson.choices[0].message.tool_calls).toHaveLength(1);

    const stream =
      chatCompletionResponseToResponsesApiSseChunks(completionWithTool).join(
        "",
      );
    expect(stream).toContain("event: response.created");
    expect(stream).toContain("event: response.completed");
    const aggregated = aggregateResponsesApiSseToResponse(stream);
    expect(aggregated?.output_text).toBe(response.output_text);
    expect(aggregated?.output.map((item) => item.type)).toEqual([
      "message",
      "function_call",
    ]);
  });
});

describe("protocol-aware replay", () => {
  let tempDir: string;
  let workDir: string;
  let cachePath: string;

  beforeEach(async () => {
    tempDir = await mkdtemp(path.join(os.tmpdir(), "protocol-replay-"));
    workDir = path.join(tempDir, "work");
    cachePath = path.join(tempDir, "cache.yaml");
    await writeFile(
      cachePath,
      yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Hello" },
              { role: "assistant", content: "Hi there!" },
            ],
          },
        ],
      } satisfies NormalizedData),
    );
  });

  afterEach(async () => {
    await rm(tempDir, { recursive: true, force: true });
  });

  test.each([
    {
      backend: "anthropic-messages" as ReplayBackend,
      endpoint: "/v1/messages",
      request: {
        model: "test-model",
        system: "Be helpful",
        messages: [{ role: "user", content: "Hello" }],
        max_tokens: 128,
      },
      assertBody: (body: Record<string, unknown>) => {
        expect(body.type).toBe("message");
        expect(body.content).toEqual([
          { type: "text", text: "Hi there!", citations: null },
        ]);
      },
    },
    {
      backend: "openai-responses" as ReplayBackend,
      endpoint: "/responses",
      request: {
        model: "test-model",
        instructions: "Be helpful",
        input: [
          {
            type: "message",
            role: "user",
            content: [{ type: "input_text", text: "Hello" }],
          },
        ],
      },
      assertBody: (body: Record<string, unknown>) => {
        expect(body.object).toBe("response");
        expect(body.output_text).toBe("Hi there!");
      },
    },
    {
      backend: "openai-completions" as ReplayBackend,
      endpoint: "/chat/completions",
      request: {
        model: "test-model",
        messages: [
          { role: "system", content: "Be helpful" },
          { role: "user", content: "Hello" },
        ],
      },
      assertBody: (body: Record<string, unknown>) => {
        expect(body.object).toBe("chat.completion");
      },
    },
  ])("replays $backend and exposes canonical exchanges", async (testCase) => {
    const proxy = new ReplayingCapiProxy(
      "http://localhost:9999",
      cachePath,
      workDir,
    );
    const proxyUrl = await proxy.start();
    await proxy.updateConfig({
      filePath: cachePath,
      workDir,
      backend: testCase.backend,
    });

    try {
      const response = await fetch(`${proxyUrl}${testCase.endpoint}`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(testCase.request),
      });
      expect(response.status).toBe(200);
      testCase.assertBody((await response.json()) as Record<string, unknown>);

      const exchangesResponse = await fetch(`${proxyUrl}/exchanges`);
      const exchanges = (await exchangesResponse.json()) as Array<{
        request: { messages: Array<{ role: string; content: unknown }> };
      }>;
      expect(exchanges).toHaveLength(1);
      expect(exchanges[0].request.messages.at(-1)).toEqual({
        role: "user",
        content: "Hello",
      });
    } finally {
      await proxy.stop(true);
    }
  });

  test.each([
    {
      backend: "openai-responses" as ReplayBackend,
      endpoint: "/responses",
      request: {
        model: "test-model",
        instructions: "Be helpful",
        input: [
          {
            type: "message",
            role: "user",
            content: [
              {
                type: "input_text",
                text: "Hook context\n\n\n<current_datetime>2026-01-01T00:00:00Z</current_datetime>\n\n",
              },
            ],
          },
          {
            type: "message",
            role: "user",
            content: [{ type: "input_text", text: "Hello" }],
          },
        ],
      },
    },
    {
      backend: "openai-completions" as ReplayBackend,
      endpoint: "/chat/completions",
      request: {
        model: "test-model",
        messages: [
          { role: "system", content: "Be helpful" },
          {
            role: "user",
            content:
              "Hook context\n\n\n<current_datetime>2026-01-01T00:00:00Z</current_datetime>\n\n",
          },
          { role: "user", content: "Hello" },
        ],
      },
    },
  ])("coalesces adjacent user messages for $backend", async (testCase) => {
    await writeFile(
      cachePath,
      yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "Hook context" },
              { role: "user", content: "Hello" },
              { role: "assistant", content: "Hi there!" },
            ],
          },
        ],
      } satisfies NormalizedData),
    );
    const proxy = new ReplayingCapiProxy(
      "http://localhost:9999",
      cachePath,
      workDir,
    );
    const proxyUrl = await proxy.start();
    await proxy.updateConfig({
      filePath: cachePath,
      workDir,
      backend: testCase.backend,
    });

    try {
      const response = await fetch(`${proxyUrl}${testCase.endpoint}`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(testCase.request),
      });
      expect(response.status).toBe(200);
    } finally {
      await proxy.stop(true);
    }
  });

  test("normalizes Anthropic spacing for adjacent user turns", async () => {
    await writeFile(
      cachePath,
      yaml.stringify({
        models: ["test-model"],
        conversations: [
          {
            messages: [
              { role: "system", content: "${system}" },
              { role: "user", content: "First prompt" },
              { role: "user", content: "Recovery prompt" },
              { role: "assistant", content: "Recovered" },
            ],
          },
        ],
      } satisfies NormalizedData),
    );
    const proxy = new ReplayingCapiProxy(
      "http://localhost:9999",
      cachePath,
      workDir,
    );
    const proxyUrl = await proxy.start();
    await proxy.updateConfig({
      filePath: cachePath,
      workDir,
      backend: "anthropic-messages",
    });

    try {
      const response = await fetch(`${proxyUrl}/v1/messages`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          model: "test-model",
          system: "Be helpful",
          messages: [
            {
              role: "user",
              content: "First prompt\n\n\n\n\nRecovery prompt",
            },
          ],
          max_tokens: 128,
        }),
      });
      expect(response.status).toBe(200);
    } finally {
      await proxy.stop(true);
    }
  });

  test.each([
    {
      backend: "anthropic-messages" as ReplayBackend,
      endpoint: "/v1/messages",
      request: {
        model: "test-model",
        system: "Be helpful",
        messages: [{ role: "user", content: "${compaction_prompt}" }],
        max_tokens: 128,
      },
    },
    {
      backend: "openai-responses" as ReplayBackend,
      endpoint: "/responses",
      request: {
        model: "test-model",
        instructions: "Be helpful",
        input: [
          {
            type: "message",
            role: "user",
            content: [{ type: "input_text", text: "${compaction_prompt}" }],
          },
        ],
      },
    },
    {
      backend: "openai-completions" as ReplayBackend,
      endpoint: "/chat/completions",
      request: {
        model: "test-model",
        messages: [
          { role: "system", content: "Be helpful" },
          { role: "user", content: "${compaction_prompt}" },
        ],
      },
    },
  ])("synthesizes a compaction response for $backend", async (testCase) => {
    const proxy = new ReplayingCapiProxy(
      "http://localhost:9999",
      cachePath,
      workDir,
    );
    const proxyUrl = await proxy.start();
    await proxy.updateConfig({
      filePath: cachePath,
      workDir,
      backend: testCase.backend,
    });

    try {
      const response = await fetch(`${proxyUrl}${testCase.endpoint}`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify(testCase.request),
      });
      expect(response.status).toBe(200);
      const body = JSON.stringify(await response.json());
      expect(body).toContain("<overview>");
      expect(body).toContain("<history>");
      expect(body).toContain("<checkpoint_title>");
    } finally {
      await proxy.stop(true);
    }
  });

  test("rejects an inference request sent over the wrong protocol", async () => {
    const proxy = new ReplayingCapiProxy(
      "http://localhost:9999",
      cachePath,
      workDir,
    );
    const proxyUrl = await proxy.start();
    await proxy.updateConfig({
      filePath: cachePath,
      workDir,
      backend: "anthropic-messages",
    });

    try {
      const response = await fetch(`${proxyUrl}/chat/completions`, {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({
          model: "test-model",
          messages: [{ role: "user", content: "Hello" }],
        }),
      });
      expect(response.status).toBe(400);
      await expect(response.text()).resolves.toContain("protocol_mismatch");
    } finally {
      await proxy.stop(true);
    }
  });
});
