# Session management

Session management coordinates independent local root sessions without sharing their conversation history.
It is experimental and disabled by default.
The runtime enforces the same policy for SDK calls and model tools.

## Discovery

`session.rpc.management.list({})` discovers other live, opted-in root sessions on the same machine that use the same resolved configuration directory.
The default `all` scope works within one process and across separate runtime processes.
Results contain the ID, name, working directory, selected model, busy/idle state and optional `parentSessionId`.
When known, `sessionStatePath` identifies the peer's actual local session-state directory containing `events.jsonl`.
Use this returned path instead of reconstructing a location under the user's home directory.
The path is omitted when the peer has no recorded local state directory.
The transcript file can be absent until the peer has activity to persist.
An exact `cwd` filter is optional.
The caller, subagents, remote sessions, closed sessions and other configuration profiles are excluded.
Saved history by itself is not a live session.

Session management does not expose a peer-history reading endpoint.
`message` accepts peer text, wakes an idle session and does not interrupt a busy session's running tool.
Peer text and history never constitute human approval.
Message acceptance does not mean a reply or completed task.

## Names and model tools

The model tools are `list_sessions`, `create_session`, `send_session_message`, `close_session` and `rename_session`.
To inspect peer history with file tools, use the `sessionStatePath` returned by `list_sessions`.
The `send_session_message` and `close_session` tools accept a full session ID or an exact, case-sensitive current name in `session_id`.
Names are the existing session names, not separate handles, and do not need to be unique.
Name lookup requires list permission and rejects duplicate names.
During a live caller session, a previously used name cannot silently switch to a different session.
Successful operations return the resolved `sessionId`; use that full ID for subsequent calls.
The SDK message and close methods require full IDs in `targetSessionId`.

The `rename_session` model tool changes only the current session's automatic name and follows the management group's master switch.
It preserves a user-chosen name instead of overwriting it.
The CLI `/rename` command and SDK `session.rpc.name.set` remain the manual naming interfaces.

In the CLI, the existing `@` picker searches session names, IDs and working directories alongside files.
Session entries stay on one line, truncating long names at the end and long working directories at the start while reserving space for the short ID.
Selecting an entry stores its full ID, so later renames cannot redirect the reference.

## Process scope

```typescript
await session.rpc.management.configure({ enabled: true, scope: "in-process" });
```

`scope` is `all` (default) or `in-process`.
It limits discovery and known-ID message and close operations.
In-process peers are not exposed to other runtime processes, even when those processes use `all`.
Peers in the same runtime process can still access an in-process session if they share its resolved configuration directory and opt in.
Changing scope replaces the receiver capability, so credentials published under `all` cannot keep accessing a newly restricted session.
Enabling or disabling the master switch preserves scope; changing scope alone preserves tool switches, including a close opt-out.
Created roots inherit scope along with the rest of the caller's policy.
The interactive CLI persists this as `sessionManagementTools.scope` and offers it in `/settings`.

The process boundary belongs to the native runtime hosting the session, not its terminal, editor or SDK client.
Local ACP sessions use that same boundary: roots in one ACP server share a process, while separate ACP servers do not.
A genuinely local AHP-host root is evaluated on its host.
Remote-marked sessions, client-side proxies, inherited-filesystem sessions and Mission Control records remain excluded.
Management does not query remote backends or tunnel discovery between machines.
A trusted SDK client may connect remotely over a supported transport; this peer policy does not restrict the client's privileged connection APIs.
Creation still requires the owning host's creation handler, and close still requires its normal lifecycle integration.
Configuring scope does not add editor-side or proxy-side creation and close handlers.

## Creation

```typescript
await parent.rpc.management.configure({ enabled: true });
const created = await parent.rpc.management.create({
    prompt: "Investigate the parser and report the result.",
    name: "Parser investigation",
    // Optional overrides:
    model: "claude-sonnet-5",
    reasoningEffort: "high",
    contextTier: "default",
    cwd: "/workspace/project",
});
```

Omitted launch options inherit the parent's current values, including changes made after that parent was created.
`contextTier` is `default` or `long_context`, subject to the selected model's normal validation.
It is not an arbitrary token-limit override.
An explicit `cwd` must be an existing absolute directory.
In the interactive CLI, a different directory must already be trusted; creation does not grant folder trust.
The child cannot receive broader permissions or discard the parent's model restrictions.

Node.js hosts register `parent.setSessionCreationHandler(...)`; Rust hosts use `set_session_creation_handler`.
Go, Python, .NET and Java omit `management.create` until they support host callbacks.
Callbacks receive effective launch options, the permission ceiling, `parentSessionId` and a runtime-allocated `newSessionId`.
Hosts must create and retain that exact ID with those options, route approvals independently and return `{ sessionId }`.
The runtime validates creation and records lineage before sending the task.
Hosts must clean up cancelled or failed creations.
Registration does not enable management tools; created sessions inherit the caller's policy, including close opt-outs.
Each created session needs its own handler to create further sessions.

## Durable lineage

The runtime records the creator in the child's existing `workspace.yaml` as `created_by_session_id`.
This is creation lineage for independent roots, not a subagent or fork relationship.
It survives close and cold resume, and appears as `parentSessionId` in live discovery.
Generic workspace metadata updates cannot introduce, replace or erase it.
There is no separate ancestry registry.

## Close

Close is enabled by default when the session-management group is enabled:

```typescript
await parent.rpc.management.close({ targetSessionId: created.sessionId });
```

Enabling the main group enables list, create, message, close and self-renaming.
Disable close in that same request's `tools` override or in a later request:

```typescript
await parent.rpc.management.configure({ tools: { close: false } });
```

An explicit opt-out is respected until close is enabled again or the main group is re-enabled.
An existing saved `sessionManagementTools.close: false` remains disabled when upgrading; the new default does not overwrite saved preferences.
The interactive CLI persists this preference as `sessionManagementTools.close`.

A session may close only live opted-in sessions it created, or sessions created recursively by its descendants.
The runtime follows durable lineage, so a closed intermediate parent does not remove an ancestor's authority.
Self, ancestors, siblings, unrelated sessions and other configuration profiles are not valid targets.
The target's owning host completes its normal close lifecycle before `{ closed: true }` is returned.
Close stops the active session and releases its resources, but preserves saved history for later resume.
It does not implicitly close the target's children.
When the interactive CLI's foreground session is closed, the UI normally moves to a fresh session.
If the user switches away while that replacement is being prepared, close preserves the new foreground and removes only the requested session.
