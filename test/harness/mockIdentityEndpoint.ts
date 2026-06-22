/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "http";
import type { AddressInfo } from "net";

/** Fixed fake AAD token the mock identity endpoint mints for every request. */
export const MOCK_IDENTITY_TOKEN = "fake-managed-identity-token";

/** Secret the runtime must echo back in the `x-identity-header` header. */
export const MOCK_IDENTITY_HEADER_SECRET = "fake-identity-header-secret";

/** Path the token is served from (the value baked into `IDENTITY_ENDPOINT`). */
const TOKEN_PATH = "/msi/token";
/** Control path: GET returns the recorded token requests as JSON. */
const RECORDED_PATH = "/__identity/recorded";
/** Control path: POST clears the recorded token requests. */
const RESET_PATH = "/__identity/reset";
/** Control path: POST closes the endpoint so its process can exit cleanly. */
const STOP_PATH = "/__identity/stop";

/** App Service / Functions managed identity selector query params. */
const IDENTITY_SELECTOR_PARAMS = ["client_id", "principal_id", "mi_res_id"] as const;

/** A single token request the runtime made to the mock identity endpoint. */
export interface RecordedIdentityRequest {
  /** `resource` query parameter the runtime asked a token for. */
  resource: string | null;
  /** `api-version` query parameter. */
  apiVersion: string | null;
  /** Value of the `x-identity-header` secret header (undefined if absent). */
  identityHeader: string | undefined;
  /**
   * Identity selector params present on the request. System-assigned identities
   * send none; user-assigned identities send exactly one of `client_id`,
   * `principal_id`, or `mi_res_id`.
   */
  identityParams: Record<string, string>;
}

/** Connection details a test needs to exercise the mock identity endpoint. */
export interface MockIdentityEndpointInfo {
  /** URL to assign to the `IDENTITY_ENDPOINT` env var. */
  endpoint: string;
  /** Secret to assign to the `IDENTITY_HEADER` env var. */
  header: string;
  /** Fake bearer token the runtime injects (`Authorization: Bearer <token>`). */
  token: string;
  /** GET URL returning the recorded token requests as JSON. */
  recordedUrl: string;
  /** POST URL clearing the recorded token requests. */
  resetUrl: string;
  /** POST URL that closes the endpoint so its host process can exit. */
  stopUrl: string;
}

/**
 * A mock of the Azure App Service / Functions managed identity token endpoint
 * (the `IDENTITY_ENDPOINT` + `IDENTITY_HEADER` contract). It mints a fixed fake
 * AAD token and records the resource + identity selector each token request
 * asked for, so e2e tests can assert the runtime resolved the managed identity
 * correctly.
 *
 * It lives in the shared harness and is exposed over HTTP (token + control
 * endpoints) so SDK e2e tests in any language can drive BYOK managed identity
 * without re-implementing the endpoint.
 */
export class MockIdentityEndpoint {
  private server: http.Server | undefined;
  private readonly requests: RecordedIdentityRequest[] = [];
  private baseUrl = "";

  /** Starts the endpoint on a random loopback port. */
  async start(): Promise<MockIdentityEndpointInfo> {
    const server = http.createServer((req, res) => this.handle(req, res));
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
  info(): MockIdentityEndpointInfo {
    return {
      endpoint: `${this.baseUrl}${TOKEN_PATH}`,
      header: MOCK_IDENTITY_HEADER_SECRET,
      token: MOCK_IDENTITY_TOKEN,
      recordedUrl: `${this.baseUrl}${RECORDED_PATH}`,
      resetUrl: `${this.baseUrl}${RESET_PATH}`,
      stopUrl: `${this.baseUrl}${STOP_PATH}`,
    };
  }

  /** Token requests recorded so far, in arrival order. */
  recordedRequests(): readonly RecordedIdentityRequest[] {
    return this.requests;
  }

  /** Clears the recorded token requests. */
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

  private handle(req: http.IncomingMessage, res: http.ServerResponse): void {
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

    if (url.pathname !== TOKEN_PATH) {
      respondJson(res, 404, { error: "Not Found (mock identity endpoint)" });
      return;
    }

    const identityParams: Record<string, string> = {};
    for (const key of IDENTITY_SELECTOR_PARAMS) {
      const value = url.searchParams.get(key);
      if (value !== null) {
        identityParams[key] = value;
      }
    }
    const resource = url.searchParams.get("resource");
    this.requests.push({
      resource,
      apiVersion: url.searchParams.get("api-version"),
      identityHeader: (req.headers["x-identity-header"] as string | undefined) ?? undefined,
      identityParams,
    });

    respondJson(res, 200, {
      access_token: MOCK_IDENTITY_TOKEN,
      expires_in: 3600,
      token_type: "Bearer",
      resource,
    });
  }
}

function respondJson(res: http.ServerResponse, statusCode: number, body: unknown): void {
  const data = JSON.stringify(body);
  res.writeHead(statusCode, {
    "content-type": "application/json",
    "content-length": Buffer.byteLength(data),
  });
  res.end(data);
}
