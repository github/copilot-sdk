"""Offline E2E coverage for generated outbound RPC methods."""

from __future__ import annotations

import dataclasses
import enum
import inspect
import json
import os
import types
import typing
from datetime import UTC, datetime
from pathlib import Path
from uuid import UUID

import pytest

from copilot import CopilotClient, RuntimeConnection, rpc
from copilot._jsonrpc import JsonRpcError
from copilot.generated import session_events as generated_session_events
from copilot.session import CopilotSession

from .testharness import E2ETestContext

pytestmark = pytest.mark.asyncio(loop_scope="module")

_SESSION_ID = "generated-rpc-surface-session"
_SAMPLE_UUID = UUID("12345678-1234-5678-1234-567812345678")
_OBJECTIVE_METHODS = {
    "session.workspaces.readAutopilotObjective",
    "session.workspaces.writeAutopilotObjective",
    "session.workspaces.deleteAutopilotObjective",
    "session.workspaces.autopilotObjectiveExists",
}

_GAP_METHODS = {
    "registerExtensionLaunchProvider": rpc.ServerRpc.register_extension_launch_provider,
    "hooks.discover": rpc.ServerHooksApi.discover,
    "models.getBuiltInCatalog": rpc.ServerModelsApi.get_built_in_catalog,
    "mcp.planInstall": rpc.ServerMcpApi.plan_install,
    "extensions.discover": rpc.ServerExtensionsApi.discover,
    "extensions.enable": rpc.ServerExtensionsApi.enable,
    "extensions.disable": rpc.ServerExtensionsApi.disable,
    "catalog.search": rpc.ServerCatalogApi.search,
    "plugins.builtin.set": rpc.ServerPluginsBuiltinApi.set,
    "skills.config.setSkillDisabled": rpc.ServerSkillsConfigApi.set_skill_disabled,
    "commands.list": rpc.ServerCommandsApi.list,
    "managedSettings.read": rpc.ServerManagedSettingsApi.read,
    "llmInference.setProvider": rpc.ServerLlmInferenceApi.set_provider,
    "sessions.getClientMetadata": rpc.ServerSessionsApi.get_client_metadata,
    "sessions.readPersistedEvents": rpc.ServerSessionsApi.read_persisted_events,
    "session.send": rpc.SessionRpc.send,
    "session.sendMessages": rpc.SessionRpc.send_messages,
    "session.abort": rpc.SessionRpc.abort,
    "session.interruptMainTurn": rpc.SessionRpc.interrupt_main_turn,
    "session.cancelAllBackgroundAgents": rpc.SessionRpc.cancel_all_background_agents,
    "session.log": rpc.SessionRpc.log,
    "session.sandbox.getEnforcementStatus": rpc.SandboxApi.get_enforcement_status,
    "session.sandbox.disableForSession": rpc.SandboxApi.disable_for_session,
    "session.debug.collectLogs": rpc.DebugApi.collect_logs,
    "session.factory.run": rpc.FactoryApi.run,
    "session.factory.resume": rpc.FactoryApi.resume,
    "session.factory.getRun": rpc.FactoryApi.get_run,
    "session.factory.listRuns": rpc.FactoryApi.list_runs,
    "session.factory.getRunDetail": rpc.FactoryApi.get_run_detail,
    "session.factory.getRunProgress": rpc.FactoryApi.get_run_progress,
    "session.factory.cancel": rpc.FactoryApi.cancel,
    "session.factory.pause": rpc.FactoryApi.pause,
    "session.factory.log": rpc.FactoryApi.log,
    "session.factory.agent": rpc.FactoryApi.agent,
    "session.factory.journal.get": rpc.FactoryJournalApi.get,
    "session.factory.journal.put": rpc.FactoryJournalApi.put,
    "session.model.switchAutoTier": rpc.ModelApi.switch_auto_tier,
    "session.model.setAllowedModels": rpc.ModelApi.set_allowed_models,
    "session.workspaces.updateMetadata": rpc.WorkspacesApi.update_metadata,
    "session.workspaces.ensure": rpc.WorkspacesApi.ensure,
    "session.workspaces.statFile": rpc.WorkspacesApi.stat_file,
    "session.workspaces.createDirectory": rpc.WorkspacesApi.create_directory,
    "session.workspaces.removePath": rpc.WorkspacesApi.remove_path,
    "session.workspaces.renamePath": rpc.WorkspacesApi.rename_path,
    "session.workspaces.addSummary": rpc.WorkspacesApi.add_summary,
    "session.workspaces.truncateSummaries": rpc.WorkspacesApi.truncate_summaries,
    "session.workspaces.readAutopilotObjective": rpc.WorkspacesApi.read_autopilot_objective,
    "session.workspaces.writeAutopilotObjective": rpc.WorkspacesApi.write_autopilot_objective,
    "session.workspaces.deleteAutopilotObjective": rpc.WorkspacesApi.delete_autopilot_objective,
    "session.workspaces.autopilotObjectiveExists": (rpc.WorkspacesApi.autopilot_objective_exists),
    "session.autopilotObjective.getState": rpc.AutopilotObjectiveApi.get_state,
    "session.agent.setPrompt": rpc.AgentApi.set_prompt,
    "session.tasks.register": rpc.TasksApi.register,
    "session.tasks.update": rpc.TasksApi.update,
    "session.mcp.moveLoadingToBackground": rpc.McpApi.move_loading_to_background,
    "session.mcp.startServer": rpc.McpApi.start_server,
    "session.mcp.restartServer": rpc.McpApi.restart_server,
    "session.mcp.oauth.authenticationStateChanged": (rpc.McpOauthApi.authentication_state_changed),
    "session.mcp.oauth.probe": rpc.McpOauthApi.probe,
    "session.mcp.oauth.respond": rpc.McpOauthApi.respond,
    "session.mcp.resources.read": rpc.McpResourcesApi.read,
    "session.mcp.resources.list": rpc.McpResourcesApi.list,
    "session.mcp.resources.listTemplates": rpc.McpResourcesApi.list_templates,
    "session.tools.execute": rpc.ToolsApi.execute,
    "session.tools.getBuiltinDescriptors": rpc.ToolsApi.get_builtin_descriptors,
    "session.tools.taskCompleteEventData": rpc.ToolsApi.task_complete_event_data,
    "session.tools.set": rpc.ToolsApi.set,
    "session.permissions.configure": rpc.PermissionsApi.configure,
    "session.permissions.pendingRequests": rpc.PermissionsApi.pending_requests,
    "session.permissions.modifyRules": rpc.PermissionsApi.modify_rules,
    "session.permissions.setRequired": rpc.PermissionsApi.set_required,
    "session.permissions.notifyPromptShown": rpc.PermissionsApi.notify_prompt_shown,
    "session.permissions.paths.list": rpc.PermissionsPathsApi.list,
    "session.permissions.paths.add": rpc.PermissionsPathsApi.add,
    "session.permissions.paths.updatePrimary": rpc.PermissionsPathsApi.update_primary,
    "session.permissions.paths.isPathWithinAllowedDirectories": (
        rpc.PermissionsPathsApi.is_path_within_allowed_directories
    ),
    "session.permissions.paths.isPathWithinWorkspace": (
        rpc.PermissionsPathsApi.is_path_within_workspace
    ),
    "session.permissions.locations.resolve": rpc.PermissionsLocationsApi.resolve,
    "session.permissions.locations.apply": rpc.PermissionsLocationsApi.apply,
    "session.permissions.locations.addToolApproval": (
        rpc.PermissionsLocationsApi.add_tool_approval
    ),
    "session.permissions.folderTrust.isTrusted": rpc.PermissionsFolderTrustApi.is_trusted,
    "session.permissions.folderTrust.addTrusted": rpc.PermissionsFolderTrustApi.add_trusted,
    "session.permissions.urls.setUnrestrictedMode": (rpc.PermissionsUrlsApi.set_unrestricted_mode),
    "session.metadata.getClientMetadata": rpc.MetadataApi.get_client_metadata,
    "session.metadata.updateClientMetadata": rpc.MetadataApi.update_client_metadata,
    "session.contentExclusion.checkPaths": rpc.ContentExclusionApi.check_paths,
    "session.history.clearContext": rpc.HistoryApi.clear_context,
    "session.queue.moveItem": rpc.QueueApi.move_item,
    "session.queue.insertAt": rpc.QueueApi.insert_at,
    "session.queue.removeAt": rpc.QueueApi.remove_at,
    "session.queue.updateText": rpc.QueueApi.update_text,
    "session.queue.duplicateAt": rpc.QueueApi.duplicate_at,
    "session.queue.setDrainPaused": rpc.QueueApi.set_drain_paused,
    "session.queue.sendNow": rpc.QueueApi.send_now,
    "session.limitPrediction.predict": rpc.LimitPredictionApi.predict,
}

