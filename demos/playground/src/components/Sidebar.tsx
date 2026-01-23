/**
 * Sidebar component - Session management
 *
 * Contains the new session form and list of existing sessions.
 */

import { useState } from "react";
import type { CreateSessionOptions, ProviderConfig, Session } from "../types";
import { SessionItem } from "./SessionItem";

/** Available models in the dropdown */
const MODELS = [
    "claude-sonnet-4.5",
    "gpt-5",
    "gpt-5.1",
    "gpt-5.2",
    "gpt-5-mini",
    "gpt-4.1",
    "claude-sonnet-4",
    "claude-opus-4.5",
    "gemini-3-pro-preview",
];

interface SidebarProps {
    sessions: Session[];
    activeSessionId: string | null;
    onCreateSession: (options: CreateSessionOptions) => Promise<string>;
    onSelectSession: (sessionId: string) => void;
    onDestroySession: (sessionId: string) => Promise<void>;
}

export function Sidebar({
    sessions,
    activeSessionId,
    onCreateSession,
    onSelectSession,
    onDestroySession,
}: SidebarProps) {
    // Form state
    const [model, setModel] = useState(MODELS[0]);
    const [isCreating, setIsCreating] = useState(false);

    // Provider config state
    const [providerType, setProviderType] = useState<ProviderConfig["type"] | "">("");
    const [providerBaseUrl, setProviderBaseUrl] = useState("");
    const [providerModel, setProviderModel] = useState("");
    const [providerApiKey, setProviderApiKey] = useState("");
    const [providerBearerToken, setProviderBearerToken] = useState("");
    const [providerWireApi, setProviderWireApi] = useState<"completions" | "responses">(
        "completions"
    );
    const [azureApiVersion, setAzureApiVersion] = useState("");

    // System prompt state
    const [systemPromptMode, setSystemPromptMode] = useState<"append" | "replace">("append");
    const [systemPromptContent, setSystemPromptContent] = useState("");

    const handleCreateSession = async () => {
        setIsCreating(true);
        try {
            const options: CreateSessionOptions = {
                model: providerType && providerModel ? providerModel : model,
            };

            // Add provider config if configured
            if (providerType && providerBaseUrl) {
                options.provider = {
                    type: providerType,
                    baseUrl: providerBaseUrl,
                };
                if (providerApiKey) {
                    options.provider.apiKey = providerApiKey;
                }
                if (providerBearerToken) {
                    options.provider.bearerToken = providerBearerToken;
                }
                if (providerWireApi && providerType !== "anthropic") {
                    options.provider.wireApi = providerWireApi;
                }
                if (azureApiVersion && providerType === "azure") {
                    options.provider.azure = { apiVersion: azureApiVersion };
                }
            }

            // Add system prompt if configured
            if (systemPromptContent.trim()) {
                options.systemMessage = {
                    mode: systemPromptMode,
                    content: systemPromptContent,
                };
            }

            await onCreateSession(options);

            // Clear form after successful creation
            setProviderType("");
            setProviderBaseUrl("");
            setProviderModel("");
            setProviderApiKey("");
            setProviderBearerToken("");
            setAzureApiVersion("");
            setSystemPromptContent("");
        } finally {
            setIsCreating(false);
        }
    };

    return (
        <aside className="sidebar">
            {/* New Session Form */}
            <div className="sidebar-section">
                <h2>New Session</h2>

                <div className="input-group">
                    <select
                        value={model}
                        onChange={(e) => setModel(e.target.value)}
                        style={{ flex: 1 }}
                    >
                        {MODELS.map((m) => (
                            <option key={m} value={m}>
                                {m}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Provider Config */}
                <details className="advanced-options">
                    <summary>Provider Config</summary>
                    <div className="advanced-options-content">
                        <select
                            value={providerType}
                            onChange={(e) =>
                                setProviderType(e.target.value as ProviderConfig["type"] | "")
                            }
                            style={{ width: "100%" }}
                        >
                            <option value="">Default (Copilot API)</option>
                            <option value="openai">OpenAI</option>
                            <option value="azure">Azure OpenAI</option>
                            <option value="anthropic">Anthropic</option>
                        </select>

                        <input
                            type="text"
                            placeholder="Model name (e.g., gpt-4o)"
                            value={providerModel}
                            onChange={(e) => setProviderModel(e.target.value)}
                            style={{ width: "100%" }}
                        />

                        <input
                            type="text"
                            placeholder="Base URL (e.g., https://api.openai.com/v1)"
                            value={providerBaseUrl}
                            onChange={(e) => setProviderBaseUrl(e.target.value)}
                            style={{ width: "100%" }}
                        />

                        <input
                            type="password"
                            placeholder="API Key"
                            value={providerApiKey}
                            onChange={(e) => setProviderApiKey(e.target.value)}
                            style={{ width: "100%" }}
                        />

                        <input
                            type="password"
                            placeholder="Bearer Token (for AAD/token auth)"
                            value={providerBearerToken}
                            onChange={(e) => setProviderBearerToken(e.target.value)}
                            style={{ width: "100%" }}
                        />

                        <select
                            value={providerWireApi}
                            onChange={(e) =>
                                setProviderWireApi(e.target.value as "completions" | "responses")
                            }
                            style={{ width: "100%" }}
                        >
                            <option value="completions">Completions API</option>
                            <option value="responses">Responses API</option>
                        </select>

                        {providerType === "azure" && (
                            <input
                                type="text"
                                placeholder="Azure API Version (default: 2024-10-21)"
                                value={azureApiVersion}
                                onChange={(e) => setAzureApiVersion(e.target.value)}
                                style={{ width: "100%" }}
                            />
                        )}
                    </div>
                </details>

                {/* System Prompt */}
                <details className="advanced-options">
                    <summary>System Prompt</summary>
                    <div className="advanced-options-content">
                        <select
                            value={systemPromptMode}
                            onChange={(e) =>
                                setSystemPromptMode(e.target.value as "append" | "replace")
                            }
                            style={{ width: "100%" }}
                        >
                            <option value="append">Append to default</option>
                            <option value="replace">Replace entirely</option>
                        </select>

                        <textarea
                            placeholder="Enter custom system prompt..."
                            value={systemPromptContent}
                            onChange={(e) => setSystemPromptContent(e.target.value)}
                            rows={4}
                        />
                    </div>
                </details>

                <button
                    onClick={handleCreateSession}
                    disabled={isCreating}
                    style={{ width: "100%" }}
                >
                    {isCreating ? "Creating..." : "Create Session"}
                </button>
            </div>

            {/* Sessions List */}
            <div className="sessions-list">
                {sessions.length === 0 ? (
                    <div className="empty-state">
                        <span className="empty-state-icon">📭</span>
                        <span>No sessions yet</span>
                    </div>
                ) : (
                    sessions.map((session) => (
                        <SessionItem
                            key={session.id}
                            session={session}
                            isActive={session.id === activeSessionId}
                            onSelect={() => onSelectSession(session.id)}
                            onDestroy={() => onDestroySession(session.id)}
                        />
                    ))
                )}
            </div>
        </aside>
    );
}
