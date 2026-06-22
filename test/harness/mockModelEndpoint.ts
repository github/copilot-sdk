/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "http";
import type { AddressInfo } from "net";

/** Control path: GET returns the recorded model requests as JSON. */
const RECORDED_PATH = "/__model/recorded";
/** Control path: POST clears the recorded model requests. */
const RESET_PATH = "/__model/reset";
/** Control path: POST closes the endpoint so its process can exit cleanly. */
const STOP_PATH = "/__model/stop";

/** A single inference request the runtime made to the mock model endpoint. */
export interface RecordedModelRequest {
  /** Value of the `Authorization` header (undefined if absent). */
  authorization: string | undefined;
  /** Request path the runtime called (e.g. `/chat/completions`). */
  path: string;
  /** Request method. */
  method: string;
}

/** Connection details a test needs to exercise the mock model endpoint. */
export interface MockModelEndpointInfo {
  /** Base URL to assign as the BYOK provider's `baseUrl`. */
  baseUrl: string;
  /** GET URL returning the recorded model requests as JSON. */
  recordedUrl: string;
  /** POST URL clearing the recorded model requests. */
  resetUrl: string;
  /** POST URL that closes the endpoint so its host process can exit. */
  stopUrl: string;
}

/**
 * A mock of a BYOK provider's OpenAI-compatible inference endpoint. It records
 * the `Authorization` header (and path/method) of every inference request the
 * runtime sent — so e2e tests can assert the credential the runtime injected —
 * and replies with a minimal OpenAI chat completion (streamed or not) so the
 * turn finishes cleanly.
 *
 * It lives in the shared harness and is exposed over HTTP (inference + control
 * endpoints) so SDK e2e tests in any language can point a BYOK provider at it
 * without re-implementing the endpoint.
 */
export class MockModelEndpoint {
  private server: http.Server | undefined;
  private readonly requests: RecordedModelRequest[] = [];
  private baseUrl = "";

  /** Starts the endpoint on a random loopback port. */
  async start(): Promise<MockModelEndpointInfo> {
    const server = http.createServer((req, res) => {
      void this.handle(req, res);
    });
    this.server = server;
    await new Promise<void>((resolve, reject) => {
      server.once("error", reject);
      server.listen(0, "127.0.0.1", () => resolve());
    });
    const { port } = server.address() as AddressInfo;
    this.baseUrl = `http://127.0.0.1:${port}`;
    return this.info();
  }

  /** Connection details for the running endpoint. */
  info(): MockModelEndpointInfo {
    return {
      baseUrl: this.baseUrl,
      recordedUrl: `${this.baseUrl}${RECORDED_PATH}`,
      resetUrl: `${this.baseUrl}${RESET_PATH}`,
      stopUrl: `${this.baseUrl}${STOP_PATH}`,
    };
  }

  /** Inference requests recorded so far, in arrival order. */
  recordedRequests(): readonly RecordedModelRequest[] {
    return this.requests;
  }

  /** Clears recorded requests. */
  reset(): void {
    this.requests.length = 0;
  }

  async stop(): Promise<void> {
    const server = this.server;
    if (!server) {
      return;
    }
    this.server = undefined;
    await new Promise<void>((resolve) => server.close(() => resolve()));
  }

  private async handle(
    req: http.IncomingMessage,
    res: http.ServerResponse,
  ): Promise<void> {
    const url = new URL(req.url ?? "/", this.baseUrl);

    if (req.method === "POST" && url.pathname === RESET_PATH) {
      this.reset();
      respondJson(res, 200, { ok: true });
      return;
    }

    if (req.method === "POST" && url.pathname === STOP_PATH) {
      respondJson(res, 200, { ok: true });
      res.on("finish", () => void this.stop());
      return;
    }

    if (req.method === "GET" && url.pathname === RECORDED_PATH) {
      respondJson(res, 200, this.requests);
      return;
    }

    // Any other path is treated as an inference request.
    const body = await readBody(req);
    this.requests.push({
      authorization:
        (req.headers["authorization"] as string | undefined) ?? undefined,
      path: req.url ?? "",
      method: req.method ?? "GET",
    });

    let wantsStream = false;
    try {
      wantsStream = (JSON.parse(body) as { stream?: boolean }).stream === true;
    } catch {
      // Non-JSON body: fall back to a non-streaming reply.
    }

    if (wantsStream) {
      respondStream(res);
    } else {
      respondCompletion(res);
    }
  }
}

/** Reads the full request body as a string. */
function readBody(req: http.IncomingMessage): Promise<string> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    req.on("data", (chunk: Buffer) => chunks.push(chunk));
    req.on("end", () => resolve(Buffer.concat(chunks).toString("utf8")));
    req.on("error", reject);
  });
}

/** Writes a minimal streamed OpenAI chat completion ending in `[DONE]`. */
function respondStream(res: http.ServerResponse): void {
  res.writeHead(200, { "content-type": "text/event-stream" });
  const base = {
    id: "mock-completion",
    object: "chat.completion.chunk",
    created: Math.floor(Date.now() / 1000),
    model: "mock-model",
  };
  res.write(
    `data: ${JSON.stringify({
      ...base,
      choices: [
        {
          index: 0,
          delta: { role: "assistant", content: "OK" },
          finish_reason: null,
          logprobs: null,
        },
      ],
    })}\n\n`,
  );
  res.write(
    `data: ${JSON.stringify({
      ...base,
      choices: [{ index: 0, delta: {}, finish_reason: "stop", logprobs: null }],
    })}\n\n`,
  );
  res.write("data: [DONE]\n\n");
  res.end();
}

/** Writes a minimal non-streaming OpenAI chat completion. */
function respondCompletion(res: http.ServerResponse): void {
  respondJson(res, 200, {
    id: "mock-completion",
    object: "chat.completion",
    created: Math.floor(Date.now() / 1000),
    model: "mock-model",
    choices: [
      {
        index: 0,
        message: { role: "assistant", content: "OK" },
        finish_reason: "stop",
        logprobs: null,
      },
    ],
    usage: { prompt_tokens: 0, completion_tokens: 0, total_tokens: 0 },
  });
}

function respondJson(
  res: http.ServerResponse,
  statusCode: number,
  body: unknown,
): void {
  const data = JSON.stringify(body);
  res.writeHead(statusCode, {
    "content-type": "application/json",
    "content-length": Buffer.byteLength(data),
  });
  res.end(data);
}
