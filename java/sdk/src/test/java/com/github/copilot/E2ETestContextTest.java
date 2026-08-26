/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class E2ETestContextTest {

    @Test
    void expectedUserPromptsParseFoldedBlockScalars() {
        String snapshot = """
                conversations:
                  - messages:
                      - role: user
                        content: First prompt
                          continued here.
                      - role: assistant
                        content: response
                      - role: user
                        content: >-
                          <system_notification>

                          Agent completed successfully.

                          </system_notification>
                """;

        assertEquals(
                List.of("First prompt continued here.",
                        "<system_notification>\nAgent completed successfully.\n</system_notification>"),
                E2ETestContext.parseExpectedUserPrompts(snapshot));
    }
}
