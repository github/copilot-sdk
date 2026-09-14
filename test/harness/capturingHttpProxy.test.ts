/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "http";
import { afterEach, beforeEach, describe, expect, test } from "vitest";
import { CapturedExchange, CapturingHttpProxy } from "./capturingHttpProxy";

describe("Capturing HTTP Proxy", () => {
  let proxy: CapturingHttpProxy;
  let testServer: http.Server;
  let testServerAddress: string;
  let onHangingRequest: (() => void) | undefined;
  let onStreamingResponse: (() => void) | undefined;

  beforeEach(async () => {
    testServer = http.createServer((req, res) => {
      if (req.url === "/hang") {
        onHangingRequest?.();
        return;
      }
      if (req.url === "/stream") {
        res.writeHead(200, { "content-type": "text/plain" });
        res.write("started");
        onStreamingResponse?.();
        return;
      }
      res.writeHead(200, { "content-type": "application/json" });
      res.end(JSON.stringify({ message: "Hello", path: req.url }));
    });

    await new Promise<void>((resolve, reject) => {
      testServer.listen(0, "127.0.0.1", () => {
        const addr = testServer.address();
        if (addr instanceof Object) {
          testServerAddress = `http://${addr.address}:${addr.port}`;
          resolve();
        } else {
          reject(new Error("Failed to get test server address"));
        }
      });
    });
  });

  afterEach(async () => {
    if (proxy) {
      await proxy.stop();
    }

    if (testServer) {
      await new Promise<void>((resolve, reject) =>
        testServer.close((err) => {
          if (err) {
            reject(err);
          } else {
            resolve();
          }
        }),
      );
    }
  });

  test("captures HTTP requests and responses", async () => {
    proxy = new CapturingHttpProxy(testServerAddress);
    const proxyUrl = await proxy.start();

    const response = await fetch(`${proxyUrl}/api/test`);
    expect(response.status).toBe(200);

    // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
    const data = await response.json();
    expect(data).toEqual({ message: "Hello", path: "/api/test" });
    expect(proxy.exchanges).toMatchObject([
      {
        request: {
          url: "/api/test",
          method: "GET",
        },
        response: {
          statusCode: 200,
          body: JSON.stringify({ message: "Hello", path: "/api/test" }),
        },
      } as CapturedExchange,
    ]);
  });

  test("stops while a proxied request is still active", async () => {
    proxy = new CapturingHttpProxy(testServerAddress);
    const proxyUrl = await proxy.start();
    const requestStarted = new Promise<void>((resolve) => {
      onHangingRequest = resolve;
    });
    const request = fetch(`${proxyUrl}/hang`).catch(() => undefined);
    await requestStarted;

    await proxy.stop();

    await request;
  });

  test("stops while a proxied response is still streaming", async () => {
    proxy = new CapturingHttpProxy(testServerAddress);
    const proxyUrl = await proxy.start();
    const responseStarted = new Promise<void>((resolve) => {
      onStreamingResponse = resolve;
    });
    const responsePromise = fetch(`${proxyUrl}/stream`);
    await responseStarted;
    const response = await responsePromise;
    const body = response.text().catch(() => undefined);

    await proxy.stop();

    await body;
  });
});
