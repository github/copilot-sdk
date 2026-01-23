/**
 * UsageMessage component - Display token usage and cost information
 */

import type { Message as MessageType } from "../types";

interface UsageMessageProps {
    message: MessageType;
}

export function UsageMessage({ message }: UsageMessageProps) {
    const { usage } = message;
    if (!usage) return null;

    // Format duration in seconds
    const formatDuration = (ms: number) => {
        return `${(ms / 1000).toFixed(2)}s`;
    };

    // Format token count with commas
    const formatTokens = (tokens: number) => {
        return tokens.toLocaleString();
    };

    // Check if this is a model usage event (has inputTokens) or context window event (has tokenLimit)
    const isModelUsage = usage.inputTokens !== undefined || usage.outputTokens !== undefined;
    const isContextUsage = usage.tokenLimit !== undefined;

    return (
        <div className="message usage">
            <div className="message-header">📊 Usage</div>
            <div className="usage-content">
                {isModelUsage && (
                    <div className="usage-section">
                        {usage.model && (
                            <div className="usage-item">
                                <span className="usage-label">Model:</span>
                                <span className="usage-value">{usage.model}</span>
                            </div>
                        )}
                        <div className="usage-tokens">
                            {usage.inputTokens !== undefined && (
                                <div className="usage-item">
                                    <span className="usage-label">Input:</span>
                                    <span className="usage-value">
                                        {formatTokens(usage.inputTokens)} tokens
                                    </span>
                                </div>
                            )}
                            {usage.outputTokens !== undefined && (
                                <div className="usage-item">
                                    <span className="usage-label">Output:</span>
                                    <span className="usage-value">
                                        {formatTokens(usage.outputTokens)} tokens
                                    </span>
                                </div>
                            )}
                            {usage.cacheReadTokens !== undefined && usage.cacheReadTokens > 0 && (
                                <div className="usage-item">
                                    <span className="usage-label">Cache Read:</span>
                                    <span className="usage-value">
                                        {formatTokens(usage.cacheReadTokens)} tokens
                                    </span>
                                </div>
                            )}
                            {usage.cacheWriteTokens !== undefined && usage.cacheWriteTokens > 0 && (
                                <div className="usage-item">
                                    <span className="usage-label">Cache Write:</span>
                                    <span className="usage-value">
                                        {formatTokens(usage.cacheWriteTokens)} tokens
                                    </span>
                                </div>
                            )}
                        </div>
                        <div className="usage-meta">
                            {usage.cost !== undefined && (
                                <div className="usage-item">
                                    <span className="usage-label">Premium Requests:</span>
                                    <span className="usage-value">{usage.cost}</span>
                                </div>
                            )}
                            {usage.duration !== undefined && (
                                <div className="usage-item">
                                    <span className="usage-label">Duration:</span>
                                    <span className="usage-value">
                                        {formatDuration(usage.duration)}
                                    </span>
                                </div>
                            )}
                        </div>
                    </div>
                )}
                {isContextUsage && (() => {
                    const percentage = Math.min(100, ((usage.currentTokens || 0) / (usage.tokenLimit || 1)) * 100);
                    const bgSize = percentage > 0 ? (100 / percentage) * 100 : 100;
                    return (
                        <div className="usage-section">
                            <div className="usage-item">
                                <span className="usage-label">Context Window:</span>
                                <span className="usage-value">
                                    {formatTokens(usage.currentTokens || 0)} /{" "}
                                    {formatTokens(usage.tokenLimit || 0)} tokens
                                </span>
                            </div>
                            {usage.messagesLength !== undefined && (
                                <div className="usage-item">
                                    <span className="usage-label">Messages:</span>
                                    <span className="usage-value">{usage.messagesLength}</span>
                                </div>
                            )}
                            {usage.tokenLimit && usage.currentTokens && (
                                <div className="usage-bar">
                                    <div
                                        className="usage-bar-fill"
                                        style={{
                                            width: `${percentage}%`,
                                            backgroundSize: `${bgSize}% 100%`,
                                        }}
                                    />
                                </div>
                            )}
                        </div>
                    );
                })()}
            </div>
        </div>
    );
}