_FAKE_CLI = r"""
const fs = require("fs");

function argValue(name) {
  const index = process.argv.indexOf(name);
  return index >= 0 ? process.argv[index + 1] : undefined;
}

const captureFile = argValue("--capture-file");
const responses = JSON.parse(fs.readFileSync(argValue("--responses-file"), "utf8"));
const requests = [];
let objective = null;
let buffer = Buffer.alloc(0);

function saveCapture() {
  fs.writeFileSync(captureFile, JSON.stringify({ requests }));
}

function writeResponse(id, result) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, result });
  process.stdout.write(
    `Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`
  );
}

function writeError(id, code, message, data) {
  const body = JSON.stringify({ jsonrpc: "2.0", id, error: { code, message, data } });
  process.stdout.write(
    `Content-Length: ${Buffer.byteLength(body, "utf8")}\r\n\r\n${body}`
  );
}

function handleMessage(message) {
  if (!Object.prototype.hasOwnProperty.call(message, "id")) return;

  requests.push({ method: message.method, params: message.params });
  saveCapture();

  if (message.method === "connect") {
    writeResponse(message.id, { ok: true, protocolVersion: 4, version: "fake" });
    return;
  }
  if (message.method === "ping") {
    writeResponse(message.id, {
      message: "pong",
      protocolVersion: 4,
      timestamp: 1770000000000,
    });
    return;
  }
  if (message.method === "catalog.search" && message.params.query === "raise-jsonrpc-error") {
    writeError(message.id, -32077, "deterministic catalog failure", {
      retryable: false,
      source: "fake-cli",
    });
    return;
  }
  if (message.method === "session.workspaces.writeAutopilotObjective") {
    const operation = objective === null ? "created" : "updated";
    objective = message.params.content;
    writeResponse(message.id, { operation });
    return;
  }
  if (message.method === "session.workspaces.readAutopilotObjective") {
    writeResponse(message.id, { content: objective });
    return;
  }
  if (message.method === "session.workspaces.autopilotObjectiveExists") {
    writeResponse(message.id, { exists: objective !== null });
    return;
  }
  if (message.method === "session.workspaces.deleteAutopilotObjective") {
    const deleted = objective !== null;
    objective = null;
    writeResponse(message.id, { deleted });
    return;
  }

  writeResponse(message.id, responses[message.method] ?? {});
}

function processBuffer() {
  while (true) {
    const headerEnd = buffer.indexOf("\r\n\r\n");
    if (headerEnd < 0) return;
    const header = buffer.subarray(0, headerEnd).toString("utf8");
    const match = /Content-Length:\s*(\d+)/i.exec(header);
    if (!match) throw new Error("Missing Content-Length header");
    const length = Number(match[1]);
    const bodyStart = headerEnd + 4;
    const bodyEnd = bodyStart + length;
    if (buffer.length < bodyEnd) return;
    const body = buffer.subarray(bodyStart, bodyEnd).toString("utf8");
    buffer = buffer.subarray(bodyEnd);
    handleMessage(JSON.parse(body));
  }
}

saveCapture();
process.stdin.on("data", chunk => {
  buffer = Buffer.concat([buffer, chunk]);
  processBuffer();
});
process.stdin.resume();
"""


