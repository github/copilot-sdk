import { createHash } from "node:crypto";

export type DelegateLifecycleState = "queued" | "running" | "completed" | "failed" | "cancelled";

export interface DelegateLifecycleEvent {
    correlationId: string;
    state: DelegateLifecycleState;
    runId?: number;
    runUrl?: string;
}

export interface GitHubActionsTarget {
    repository: string;
    workflow: string;
    allowedRefs: readonly string[];
    allowedAgentTypes: readonly string[];
}

export interface GitHubActionsClientOptions {
    token: () => string | Promise<string>;
    targets: readonly GitHubActionsTarget[];
    apiUrl?: string;
    pollIntervalMs?: number;
    timeoutMs?: number;
    onLifecycle?: (event: DelegateLifecycleEvent) => void;
    fetch?: typeof fetch;
    sleep?: (milliseconds: number, signal?: AbortSignal) => Promise<void>;
}

export interface DelegateRequest {
    task: string;
    repository: string;
    ref: string;
    agentType: string;
    expectedOutput: string;
}

export interface DelegateInvocation {
    sessionId: string;
    toolCallId: string;
    signal?: AbortSignal;
}

export interface DelegateHandle {
    repository: string;
    workflow: string;
    ref: string;
    correlationId: string;
    outputBranch: string;
}

export interface DelegateResult {
    correlationId: string;
    runId: number;
    runUrl: string;
    conclusion: string;
    outputBranch: string;
    headSha: string;
    artifacts: Array<{
        id: number;
        name: string;
        downloadUrl: string;
    }>;
    pullRequests: Array<{
        number: number;
        url: string;
        state: string;
    }>;
}

interface WorkflowRun {
    id: number;
    status: string;
    conclusion: string | null;
    html_url: string;
    display_title: string;
    head_sha: string;
}

interface WorkflowRunsResponse {
    workflow_runs: WorkflowRun[];
}

interface ArtifactsResponse {
    artifacts: Array<{
        id: number;
        name: string;
        archive_download_url: string;
        expired: boolean;
    }>;
}

interface PullRequestResponse {
    number: number;
    html_url: string;
    state: string;
}

const REPOSITORY_PATTERN = /^[A-Za-z0-9_.-]+\/[A-Za-z0-9_.-]+$/;
const REF_PATTERN = /^[A-Za-z0-9][A-Za-z0-9._/-]{0,254}$/;
const AGENT_TYPE_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;
const MAX_TASK_LENGTH = 12_000;
const MAX_EXPECTED_OUTPUT_LENGTH = 1_000;

function abortError(): Error {
    const error = new Error("GitHub Actions delegation was cancelled.");
    error.name = "AbortError";
    return error;
}

function timeoutError(correlationId: string): Error {
    const error = new Error(`Timed out waiting for GitHub Actions run ${correlationId}.`);
    error.name = "TimeoutError";
    return error;
}

function defaultSleep(milliseconds: number, signal?: AbortSignal): Promise<void> {
    return new Promise((resolve, reject) => {
        if (signal?.aborted) {
            reject(abortError());
            return;
        }

        const onAbort = () => {
            clearTimeout(timer);
            reject(abortError());
        };
        const timer = setTimeout(() => {
            signal?.removeEventListener("abort", onAbort);
            resolve();
        }, milliseconds);
        signal?.addEventListener("abort", onAbort, { once: true });
    });
}

function validateRequest(request: DelegateRequest, target: GitHubActionsTarget | undefined): void {
    if (!REPOSITORY_PATTERN.test(request.repository) || !target) {
        throw new Error(`Repository "${request.repository}" is not allowlisted.`);
    }
    if (!REF_PATTERN.test(request.ref) || !target.allowedRefs.includes(request.ref)) {
        throw new Error(`Ref "${request.ref}" is not allowlisted for ${request.repository}.`);
    }
    if (
        !AGENT_TYPE_PATTERN.test(request.agentType) ||
        !target.allowedAgentTypes.includes(request.agentType)
    ) {
        throw new Error(
            `Agent type "${request.agentType}" is not allowlisted for ${request.repository}.`
        );
    }
    if (request.task.trim().length === 0 || request.task.length > MAX_TASK_LENGTH) {
        throw new Error(`Task must contain between 1 and ${MAX_TASK_LENGTH} characters.`);
    }
    if (
        request.expectedOutput.trim().length === 0 ||
        request.expectedOutput.length > MAX_EXPECTED_OUTPUT_LENGTH
    ) {
        throw new Error(
            `Expected output must contain between 1 and ${MAX_EXPECTED_OUTPUT_LENGTH} characters.`
        );
    }
}

