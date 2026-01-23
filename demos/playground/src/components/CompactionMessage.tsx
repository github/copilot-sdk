/**
 * CompactionMessage component - Display context window compaction events
 */

import type { Message as MessageType } from "../types";

interface CompactionMessageProps {
    message: MessageType;
}

export function CompactionMessage({ message }: CompactionMessageProps) {
    const { compaction } = message;
    if (!compaction) return null;

    // Format token count with commas
    const formatTokens = (tokens: number) => {
        return tokens.toLocaleString();
    };

    if (compaction.status === "started") {
        return (
            <div className="message compaction started">
                <div className="message-header">🗜️ Compaction Started</div>
                <div className="compaction-content">
                    <span className="compaction-status">
                        Compacting conversation history to free up context window space...
                    </span>
                </div>
            </div>
        );
    }

    // Compaction complete
    const isSuccess = compaction.success;

    return (
        <div className={`message compaction ${isSuccess ? "success" : "failed"}`}>
            <div className="message-header">
                {isSuccess ? "✅ Compaction Complete" : "❌ Compaction Failed"}
            </div>
            <div className="compaction-content">
                {isSuccess ? (
                    <>
                        {compaction.tokensRemoved !== undefined && (
                            <div className="compaction-stats">
                                <div className="compaction-item">
                                    <span className="compaction-label">Tokens Removed:</span>
                                    <span className="compaction-value highlight">
                                        {formatTokens(compaction.tokensRemoved)}
                                    </span>
                                </div>
                                {compaction.messagesRemoved !== undefined && (
                                    <div className="compaction-item">
                                        <span className="compaction-label">Messages Removed:</span>
                                        <span className="compaction-value">
                                            {compaction.messagesRemoved}
                                        </span>
                                    </div>
                                )}
                            </div>
                        )}
                        {compaction.preCompactionTokens !== undefined &&
                            compaction.postCompactionTokens !== undefined && (
                                <div className="compaction-stats">
                                    <div className="compaction-item">
                                        <span className="compaction-label">Before:</span>
                                        <span className="compaction-value">
                                            {formatTokens(compaction.preCompactionTokens)} tokens
                                        </span>
                                    </div>
                                    <div className="compaction-item">
                                        <span className="compaction-label">After:</span>
                                        <span className="compaction-value">
                                            {formatTokens(compaction.postCompactionTokens)} tokens
                                        </span>
                                    </div>
                                </div>
                            )}
                        {compaction.compactionTokensUsed && (
                            <div className="compaction-stats">
                                <div className="compaction-item">
                                    <span className="compaction-label">Compaction Cost:</span>
                                    <span className="compaction-value">
                                        {formatTokens(
                                            compaction.compactionTokensUsed.input +
                                                compaction.compactionTokensUsed.output
                                        )}{" "}
                                        tokens
                                    </span>
                                </div>
                            </div>
                        )}
                        {compaction.summaryContent && (
                            <details className="compaction-summary">
                                <summary>View Summary</summary>
                                <div className="compaction-summary-content">
                                    {compaction.summaryContent}
                                </div>
                            </details>
                        )}
                    </>
                ) : (
                    <div className="compaction-error">
                        {compaction.error || "Unknown error during compaction"}
                    </div>
                )}
            </div>
        </div>
    );
}