def _type_globals() -> dict[str, object]:
    return {
        **vars(generated_session_events),
        **vars(rpc),
    }


def _sample_value(
    annotation: object,
    *,
    depth: int = 0,
    stack: tuple[object, ...] = (),
) -> object:
    if annotation in (inspect.Signature.empty, typing.Any, object):
        return {"sample": "value"}
    if annotation in (None, type(None)):
        return None

    origin = typing.get_origin(annotation)
    arguments = typing.get_args(annotation)
    if origin in (typing.Union, types.UnionType):
        choices = [item for item in arguments if item is not type(None)]
        return _sample_value(choices[0] if choices else type(None), depth=depth, stack=stack)
    if origin is typing.Literal:
        return arguments[0]
    if origin is list:
        return [_sample_value(arguments[0], depth=depth + 1, stack=stack)]
    if origin is dict:
        return {"key": _sample_value(arguments[1], depth=depth + 1, stack=stack)}
    if origin is tuple:
        return tuple(
            _sample_value(item, depth=depth + 1, stack=stack)
            for item in arguments
            if item is not Ellipsis
        )
    if origin is typing.Annotated:
        return _sample_value(arguments[0], depth=depth, stack=stack)

    if isinstance(annotation, type) and issubclass(annotation, enum.Enum):
        return next(iter(annotation))
    if annotation is str:
        return "sample-value"
    if annotation is bool:
        return True
    if annotation is int:
        return 7
    if annotation is float:
        return 1.5
    if annotation is datetime:
        return datetime(2026, 1, 2, 3, 4, 5, tzinfo=UTC)
    if annotation is UUID:
        return _SAMPLE_UUID
    if annotation is list:
        return [{"sample": "value"}]
    if annotation is dict:
        return {"key": "value"}

    if dataclasses.is_dataclass(annotation):
        if annotation in stack:
            return None
        hints = typing.get_type_hints(
            annotation,
            globalns=_type_globals(),
            localns=_type_globals(),
        )
        values = {}
        for field in dataclasses.fields(annotation):
            required = (
                field.default is dataclasses.MISSING
                and field.default_factory is dataclasses.MISSING
            )
            if required or depth < 4:
                values[field.name] = _sample_value(
                    hints.get(field.name, field.type),
                    depth=depth + 1,
                    stack=(*stack, annotation),
                )
        constructor = typing.cast(typing.Callable[..., object], annotation)
        return constructor(**values)

    return {"sample": "value"}


