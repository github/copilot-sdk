/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: api.schema.json

package com.github.copilot.generated.rpc;

import com.github.copilot.CopilotExperimental;
import java.util.concurrent.CompletableFuture;
import javax.annotation.processing.Generated;

/**
 * API methods for the {@code cloudTasks} namespace.
 *
 * @since 1.0.0
 */
@javax.annotation.processing.Generated("copilot-sdk-codegen")
final class ServerCloudTasksApi {

    private final RpcCaller caller;

    /** @param caller the RPC transport function */
    ServerCloudTasksApi(RpcCaller caller) {
        this.caller = caller;
    }

    /**
     * Starts a cloud coding-agent task, the remote run whose record `cloudTasks.get` later reads. Answered above any session because the task it creates is not a background task inside one. Hosts other than the CLI want this: a GitHub App or an SDK consumer that hands work to a cloud agent creates the same task under the same credential, and resolving it here keeps one credential, endpoint and validation path. Target the task either by repository, supplying both `repositoryOwner` and `repositoryName`, or by `owner` alone for an account-level run. The result separates a rejected request from a failed one: when `ok` is false, `reason` categorises the rejection and `message` carries the explanation to show, while `failure` describes the HTTP call itself and is present only when one was actually made.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CloudTasksCreateResult> create(CloudTasksCreateParams params) {
        return caller.invoke("cloudTasks.create", params, CloudTasksCreateResult.class);
    }

    /**
     * Deletes one cloud coding-agent task, the remote run `cloudTasks.create` started. The deletion is a soft one: the service withdraws the task from its listings rather than destroying the record, so a host removing a run from its own UI is the intended caller. Answered above any session for the same reason as `cloudTasks.get` -- the task is remote and has no local counterpart. Hosts other than the CLI want this: a GitHub App or an SDK consumer that manages cloud agent runs withdraws the same record under the same credential, and resolving it here keeps one credential-resolution and endpoint path. The runtime resolves each token from `authInfo` or `authContextId`. A host may preserve an explicit `baseUrl`; omission derives the endpoint from `authInfo`. The result is the HTTP outcome rather than a bare success flag: the call returns no body, so `ok` alone describes a success, while a failure is reported through `statusCode`, `errorMessage` and `errorPayloadJson` instead of being raised as an error.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CloudTasksDeleteResult> delete(CloudTasksDeleteParams params) {
        return caller.invoke("cloudTasks.delete", params, CloudTasksDeleteResult.class);
    }

    /**
     * Reads one cloud coding-agent task from the Copilot API. This is the task a cloud agent runs on a host's behalf, not a background task inside a session, so it is answered above any session and the `tasks` group does not cover it. Hosts other than the CLI want this: a GitHub App or an SDK consumer that shows the state of a cloud agent run must read the same task record, and resolving it here keeps one credential-resolution, task-URL and circuit-breaker path instead of each host rebuilding them. The result is the HTTP outcome rather than the task itself: a caller must read `ok` and `statusCode` before `payloadJson`, because a 404 is reported as a non-ok result rather than an error, which is what lets a caller tell "no such task" apart from "the API call failed".
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CloudTasksGetResult> get(CloudTasksGetParams params) {
        return caller.invoke("cloudTasks.get", params, CloudTasksGetResult.class);
    }

    /**
     * Reads the logs a cloud coding-agent session produced, from the Copilot API. Answered above any session, for the same reason as `cloudTasks.get`: the session being asked about runs remotely and has no local counterpart. Read `resolved` first. It is false when no Copilot API URL could be resolved for the credential, which is a different outcome from a request that was made and failed, and a caller that treats the two alike will report a transport error for an account that simply has no API endpoint. When `resolved` is true, `debugMessages` carries the diagnostics the fetch produced and should be logged whatever the outcome, `errorString` reports a retrieval fault, and only then do `ok` and `statusCode` describe the HTTP result.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CloudTasksGetSessionLogsResult> getSessionLogs(CloudTasksGetSessionLogsParams params) {
        return caller.invoke("cloudTasks.getSessionLogs", params, CloudTasksGetSessionLogsResult.class);
    }

    /**
     * Reconstructs the session a cloud coding-agent task recorded, by reading that task's events and ordering them for replay. Answered above any session, for the same reason as `cloudTasks.get`: the session being rebuilt ran remotely and has no local counterpart. Hosts other than the CLI want this: an SDK consumer attaching to a run in progress, an ACP client resuming one, or a GitHub App rendering a task's transcript all rebuild the same history from the same events, and resolving it here keeps one credential, endpoint, ordering and anchor-detection path. A newly created task has no events yet, so this polls for them and fails once they do not arrive. Read `preStart` before either list: when it is true the session has not started, `replayEventsJson` is deliberately empty because there is no history to reconstruct, and only `observedEventsJson` is meaningful.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CloudTasksGetSessionReplayResult> getSessionReplay(CloudTasksGetSessionReplayParams params) {
        return caller.invoke("cloudTasks.getSessionReplay", params, CloudTasksGetSessionReplayResult.class);
    }

    /**
     * Reads whether one cloud coding-agent task is shared, from the Mission Control service. Answered above any session, for the same reason as `cloudTasks.get`: the task is a remote record with no local counterpart. Hosts other than the CLI want this: an SDK consumer or a GitHub App that offers a "share this run" affordance must read the same flag from the same service, and resolving it here keeps one credential-resolution and endpoint path. The credential is resolved from `authInfo` inside the runtime rather than being carried across the door, so no caller handles a raw token. Read `credentialResolved` first: it is false when the account has no usable credential, in which case no request was made at all, which a caller must not report as a failed request.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CloudTasksSharingStatusResult> getSharingStatus(CloudTasksGetSharingStatusParams params) {
        return caller.invoke("cloudTasks.getSharingStatus", params, CloudTasksSharingStatusResult.class);
    }

    /**
     * Changes whether one cloud coding-agent task is shared, through the Mission Control service, and reports the status the task carries afterwards. The companion to `cloudTasks.getSharingStatus` and answered above any session for the same reason. The status is sent verbatim and is not narrowed to a closed set, because the service owns these values. The credential is resolved from `authInfo` inside the runtime rather than being carried across the door. Read `credentialResolved` first: it is false when the account has no usable credential, in which case nothing was changed and no request was made.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CloudTasksSharingStatusResult> setSharingStatus(CloudTasksSetSharingStatusParams params) {
        return caller.invoke("cloudTasks.setSharingStatus", params, CloudTasksSharingStatusResult.class);
    }

    /**
     * Finds the session a caller should open for one cloud coding-agent task. A task identifier alone does not say what to open: the run may already exist locally, it may exist only in the cloud, or it may be a task that cannot be resumed at all. Resolving that needs a local session scan and a Mission Control lookup that agree on identity, which is why it is answered here rather than left to each host. Hosts other than the CLI want it: an IDE or a GitHub App that opens a run from a task link faces the same three outcomes. Read `kind` before any other field. `local-session` carries `sessionId`, `remote-session` carries `metadataJson`, `unresumable-task` means the task exists but cannot be continued, and `none` means no session matched. A failed service call is reported as `error` rather than raised, so a caller can surface the service's own message. Pass every local session identifier already rejected in `ignoredLocalSessionIds` and call again to get the next candidate; that is how a host skips a local session it could not load.
     *
     * @apiNote This method is experimental and may change in a future version.
     * @since 1.0.0
     */
    @CopilotExperimental
    CompletableFuture<CloudTasksResolveTaskIdResult> resolveTaskId(CloudTasksResolveTaskIdParams params) {
        return caller.invoke("cloudTasks.resolveTaskId", params, CloudTasksResolveTaskIdResult.class);
    }

}
