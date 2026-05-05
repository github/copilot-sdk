/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs from "fs";
import http from "http";
import https from "https";
import tls from "tls";
import { describe, expect, test } from "vitest";
import {
  ConnectProxy,
  parseConnectTarget,
  type RequestHandler,
} from "./connectProxy";
import { createE2eRequestHandler } from "./mockHandlers";

describe("parseConnectTarget", () => {
  test("parses host:port", () => {
    expect(parseConnectTarget("example.com:443")).toEqual({
      host: "example.com",
      port: "443",
    });
  });

  test("defaults missing port to 443", () => {
    expect(parseConnectTarget("example.com")).toEqual({
      host: "example.com",
      port: "443",
    });
  });

  test("parses IPv6 bracket form", () => {
    expect(parseConnectTarget("[::1]:8443")).toEqual({
      host: "::1",
      port: "8443",
    });
  });

  test("rejects malformed IPv6 authority", () => {
    expect(parseConnectTarget("[::1:443")).toEqual({ host: "", port: "" });
    expect(parseConnectTarget("[::1]443")).toEqual({ host: "", port: "" });
  });
});

describe("ConnectProxy", () => {
  test("starts and stops cleanly", async () => {
    const proxy = new ConnectProxy(
      (_req, res) => {
        res.writeHead(200);
        res.end("ok");
        return true;
      },
      { interceptDomains: ["example.com"] },
    );
    await proxy.start();

    expect(proxy.proxyUrl).toMatch(/^http:\/\/127\.0\.0\.1:\d+$/);
    expect(proxy.caFilePath).toMatch(/test-ca-bundle\.pem$/);

    await proxy.stop();
  });

  test("intercepts HTTPS requests to configured domains", async () => {
    const requests: Array<{ host: string; url: string }> = [];
    const handler: RequestHandler = (req, res, targetHost) => {
      requests.push({ host: targetHost, url: req.url ?? "/" });
      res.writeHead(200, { "content-type": "text/plain" });
      res.end("mocked");
      return true;
    };

    const proxy = new ConnectProxy(handler, {
      interceptDomains: ["test.example.com"],
    });
    await proxy.start();

    try {
      const response = await makeHttpsRequest(
        proxy.proxyUrl,
        proxy.caFilePath,
        "test.example.com",
        "/api/test",
      );
      expect(response.statusCode).toBe(200);
      expect(response.body).toBe("mocked");
      expect(requests).toEqual([
        { host: "test.example.com", url: "/api/test" },
      ]);
      expect(proxy.connectLog[0].host).toBe("test.example.com");
    } finally {
      await proxy.stop();
    }
  });

  test("rejects CONNECT to non-intercepted domains", async () => {
    const blocked: string[] = [];
    const proxy = new ConnectProxy(
      (_req, res) => {
        res.writeHead(200);
        res.end("ok");
        return true;
      },
      {
        interceptDomains: ["allowed.example.com"],
        onBlockedConnection: (host) => blocked.push(host),
      },
    );
    await proxy.start();

    try {
      await expect(
        makeHttpsRequest(
          proxy.proxyUrl,
          proxy.caFilePath,
          "blocked.example.com",
          "/",
        ),
      ).rejects.toThrow();
      expect(blocked).toEqual(["blocked.example.com"]);
    } finally {
      await proxy.stop();
    }
  });

  test("mocks GitHub HTTPS requests without reaching the network", async () => {
    const proxy = new ConnectProxy(
      createE2eRequestHandler({ capiProxyUrl: "http://127.0.0.1:1" }),
      { interceptDomains: ["github.com", "api.github.com"] },
    );
    await proxy.start();

    try {
      const githubResponse = await makeHttpsRequest(
        proxy.proxyUrl,
        proxy.caFilePath,
        "github.com",
        "/github/copilot-sdk/issues/1234",
      );
      expect(githubResponse.statusCode).toBe(404);
      expect(githubResponse.body).toContain("Not Found (e2e mock)");

      const apiResponse = await makeHttpsRequest(
        proxy.proxyUrl,
        proxy.caFilePath,
        "api.github.com",
        "/user",
      );
      expect(apiResponse.statusCode).toBe(200);
      expect(JSON.parse(apiResponse.body)).toMatchObject({
        login: "sdk-e2e-user",
      });
    } finally {
      await proxy.stop();
    }
  });
});

function makeHttpsRequest(
  proxyUrl: string,
  caFilePath: string,
  hostname: string,
  path: string,
): Promise<{ statusCode: number; body: string }> {
  return new Promise((resolve, reject) => {
    const proxy = new URL(proxyUrl);
    const connectReq = http.request({
      host: proxy.hostname,
      port: Number(proxy.port),
      method: "CONNECT",
      path: `${hostname}:443`,
    });

    connectReq.on("connect", (_res, socket) => {
      const ca = fs.readFileSync(caFilePath);
      const req = https.request(
        {
          hostname,
          path,
          method: "GET",
          createConnection: () =>
            tls.connect({ socket, servername: hostname, ca }),
        },
        (res) => {
          let body = "";
          res.on("data", (chunk: Buffer) => {
            body += chunk.toString();
          });
          res.on("end", () =>
            resolve({ statusCode: res.statusCode ?? 0, body }),
          );
        },
      );
      req.on("error", reject);
      req.end();
    });

    connectReq.on("response", (res) => {
      res.resume();
      reject(new Error(`CONNECT failed with status ${res.statusCode}`));
    });

    connectReq.on("error", reject);
    connectReq.end();
  });
}
