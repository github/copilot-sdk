# Examples: New Java E2E Test with YAML Snapshot

## Example 1: Simple single-turn conversation (no tool calls)

### Snapshot YAML

File: `test/snapshots/system_message_sections/should_use_replaced_identity_section_in_response.yaml`

```yaml
models:
  - claude-sonnet-4.5
conversations:
  - messages:
      - role: system
        content: ${system}
      - role: user
        content: Who are you?
      - role: assistant
        content: >-
          I'm Botanica, your helpful gardening assistant! I'm here to help you
          with all things related to plants and gardening. Whether you have
          questions about plant care, garden design, soil preparation, pest
          management, or anything else in the world of gardening, I'm happy to
          help. What would you like to know about plants or gardening today?
```

### Corresponding Java test method

```java
@Test
void shouldUseReplacedIdentitySectionInResponse() throws Exception {
    ctx.configureForTest("system_message_sections", "should_use_replaced_identity_section_in_response");

    var systemMessage = new SystemMessageConfig().setMode(SystemMessageMode.CUSTOMIZE)
            .setSections(Map.of(SystemMessageSections.IDENTITY,
                    new SectionOverride().setAction(SectionOverrideAction.REPLACE)
                            .setContent("You are a helpful gardening assistant called Botanica. "
                                    + "You only answer questions about plants and gardening.")));

    try (CopilotClient client = ctx.createClient()) {
        CopilotSession session = client.createSession(new SessionConfig().setSystemMessage(systemMessage)
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(30, TimeUnit.SECONDS);

        try {
            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions().setPrompt("Who are you?"), 60_000).get(90, TimeUnit.SECONDS);

            assertNotNull(response, "Expected a response from the assistant");
            String content = response.getData().content().toLowerCase();
            assertTrue(content.contains("botanica") || content.contains("garden") || content.contains("plant"),
                    "Expected response to reflect the replaced identity section, but got: "
                            + response.getData().content());
        } finally {
            session.close();
        }
    }
}
```

**Key points:**
- `configureForTest("system_message_sections", "should_use_replaced_identity_section_in_response")` 
  maps to `test/snapshots/system_message_sections/should_use_replaced_identity_section_in_response.yaml`
- The prompt `"Who are you?"` exactly matches the YAML's user content
- `ctx.createClient()` uses `fake-token-for-e2e-tests` — works in CI

---

## Example 2: Multi-turn with tool calls (from existing tests)

### Snapshot YAML

File: `test/snapshots/system_message_transform/should_invoke_transform_callbacks_with_section_content.yaml`

```yaml
models:
  - claude-sonnet-4.5
conversations:
  # First exchange: model decides to call tools
  - messages:
      - role: system
        content: ${system}
      - role: user
        content: Read the contents of test.txt and tell me what it says
      - role: assistant
        content: I'll read the test.txt file for you.
        tool_calls:
          - id: toolcall_0
            type: function
            function:
              name: report_intent
              arguments: '{"intent":"Reading test.txt file"}'
          - id: toolcall_1
            type: function
            function:
              name: view
              arguments: '{"path":"${workdir}/test.txt"}'
  # Second exchange: after tool results come back, model gives final answer
  - messages:
      - role: system
        content: ${system}
      - role: user
        content: Read the contents of test.txt and tell me what it says
      - role: assistant
        content: I'll read the test.txt file for you.
        tool_calls:
          - id: toolcall_0
            type: function
            function:
              name: report_intent
              arguments: '{"intent":"Reading test.txt file"}'
          - id: toolcall_1
            type: function
            function:
              name: view
              arguments: '{"path":"${workdir}/test.txt"}'
      - role: tool
        tool_call_id: toolcall_0
        content: Intent logged
      - role: tool
        tool_call_id: toolcall_1
        content: 1. Hello transform!
      - role: assistant
        content: |-
          The file test.txt contains:
          ```
          Hello transform!
          ```
```

### Corresponding Java test method

```java
@Test
void transformOnIdentitySectionReceivesNonEmptyContent() throws Exception {
    ctx.configureForTest("system_message_transform", "should_invoke_transform_callbacks_with_section_content");

    ConcurrentHashMap<String, String> capturedContent = new ConcurrentHashMap<>();

    var systemMessage = new SystemMessageConfig().setMode(SystemMessageMode.CUSTOMIZE)
            .setSections(Map.of(SystemMessageSections.IDENTITY, new SectionOverride().setTransform(content -> {
                capturedContent.put("identity", content);
                return CompletableFuture.completedFuture(content);
            }), SystemMessageSections.TONE, new SectionOverride().setTransform(content -> {
                capturedContent.put("tone", content);
                return CompletableFuture.completedFuture(content);
            })));

    try (CopilotClient client = ctx.createClient()) {
        // Create the file the snapshot expects the CLI view tool to read
        Path testFile = ctx.getWorkDir().resolve("test.txt");
        Files.writeString(testFile, "Hello transform!");

        CopilotSession session = client.createSession(new SessionConfig().setSystemMessage(systemMessage)
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(30, TimeUnit.SECONDS);

        try {
            AssistantMessageEvent response = session
                    .sendAndWait(new MessageOptions()
                            .setPrompt("Read the contents of test.txt and tell me what it says"), 60_000)
                    .get(90, TimeUnit.SECONDS);

            assertNotNull(response, "Expected a response from the assistant");

            String identityContent = capturedContent.get("identity");
            assertNotNull(identityContent, "Expected identity transform callback to be invoked");
            assertTrue(!identityContent.isBlank(), "Expected identity section content to be non-empty");
        } finally {
            session.close();
        }
    }
}
```

**Key points:**
- The file `test.txt` must be created in `ctx.getWorkDir()` **before** sending the prompt
- The CLI's `view` tool will actually read that file; the YAML's tool result `"1. Hello transform!"` must match what `view` returns for that file content
- Two conversation entries: first for the tool-call decision, second for the final response after tool results
