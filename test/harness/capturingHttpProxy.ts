/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http, { RequestOptions } from "http";
import https from "https";

/**
 * Intended to be used in E2E tests so they can assert about requests/responses.
 */
export class CapturingHttpProxy {
  private readonly capturedExchanges: CapturedExchange[] = [];
  private readonly activeRequests = new Set<http.ClientRequest>();
  private readonly activeResponses = new Set<http.IncomingMessage>();
  private server?: http.Server;
  private stopPromise?: Promise<void>;

  constructor(private targetUrl: string) {}

  get exchanges(): ReadonlyArray<CapturedExchange> {
    return this.capturedExchanges;
  }

  async start(): Promise<string> {
    const targetUrlObj = new URL(this.targetUrl);
    const isHttps = targetUrlObj.protocol === "https:";

    this.server = http.createServer((req, res) => {
      const chunks: Buffer[] = [];

      req.on("data", (chunk: Buffer) => {
        chunks.push(chunk);
      });

      req.on("end", () => {
        const body = Buffer.concat(chunks).toString("utf8");
        const startTime = Date.now();

        const capturedRequest: CapturedRequest = {
          method: req.method || "GET",
          url: req.url || "/",
          headers: req.headers,
          body,
          startTime,
        };

        const exchange: CapturedExchange = {
          request: capturedRequest,
        };

        this.capturedExchanges.push(exchange);

        // Copy headers but update Host to match target
        const proxyHeaders = { ...req.headers };
        proxyHeaders.host = targetUrlObj.host;
        delete proxyHeaders.connection;

        let responseStatusCode: number | undefined;
        let responseHeaders: http.IncomingHttpHeaders | undefined;
        const responseChunks: Buffer[] = [];
        this.performRequest({
          isHttps,
          requestOptions: {
            hostname: targetUrlObj.hostname,
            port: targetUrlObj.port || (isHttps ? 443 : 80),
            path: req.url,
            method: req.method,
            headers: proxyHeaders,
          },
          body,
          onResponseStart: (statusCode, headers) => {
            responseStatusCode = statusCode;
            responseHeaders = headers;
            res.writeHead(statusCode, responseHeaders);
          },
          onData: (chunk) => {
            responseChunks.push(chunk);
            res.write(chunk);
          },
          onResponseEnd: () => {
            const endTime = Date.now();
            const responseBody = Buffer.concat(responseChunks).toString("utf8");

            exchange.response = {
              statusCode: responseStatusCode || 500,
              headers: responseHeaders || {},
              body: responseBody,
              endTime,
            };

            exchange.durationMs = endTime - startTime;

            res.end();
          },
          onError: (err) => {
            if (!this.server) {
              res.destroy();
              return;
            }
            console.error("Error in proxying request:", err);
            const endTime = Date.now();
            const formattedError =
              err instanceof Error
                ? `${err.message}\n${err.stack}`
                : String(err);
            const errorHeaders = { "x-github-request-id": "proxy-error" };
            exchange.response = {
              statusCode: 500,
              headers: errorHeaders,
              body: `Proxy error: ${formattedError}`,
              endTime,
            };

            exchange.durationMs = endTime - startTime;

            res.writeHead(exchange.response.statusCode, errorHeaders);
            res.end("Proxy error");
          },
        });
      });
    });

    return new Promise((resolve, reject) => {
      this.server!.on("error", (err) => {
        reject(err);
      });

      this.server!.listen(0, "127.0.0.1", () => {
        const addr = this.server!.address();
        if (addr instanceof Object) {
          resolve(`http://${addr.address}:${addr.port}`);
        } else {
          reject(new Error("Failed to start proxy server"));
        }
      });
    });
  }

  async stop(): Promise<void> {
    if (this.stopPromise) {
      return this.stopPromise;
    }

    const server = this.server;
    if (!server) {
      return;
    }

    this.server = undefined;
    this.stopPromise = (async () => {
      const closed = new Promise<void>((resolve, reject) => {
        server.close((err) => {
          if (err) {
            reject(err);
          } else {
            resolve();
          }
        });
      });

      // server.close() waits for active connections. A replayed streaming request
      // can otherwise wedge fixture teardown after its test has already passed.
      server.closeAllConnections();
      for (const response of this.activeResponses) {
        response.destroy();
      }
      this.activeResponses.clear();
      for (const request of this.activeRequests) {
        request.destroy();
      }
      this.activeRequests.clear();

      await closed;
    })();
    return this.stopPromise;
  }

  performRequest(options: PerformRequestOptions): void {
    if (this.stopPromise) {
      options.onError(new Error("Proxy is stopping"));
      return;
    }

    const protocol = options.isHttps ? https : http;
    const upstreamRequest = protocol.request(
      options.requestOptions,
      (upstreamResponse) => {
        this.activeResponses.add(upstreamResponse);
        upstreamResponse.once("close", () => {
          this.activeResponses.delete(upstreamResponse);
        });
        options.onResponseStart(
          upstreamResponse.statusCode || 500,
          upstreamResponse.headers,
        );
        upstreamResponse.on("data", options.onData);
        upstreamResponse.on("end", options.onResponseEnd);
      },
    );

    this.activeRequests.add(upstreamRequest);
    upstreamRequest.once("close", () => {
      this.activeRequests.delete(upstreamRequest);
    });
    upstreamRequest.on("error", options.onError);

    if (options.body) {
      upstreamRequest.write(options.body);
    }

    upstreamRequest.end();
  }

  protected clearExchanges(): void {
    this.capturedExchanges.length = 0;
  }
}

export interface PerformRequestOptions {
  isHttps: boolean;
  requestOptions: RequestOptions;
  body: string | undefined;
  onResponseStart: (
    statusCode: number,
    responseHeaders: http.IncomingHttpHeaders,
  ) => void;
  onData: (chunk: Buffer) => void;
  onResponseEnd: () => void;
  onError: (err: Error | string) => void;
}

export interface CapturedRequest {
  readonly method: string;
  readonly url: string;
  readonly headers: http.IncomingHttpHeaders;
  readonly body: string;
  readonly startTime: number;
}

export interface CapturedResponse {
  readonly statusCode: number;
  readonly headers: http.IncomingHttpHeaders;
  readonly body: string;
  readonly endTime: number;
}

export interface CapturedExchange {
  request: CapturedRequest;
  response?: CapturedResponse;
  durationMs?: number;
}
