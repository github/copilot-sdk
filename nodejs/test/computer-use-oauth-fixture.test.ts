/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { test } from "vitest";
import { startComputerUseOAuthServer } from "../../test/harness/test-mcp-computer-use-oauth-server.mjs";

test("rejects token exchanges without a registered client", async (context) => {
    const { expect } = context;
    const starting = startComputerUseOAuthServer();
    context.onTestFinished(async () => {
        const [result] = await Promise.allSettled([starting]);
        if (result.status === "fulfilled") await result.value.close();
    });
    const server = await starting;
    context.signal.throwIfAborted();
    const exchange = (clientId: string) =>
        fetch(`${server.url}/token`, {
            method: "POST",
            body: new URLSearchParams({
                grant_type: "authorization_code",
                code: "accepted-code",
                code_verifier: "test-verifier",
                ...(clientId ? { client_id: clientId } : {}),
            }),
        });
    for (const clientId of ["", "wrong-client", "registered-client"]) {
        const response = await exchange(clientId);
        expect(response.status).toBe(400);
        expect(await response.json()).toEqual({ error: "invalid_client" });
    }
    const registration = await fetch(`${server.url}/register`, { method: "POST" });
    expect(registration.status).toBe(201);
    const { client_id: clientId } = await registration.json();
    for (const invalidClient of ["", "wrong-client"]) {
        const response = await exchange(invalidClient);
        expect(response.status).toBe(400);
        expect(await response.json()).toEqual({ error: "invalid_client" });
    }
    expect((await exchange(clientId)).status).toBe(200);
});

test("binds preconfigured-client token exchanges without dynamic registration", async (context) => {
    const { expect } = context;
    const starting = startComputerUseOAuthServer({ preconfiguredClientId: "app-client" });
    context.onTestFinished(async () => {
        const [result] = await Promise.allSettled([starting]);
        if (result.status === "fulfilled") await result.value.close();
    });
    const server = await starting;
    context.signal.throwIfAborted();
    for (const clientId of ["", "wrong-client", "app-client"]) {
        const response = await fetch(`${server.url}/token`, {
            method: "POST",
            body: new URLSearchParams({
                grant_type: "authorization_code",
                code: "accepted-code",
                code_verifier: "test-verifier",
                ...(clientId ? { client_id: clientId } : {}),
            }),
        });
        expect(response.status).toBe(clientId === "app-client" ? 200 : 400);
        if (clientId !== "app-client") {
            expect(await response.json()).toEqual({ error: "invalid_client" });
        }
    }
    expect(server.requests.map((request) => request.path)).not.toContain("/register");
});
