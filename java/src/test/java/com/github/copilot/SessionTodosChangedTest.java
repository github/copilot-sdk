/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.SessionTodosChangedEvent;
import com.github.copilot.generated.rpc.PlanSqlTodoDependency;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

public class SessionTodosChangedTest {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    void firesSessionTodosChangedAndExposesRowsAndDependencies() throws Exception {
        ctx.configureForTest("session_todos_changed", "fires_session_todos_changed_and_exposes_rows_and_dependencies");

        try (CopilotClient client = ctx.createClient()) {
            CopilotSession session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            CompletableFuture<SessionTodosChangedEvent> todosChanged = new CompletableFuture<>();
            session.on(event -> {
                if (event instanceof SessionTodosChangedEvent todosEvent && !todosChanged.isDone()) {
                    todosChanged.complete(todosEvent);
                }
            });

            session.sendAndWait(new MessageOptions()
                    .setPrompt("Use the sql tool to execute exactly these statements, in order, with no extra rows:\n"
                            + "1. INSERT INTO todos (id, title, status) VALUES ('alpha', 'First todo', 'pending');\n"
                            + "2. INSERT INTO todos (id, title, status) VALUES ('beta', 'Second todo', 'done');\n"
                            + "3. INSERT INTO todo_deps (todo_id, depends_on) VALUES ('beta', 'alpha');\n"
                            + "Then stop. Do not insert any other rows or create any other tables."))
                    .get(120, TimeUnit.SECONDS);

            assertNotNull(todosChanged.get(15, TimeUnit.SECONDS),
                    "Should have received at least one session.todos_changed event");

            var result = session.getRpc().plan.readSqlTodosWithDependencies().get(15, TimeUnit.SECONDS);
            assertEquals(2, result.rows().size());
            var ids = result.rows().stream().map(row -> row.id()).filter(id -> id != null).sorted().toList();

            assertEquals(java.util.List.of("alpha", "beta"), ids);
            assertTrue(result.dependencies().stream().anyMatch(SessionTodosChangedTest::isBetaDependsOnAlpha),
                    "Should contain beta -> alpha dependency");

            session.close();
        }
    }

    private static boolean isBetaDependsOnAlpha(PlanSqlTodoDependency dependency) {
        return "beta".equals(dependency.todoId()) && "alpha".equals(dependency.dependsOn());
    }
}