export class GitHubActionsClient {
    private readonly apiUrl: string;
    private readonly fetchImplementation: typeof fetch;
    private readonly pollIntervalMs: number;
    private readonly timeoutMs: number;
    private readonly sleep: (milliseconds: number, signal?: AbortSignal) => Promise<void>;
    private readonly targets = new Map<string, GitHubActionsTarget>();

    public constructor(private readonly options: GitHubActionsClientOptions) {
        this.apiUrl = (options.apiUrl ?? "https://api.github.com").replace(/\/$/, "");
        this.fetchImplementation = options.fetch ?? fetch;
        this.pollIntervalMs = options.pollIntervalMs ?? 5_000;
        this.timeoutMs = options.timeoutMs ?? 30 * 60_000;
        this.sleep = options.sleep ?? defaultSleep;

        for (const target of options.targets) {
            if (!REPOSITORY_PATTERN.test(target.repository)) {
                throw new Error(`Invalid target repository "${target.repository}".`);
            }
            if (this.targets.has(target.repository)) {
                throw new Error(`Duplicate target repository "${target.repository}".`);
            }
            if (target.allowedRefs.length === 0 || target.allowedAgentTypes.length === 0) {
                throw new Error(`Target "${target.repository}" must declare refs and agent types.`);
            }
            this.targets.set(target.repository, target);
        }
    }

    public async dispatch(
        request: DelegateRequest,
        invocation: DelegateInvocation
    ): Promise<DelegateHandle> {
        const target = this.targets.get(request.repository);
        validateRequest(request, target);
        if (invocation.signal?.aborted) {
            throw abortError();
        }

        const correlationId = createHash("sha256")
            .update(`${invocation.sessionId}\0${invocation.toolCallId}`)
            .digest("hex")
            .slice(0, 24);
        const handle: DelegateHandle = {
            repository: request.repository,
            workflow: target!.workflow,
            ref: request.ref,
            correlationId,
            outputBranch: `agent/${correlationId}`,
        };

        const existingRun = await this.findRun(handle, invocation.signal);
        if (!existingRun) {
            await this.request(
                handle.repository,
                `/actions/workflows/${encodeURIComponent(handle.workflow)}/dispatches`,
                {
                    method: "POST",
                    body: JSON.stringify({
                        ref: request.ref,
                        inputs: {
                            task: request.task,
                            repository: request.repository,
                            ref: request.ref,
                            agent_type: request.agentType,
                            expected_output: request.expectedOutput,
                            correlation_id: correlationId,
                            output_branch: handle.outputBranch,
                        },
                    }),
                    signal: invocation.signal,
                }
            );
        }

        this.emit({
            correlationId,
            state: "queued",
            runId: existingRun?.id,
            runUrl: existingRun?.html_url,
        });
        return handle;
    }

    public async wait(handle: DelegateHandle, signal?: AbortSignal): Promise<DelegateResult> {
        const deadline = Date.now() + this.timeoutMs;
        let lastState: DelegateLifecycleState = "queued";

        while (Date.now() < deadline) {
            if (signal?.aborted) {
                throw abortError();
            }

            const run = await this.findRun(handle, signal);
            if (!run) {
                await this.sleep(this.pollIntervalMs, signal);
                continue;
            }

            if (run.status !== "completed") {
                if (lastState !== "running") {
                    lastState = "running";
                    this.emit({
                        correlationId: handle.correlationId,
                        state: "running",
                        runId: run.id,
                        runUrl: run.html_url,
                    });
                }
                await this.sleep(this.pollIntervalMs, signal);
                continue;
            }

            const state = this.terminalState(run.conclusion);
            this.emit({
                correlationId: handle.correlationId,
                state,
                runId: run.id,
                runUrl: run.html_url,
            });
            return this.createResult(handle, run, signal);
        }

        throw timeoutError(handle.correlationId);
    }