def _method_hints(method: object) -> dict[str, object]:
    return typing.get_type_hints(
        method,
        globalns=_type_globals(),
        localns=_type_globals(),
    )


def _request_for(method: object) -> typing.Any:
    return _sample_value(_method_hints(method)["params"])


def _json_value(value: object) -> object:
    if hasattr(value, "to_dict"):
        serializer = typing.cast(typing.Callable[[], object], getattr(value, "to_dict"))
        return serializer()
    if isinstance(value, enum.Enum):
        return value.value
    if isinstance(value, datetime):
        return value.isoformat()
    if isinstance(value, UUID):
        return str(value)
    if isinstance(value, list):
        return [_json_value(item) for item in value]
    if isinstance(value, dict):
        return {key: _json_value(item) for key, item in value.items()}
    return value


def _response_payloads() -> dict[str, object]:
    return {
        rpc_method: _json_value(_sample_value(_method_hints(method)["return"]))
        for rpc_method, method in _GAP_METHODS.items()
    }


def _assert_result_matches_payload(result: object, payload: object) -> None:
    assert _json_value(result) == payload


def _expected_params(method: object, *, session_scoped: bool) -> dict[str, object]:
    hints = _method_hints(method)
    params = _request_for(method).to_dict() if "params" in hints else {}
    if session_scoped:
        params["sessionId"] = _SESSION_ID
    return params


def _assert_request_serialization(capture_path: Path) -> None:
    capture = json.loads(capture_path.read_text(encoding="utf-8"))
    requests = capture["requests"]

    for rpc_method, method in _GAP_METHODS.items():
        matching = [request for request in requests if request["method"] == rpc_method]
        assert matching, f"Missing captured request for {rpc_method}"
        expected = _expected_params(method, session_scoped=rpc_method.startswith("session."))
        if rpc_method == "session.workspaces.writeAutopilotObjective":
            expected["content"] = "# Deterministic objective\n\nCover generated RPC methods."
        assert expected in [request["params"] for request in matching]

    catalog_error = next(
        request
        for request in requests
        if request["method"] == "catalog.search"
        and request["params"]["query"] == "raise-jsonrpc-error"
    )
    assert catalog_error["params"]["query"] == "raise-jsonrpc-error"


