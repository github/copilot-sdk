/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import fs, { realpathSync } from "node:fs";
import os from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll, type SessionEvent } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

/**
 * E2E coverage for the runtime's `session.todos_changed` event and
 * `session.plan.readSqlTodosWithDependencies` RPC. We let the agent drive the
 * built-in `sql` tool (default mode = "copilot-cli") to insert known rows into
 * the prompted `todos` table, then assert both that the lightweight signal
 * event fired and that the structured query API returns those rows.
 */
describe("Todos changed event + readSqlTodosWithDependencies", async () => {
    const baseDir = realpathSync(fs.mkdtempSync(join(os.tmpdir(), "copilot-todos-e2e-")));
    const { copilotClient: client } = await createSdkTestContext({
        copilotClientOptions: { baseDirectory: baseDir },
    });

    it(
        "fires session.todos_changed and exposes rows + dependencies",
        { timeout: 120_000 },
        async () => {
            const session = await client.createSession({ onPermissionRequest: approveAll });

            const events: SessionEvent[] = [];
            session.on((event) => {
                events.push(event);
            });

            await session.sendAndWait({
                prompt:
                    "Use the sql tool to execute exactly these statements, in order, with no extra rows:\n" +
                    "1. CREATE TABLE todos (id TEXT PRIMARY KEY, title TEXT, description TEXT, status TEXT, created_at TEXT, updated_at TEXT);\n" +
                    "2. CREATE TABLE todo_deps (todo_id TEXT, depends_on TEXT, PRIMARY KEY (todo_id, depends_on));\n" +
                    "3. INSERT INTO todos (id, title, status) VALUES ('alpha', 'First todo', 'pending');\n" +
                    "4. INSERT INTO todos (id, title, status) VALUES ('beta', 'Second todo', 'done');\n" +
                    "5. INSERT INTO todo_deps (todo_id, depends_on) VALUES ('beta', 'alpha');\n" +
                    "Then stop. Do not insert any other rows or create any other tables.",
            });

            const todosEvents = events.filter((e) => e.type === "session.todos_changed");
            expect(todosEvents.length).toBeGreaterThanOrEqual(1);

            const result = await session.rpc.plan.readSqlTodosWithDependencies();
            const ids = result.rows.map((r) => r.id).filter((x): x is string => !!x).sort();
            expect(ids).toEqual(["alpha", "beta"]);

            const edge = result.dependencies.find(
                (d) => d.todoId === "beta" && d.dependsOn === "alpha"
            );
            expect(edge).toBeDefined();

            await session.disconnect();
        }
    );
});