    public async execute(
        request: DelegateRequest,
        invocation: DelegateInvocation
    ): Promise<DelegateResult> {
        const handle = await this.dispatch(request, invocation);
        try {
            return await this.wait(handle, invocation.signal);
        } catch (error) {
            if (
                invocation.signal?.aborted ||
                (error instanceof Error && error.name === "TimeoutError")
            ) {
                await this.cancel(handle);
            }
            throw error;
        }
    }

    public async cancel(handle: DelegateHandle): Promise<void> {
        const run = await this.findRun(handle);
        if (run?.status === "completed") {
            this.emit({
                correlationId: handle.correlationId,
                state: this.terminalState(run.conclusion),
                runId: run.id,
                runUrl: run.html_url,
            });
            return;
        }
        if (run) {
            await this.request(handle.repository, `/actions/runs/${run.id}/cancel`, {
                method: "POST",
            });
        }
        this.emit({
            correlationId: handle.correlationId,
            state: "cancelled",
            runId: run?.id,
            runUrl: run?.html_url,
        });
    }

    private async createResult(
        handle: DelegateHandle,
        run: WorkflowRun,
        signal?: AbortSignal
    ): Promise<DelegateResult> {
        const [artifactsResponse, pullRequests] = await Promise.all([
            this.requestJson<ArtifactsResponse>(
                handle.repository,
                `/actions/runs/${run.id}/artifacts`,
                { signal }
            ),
            this.requestJson<PullRequestResponse[]>(
                handle.repository,
                `/pulls?state=all&head=${encodeURIComponent(
                    `${handle.repository.split("/")[0]}:${handle.outputBranch}`
                )}`,
                { signal }
            ),
        ]);

        return {
            correlationId: handle.correlationId,
            runId: run.id,
            runUrl: run.html_url,
            conclusion: run.conclusion ?? "unknown",
            outputBranch: handle.outputBranch,
            headSha: run.head_sha,
            artifacts: artifactsResponse.artifacts
                .filter((artifact) => !artifact.expired)
                .map((artifact) => ({
                    id: artifact.id,
                    name: artifact.name,
                    downloadUrl: artifact.archive_download_url,
                })),
            pullRequests: pullRequests.map((pullRequest) => ({
                number: pullRequest.number,
                url: pullRequest.html_url,
                state: pullRequest.state,
            })),
        };
    }

    private async findRun(
        handle: DelegateHandle,
        signal?: AbortSignal
    ): Promise<WorkflowRun | undefined> {
        const response = await this.requestJson<WorkflowRunsResponse>(
            handle.repository,
            `/actions/workflows/${encodeURIComponent(
                handle.workflow
            )}/runs?event=workflow_dispatch&per_page=100`,
            { signal }
        );
        const expectedTitle = `SDK delegate ${handle.correlationId}`;
        return response.workflow_runs.find((run) => run.display_title === expectedTitle);
    }

    private terminalState(conclusion: string | null): DelegateLifecycleState {
        if (conclusion === "success") {
            return "completed";
        }
        if (conclusion === "cancelled") {
            return "cancelled";
        }
        return "failed";
    }

    private emit(event: DelegateLifecycleEvent): void {
        this.options.onLifecycle?.(event);
    }

    private async requestJson<T>(
        repository: string,
        path: string,
        init: RequestInit = {}
    ): Promise<T> {
        const response = await this.request(repository, path, init);
        return (await response.json()) as T;
    }

    private async request(
        repository: string,
        path: string,
        init: RequestInit = {}
    ): Promise<Response> {
        const token = await this.options.token();
        if (!token) {
            throw new Error("GitHub token provider returned an empty token.");
        }
        const response = await this.fetchImplementation(
            `${this.apiUrl}/repos/${repository}${path}`,
            {
                ...init,
                headers: {
                    Accept: "application/vnd.github+json",
                    Authorization: ["Bearer", token].join(" "),
                    "Content-Type": "application/json",
                    "X-GitHub-Api-Version": "2022-11-28",
                    ...init.headers,
                },
            }
        );
        if (!response.ok) {
            throw new Error(
                `GitHub API request failed (${response.status} ${response.statusText}).`
            );
        }
        return response;
    }
}