async def test_generated_rpc_gap_methods_round_trip_over_fake_cli(
    ctx: E2ETestContext,
) -> None:
    work_dir = Path(ctx.work_dir)
    suffix = str(os.getpid())
    cli_path = work_dir / f"generated-rpc-fake-cli-{suffix}.js"
    capture_path = work_dir / f"generated-rpc-capture-{suffix}.json"
    responses_path = work_dir / f"generated-rpc-responses-{suffix}.json"
    cli_path.write_text(_FAKE_CLI, encoding="utf-8")
    responses = _response_payloads()
    responses_path.write_text(json.dumps(responses), encoding="utf-8")

    client = CopilotClient(
        connection=RuntimeConnection.for_stdio(
            path=str(cli_path),
            args=[
                "--capture-file",
                str(capture_path),
                "--responses-file",
                str(responses_path),
            ],
        ),
        working_directory=ctx.work_dir,
        env=ctx.get_env(),
        use_logged_in_user=False,
    )
    results: dict[str, object] = {}

    try:
        await client.start()
        assert client._client is not None
        session = CopilotSession(_SESSION_ID, client._client)

        results[
            "registerExtensionLaunchProvider"
        ] = await client.rpc.register_extension_launch_provider()
        results["hooks.discover"] = await client.rpc.hooks.discover(
            _request_for(rpc.ServerHooksApi.discover)
        )
        results["models.getBuiltInCatalog"] = await client.rpc.models.get_built_in_catalog()
        results["mcp.planInstall"] = await client.rpc.mcp.plan_install(
            _request_for(rpc.ServerMcpApi.plan_install)
        )
        results["extensions.discover"] = await client.rpc.extensions.discover()
        results["extensions.enable"] = await client.rpc.extensions.enable(
            _request_for(rpc.ServerExtensionsApi.enable)
        )
        results["extensions.disable"] = await client.rpc.extensions.disable(
            _request_for(rpc.ServerExtensionsApi.disable)
        )
        results["catalog.search"] = await client.rpc.catalog.search(
            _request_for(rpc.ServerCatalogApi.search)
        )
        results["plugins.builtin.set"] = await client.rpc.plugins.builtin.set(
            _request_for(rpc.ServerPluginsBuiltinApi.set)
        )
        results[
            "skills.config.setSkillDisabled"
        ] = await client.rpc.skills.config.set_skill_disabled(
            _request_for(rpc.ServerSkillsConfigApi.set_skill_disabled)
        )
        results["commands.list"] = await client.rpc.commands.list()
        results["managedSettings.read"] = await client.rpc.managed_settings.read()
        results["llmInference.setProvider"] = await client.rpc.llm_inference.set_provider()
        results["sessions.getClientMetadata"] = await client.rpc.sessions.get_client_metadata(
            _request_for(rpc.ServerSessionsApi.get_client_metadata)
        )
        results["sessions.readPersistedEvents"] = await client.rpc.sessions.read_persisted_events(
            _request_for(rpc.ServerSessionsApi.read_persisted_events)
        )

        results["session.send"] = await session.rpc.send(_request_for(rpc.SessionRpc.send))
        results["session.sendMessages"] = await session.rpc.send_messages(
            _request_for(rpc.SessionRpc.send_messages)
        )
        results["session.abort"] = await session.rpc.abort(_request_for(rpc.SessionRpc.abort))
        results["session.interruptMainTurn"] = await session.rpc.interrupt_main_turn(
            _request_for(rpc.SessionRpc.interrupt_main_turn)
        )
        results[
            "session.cancelAllBackgroundAgents"
        ] = await session.rpc.cancel_all_background_agents()
        results["session.log"] = await session.rpc.log(_request_for(rpc.SessionRpc.log))
        results[
            "session.sandbox.getEnforcementStatus"
        ] = await session.rpc.sandbox.get_enforcement_status()
        results[
            "session.sandbox.disableForSession"
        ] = await session.rpc.sandbox.disable_for_session(
            _request_for(rpc.SandboxApi.disable_for_session)
        )
        results["session.debug.collectLogs"] = await session.rpc.debug.collect_logs(
            _request_for(rpc.DebugApi.collect_logs)
        )

        results["session.factory.run"] = await session.rpc.factory.run(
            _request_for(rpc.FactoryApi.run)
        )
        results["session.factory.resume"] = await session.rpc.factory.resume(
            _request_for(rpc.FactoryApi.resume)
        )
        results["session.factory.getRun"] = await session.rpc.factory.get_run(
            _request_for(rpc.FactoryApi.get_run)
        )
        results["session.factory.listRuns"] = await session.rpc.factory.list_runs(
            _request_for(rpc.FactoryApi.list_runs)
        )
        results["session.factory.getRunDetail"] = await session.rpc.factory.get_run_detail(
            _request_for(rpc.FactoryApi.get_run_detail)
        )
        results["session.factory.getRunProgress"] = await session.rpc.factory.get_run_progress(
            _request_for(rpc.FactoryApi.get_run_progress)
        )
        results["session.factory.cancel"] = await session.rpc.factory.cancel(
            _request_for(rpc.FactoryApi.cancel)
        )
        results["session.factory.pause"] = await session.rpc.factory.pause(
            _request_for(rpc.FactoryApi.pause)
        )
        results["session.factory.log"] = await session.rpc.factory.log(
            _request_for(rpc.FactoryApi.log)
        )
        results["session.factory.agent"] = await session.rpc.factory.agent(
            _request_for(rpc.FactoryApi.agent)
        )
        results["session.factory.journal.get"] = await session.rpc.factory.journal.get(
            _request_for(rpc.FactoryJournalApi.get)
        )
        results["session.factory.journal.put"] = await session.rpc.factory.journal.put(
            _request_for(rpc.FactoryJournalApi.put)
        )

        results["session.model.switchAutoTier"] = await session.rpc.model.switch_auto_tier(
            _request_for(rpc.ModelApi.switch_auto_tier)
        )
        results["session.model.setAllowedModels"] = await session.rpc.model.set_allowed_models(
            _request_for(rpc.ModelApi.set_allowed_models)
        )

        results["session.workspaces.updateMetadata"] = await session.rpc.workspaces.update_metadata(
            _request_for(rpc.WorkspacesApi.update_metadata)
        )
        results["session.workspaces.ensure"] = await session.rpc.workspaces.ensure(
            _request_for(rpc.WorkspacesApi.ensure)
        )
        results["session.workspaces.statFile"] = await session.rpc.workspaces.stat_file(
            _request_for(rpc.WorkspacesApi.stat_file)
        )
        results[
            "session.workspaces.createDirectory"
        ] = await session.rpc.workspaces.create_directory(
            _request_for(rpc.WorkspacesApi.create_directory)
        )
        results["session.workspaces.removePath"] = await session.rpc.workspaces.remove_path(
            _request_for(rpc.WorkspacesApi.remove_path)
        )
        results["session.workspaces.renamePath"] = await session.rpc.workspaces.rename_path(
            _request_for(rpc.WorkspacesApi.rename_path)
        )
        results["session.workspaces.addSummary"] = await session.rpc.workspaces.add_summary(
            _request_for(rpc.WorkspacesApi.add_summary)
        )
        results[
            "session.workspaces.truncateSummaries"
        ] = await session.rpc.workspaces.truncate_summaries(
            _request_for(rpc.WorkspacesApi.truncate_summaries)
        )

        initial_objective = await session.rpc.workspaces.read_autopilot_objective()
        assert initial_objective.content is None
        initial_exists = await session.rpc.workspaces.autopilot_objective_exists()
        assert initial_exists.exists is False

        objective_request = _request_for(rpc.WorkspacesApi.write_autopilot_objective)
        objective_request.content = "# Deterministic objective\n\nCover generated RPC methods."
        results[
            "session.workspaces.writeAutopilotObjective"
        ] = await session.rpc.workspaces.write_autopilot_objective(objective_request)
        assert results["session.workspaces.writeAutopilotObjective"].operation == "created"

        saved_objective = await session.rpc.workspaces.read_autopilot_objective()
        assert saved_objective.content == objective_request.content
        saved_exists = await session.rpc.workspaces.autopilot_objective_exists()
        assert saved_exists.exists is True
        results["session.workspaces.readAutopilotObjective"] = saved_objective
        results["session.workspaces.autopilotObjectiveExists"] = saved_exists

        results[
            "session.workspaces.deleteAutopilotObjective"
        ] = await session.rpc.workspaces.delete_autopilot_objective()
        assert results["session.workspaces.deleteAutopilotObjective"].deleted is True
        assert (await session.rpc.workspaces.read_autopilot_objective()).content is None

        results[
            "session.autopilotObjective.getState"
        ] = await session.rpc.autopilot_objective.get_state()
        results["session.agent.setPrompt"] = await session.rpc.agent.set_prompt(
            _request_for(rpc.AgentApi.set_prompt)
        )
        results["session.tasks.register"] = await session.rpc.tasks.register(
            _request_for(rpc.TasksApi.register)
        )
        results["session.tasks.update"] = await session.rpc.tasks.update(
            _request_for(rpc.TasksApi.update)
        )

        results[
            "session.mcp.moveLoadingToBackground"
        ] = await session.rpc.mcp.move_loading_to_background()
        results["session.mcp.startServer"] = await session.rpc.mcp.start_server(
            _request_for(rpc.McpApi.start_server)
        )
        results["session.mcp.restartServer"] = await session.rpc.mcp.restart_server(
            _request_for(rpc.McpApi.restart_server)
        )
        results[
            "session.mcp.oauth.authenticationStateChanged"
        ] = await session.rpc.mcp.oauth.authentication_state_changed(
            _request_for(rpc.McpOauthApi.authentication_state_changed)
        )
        results["session.mcp.oauth.probe"] = await session.rpc.mcp.oauth.probe(
            _request_for(rpc.McpOauthApi.probe)
        )
        results["session.mcp.oauth.respond"] = await session.rpc.mcp.oauth.respond(
            _request_for(rpc.McpOauthApi.respond)
        )
        results["session.mcp.resources.read"] = await session.rpc.mcp.resources.read(
            _request_for(rpc.McpResourcesApi.read)
        )
        results["session.mcp.resources.list"] = await session.rpc.mcp.resources.list(
            _request_for(rpc.McpResourcesApi.list)
        )
        results[
            "session.mcp.resources.listTemplates"
        ] = await session.rpc.mcp.resources.list_templates(
            _request_for(rpc.McpResourcesApi.list_templates)
        )

        results["session.tools.execute"] = await session.rpc.tools.execute(
            _request_for(rpc.ToolsApi.execute)
        )
        results[
            "session.tools.getBuiltinDescriptors"
        ] = await session.rpc.tools.get_builtin_descriptors(
            _request_for(rpc.ToolsApi.get_builtin_descriptors)
        )
        results[
            "session.tools.taskCompleteEventData"
        ] = await session.rpc.tools.task_complete_event_data(
            _request_for(rpc.ToolsApi.task_complete_event_data)
        )
        results["session.tools.set"] = await session.rpc.tools.set(_request_for(rpc.ToolsApi.set))

        results["session.permissions.configure"] = await session.rpc.permissions.configure(
            _request_for(rpc.PermissionsApi.configure)
        )
        results[
            "session.permissions.pendingRequests"
        ] = await session.rpc.permissions.pending_requests()
        results["session.permissions.modifyRules"] = await session.rpc.permissions.modify_rules(
            _request_for(rpc.PermissionsApi.modify_rules)
        )
        results["session.permissions.setRequired"] = await session.rpc.permissions.set_required(
            _request_for(rpc.PermissionsApi.set_required)
        )
        results[
            "session.permissions.notifyPromptShown"
        ] = await session.rpc.permissions.notify_prompt_shown(
            _request_for(rpc.PermissionsApi.notify_prompt_shown)
        )
        results["session.permissions.paths.list"] = await session.rpc.permissions.paths.list()
        results["session.permissions.paths.add"] = await session.rpc.permissions.paths.add(
            _request_for(rpc.PermissionsPathsApi.add)
        )
        results[
            "session.permissions.paths.updatePrimary"
        ] = await session.rpc.permissions.paths.update_primary(
            _request_for(rpc.PermissionsPathsApi.update_primary)
        )
        results[
            "session.permissions.paths.isPathWithinAllowedDirectories"
        ] = await session.rpc.permissions.paths.is_path_within_allowed_directories(
            _request_for(rpc.PermissionsPathsApi.is_path_within_allowed_directories)
        )
        results[
            "session.permissions.paths.isPathWithinWorkspace"
        ] = await session.rpc.permissions.paths.is_path_within_workspace(
            _request_for(rpc.PermissionsPathsApi.is_path_within_workspace)
        )
        results[
            "session.permissions.locations.resolve"
        ] = await session.rpc.permissions.locations.resolve(
            _request_for(rpc.PermissionsLocationsApi.resolve)
        )
        results[
            "session.permissions.locations.apply"
        ] = await session.rpc.permissions.locations.apply(
            _request_for(rpc.PermissionsLocationsApi.apply)
        )
        results[
            "session.permissions.locations.addToolApproval"
        ] = await session.rpc.permissions.locations.add_tool_approval(
            _request_for(rpc.PermissionsLocationsApi.add_tool_approval)
        )
        results[
            "session.permissions.folderTrust.isTrusted"
        ] = await session.rpc.permissions.folder_trust.is_trusted(
            _request_for(rpc.PermissionsFolderTrustApi.is_trusted)
        )
        results[
            "session.permissions.folderTrust.addTrusted"
        ] = await session.rpc.permissions.folder_trust.add_trusted(
            _request_for(rpc.PermissionsFolderTrustApi.add_trusted)
        )
        results[
            "session.permissions.urls.setUnrestrictedMode"
        ] = await session.rpc.permissions.urls.set_unrestricted_mode(
            _request_for(rpc.PermissionsUrlsApi.set_unrestricted_mode)
        )

        results[
            "session.metadata.getClientMetadata"
        ] = await session.rpc.metadata.get_client_metadata()
        results[
            "session.metadata.updateClientMetadata"
        ] = await session.rpc.metadata.update_client_metadata(
            _request_for(rpc.MetadataApi.update_client_metadata)
        )
        results[
            "session.contentExclusion.checkPaths"
        ] = await session.rpc.content_exclusion.check_paths(
            _request_for(rpc.ContentExclusionApi.check_paths)
        )
        results["session.history.clearContext"] = await session.rpc.history.clear_context(
            _request_for(rpc.HistoryApi.clear_context)
        )

        results["session.queue.moveItem"] = await session.rpc.queue.move_item(
            _request_for(rpc.QueueApi.move_item)
        )
        results["session.queue.insertAt"] = await session.rpc.queue.insert_at(
            _request_for(rpc.QueueApi.insert_at)
        )
        results["session.queue.removeAt"] = await session.rpc.queue.remove_at(
            _request_for(rpc.QueueApi.remove_at)
        )
        results["session.queue.updateText"] = await session.rpc.queue.update_text(
            _request_for(rpc.QueueApi.update_text)
        )
        results["session.queue.duplicateAt"] = await session.rpc.queue.duplicate_at(
            _request_for(rpc.QueueApi.duplicate_at)
        )
        results["session.queue.setDrainPaused"] = await session.rpc.queue.set_drain_paused(
            _request_for(rpc.QueueApi.set_drain_paused)
        )
        results["session.queue.sendNow"] = await session.rpc.queue.send_now(
            _request_for(rpc.QueueApi.send_now)
        )
        results["session.limitPrediction.predict"] = await session.rpc.limit_prediction.predict(
            _request_for(rpc.LimitPredictionApi.predict)
        )

        category_requests = [
            rpc.CatalogSearchRequest(
                contract=rpc.CatalogClientContract(
                    protocol_version=3,
                    required_capabilities=["catalog-search"],
                ),
                query="all candidates",
                kinds=None,
                limit=5,
            ),
            rpc.CatalogSearchRequest(
                contract=rpc.CatalogClientContract(
                    protocol_version=3,
                    required_capabilities=["catalog-search"],
                ),
                query="MCP candidates",
                kinds=[rpc.CatalogCandidateKind.MCP_SERVER],
                limit=5,
            ),
            rpc.CatalogSearchRequest(
                contract=rpc.CatalogClientContract(
                    protocol_version=3,
                    required_capabilities=["catalog-search"],
                ),
                query="skill candidates",
                kinds=[rpc.CatalogCandidateKind.AI_SKILL],
                limit=5,
            ),
        ]
        for category_request in category_requests:
            category_result = await client.rpc.catalog.search(category_request)
            assert isinstance(category_result, rpc.CatalogSearchSucceeded)
            assert category_result.search_id == "sample-value"

        error_request = _request_for(rpc.ServerCatalogApi.search)
        error_request.query = "raise-jsonrpc-error"
        with pytest.raises(JsonRpcError) as exc_info:
            await client.rpc.catalog.search(error_request)
        assert exc_info.value.code == -32077
        assert exc_info.value.message == "deterministic catalog failure"
        assert exc_info.value.data == {"retryable": False, "source": "fake-cli"}

        for rpc_method, payload in responses.items():
            if rpc_method not in _OBJECTIVE_METHODS:
                _assert_result_matches_payload(results[rpc_method], payload)

        planned = results["mcp.planInstall"]
        assert isinstance(planned, rpc.MCPPlanInstallPlanned)
        assert planned.plan.transport_choices[0].transport
        assert planned.plan.transport_choices[0].required_values[0].key == "sample-value"

        catalog = results["catalog.search"]
        assert isinstance(catalog, rpc.CatalogSearchSucceeded)
        assert catalog.candidates[0].installability.value
        assert catalog.candidates[0].provenance.authority == "sample-value"

        factory_detail = results["session.factory.getRunDetail"]
        assert factory_detail.consumed.active_ms == 7
        assert factory_detail.agents[0].agent_id == "sample-value"
        assert factory_detail.progress.records[0].seq == 7

        permission_requests = results["session.permissions.pendingRequests"]
        assert len(permission_requests.items) == 1
        assert permission_requests.items[0].request_id == "sample-value"
        assert isinstance(
            permission_requests.items[0].request,
            generated_session_events.PermissionPromptRequestCommands,
        )
        assert permission_requests.items[0].request.kind == "commands"
        assert permission_requests.items[0].request.command_identifiers == ["sample-value"]

        mcp_resources = results["session.mcp.resources.read"]
        assert mcp_resources.contents[0].uri == "sample-value"
        assert mcp_resources.contents[0].mime_type == "sample-value"

        tool_result = typing.cast(dict[str, typing.Any], results["session.tools.execute"])
        assert tool_result["resultType"] == "denied"
        assert tool_result["binaryResultsForLlm"][0]["metadata"]["key"]["sample"] == "value"
        assert tool_result["taskCompletionDecision"]["reviewerResultMeta"]["sample"] == "value"

        content_checks = results["session.contentExclusion.checkPaths"]
        assert content_checks.available is True
        assert content_checks.checks[0].excluded is True

        _assert_request_serialization(capture_path)
        captured_catalog_params = [
            request["params"]
            for request in json.loads(capture_path.read_text(encoding="utf-8"))["requests"]
            if request["method"] == "catalog.search"
        ]
        for category_request in category_requests:
            assert category_request.to_dict() in captured_catalog_params
    finally:
        await client.force_stop()
        cli_path.unlink(missing_ok=True)
        capture_path.unlink(missing_ok=True)
        responses_path.unlink(missing_ok=True)
