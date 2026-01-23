/**
 * ToolMessage component - Display a tool execution with collapsible details
 */

import { useState } from "react";
import { getToolInfo } from "../hooks/useCopilotSDK";
import type { Message } from "../types";

interface ToolMessageProps {
    message: Message;
}

export function ToolMessage({ message }: ToolMessageProps) {
    const [isExpanded, setIsExpanded] = useState(false);
    const toolInfo = getToolInfo(message.toolName || "unknown");

    const formatContent = (content: unknown): string => {
        if (content === undefined || content === null) return "(empty)";
        if (typeof content === "string") {
            // Truncate very long strings
            if (content.length > 5000) {
                return content.substring(0, 5000) + "\n... (truncated)";
            }
            return content;
        }
        try {
            return JSON.stringify(content, null, 2);
        } catch {
            return String(content);
        }
    };

    const getStatusClass = () => {
        if (!message.completed) return "running";
        return message.success ? "success" : "failure";
    };

    const getStatusText = () => {
        if (!message.completed) return "⏳";
        return message.success ? "✓" : "✗";
    };

    return (
        <div className="message tool">
            <div className="tool-header" onClick={() => setIsExpanded(!isExpanded)}>
                <span className="tool-icon">{toolInfo.icon}</span>
                <span className="tool-label">{toolInfo.label}</span>
                <span className={`tool-status ${getStatusClass()}`}>{getStatusText()}</span>
                <span className="tool-toggle">{isExpanded ? "▲" : "▼"}</span>
            </div>

            <div className={`tool-details ${isExpanded ? "" : "collapsed"}`}>
                {/* Arguments */}
                {message.arguments !== undefined && (
                    <div className="tool-section">
                        <div className="tool-section-header">Arguments</div>
                        <pre className="tool-content">{formatContent(message.arguments)}</pre>
                    </div>
                )}

                {/* Result or Error */}
                {message.completed &&
                    (message.error ? (
                        <div className="tool-section error">
                            <div className="tool-section-header">Error</div>
                            <pre className="tool-content">{message.error}</pre>
                        </div>
                    ) : message.result !== undefined ? (
                        <div className="tool-section">
                            <div className="tool-section-header">Result</div>
                            <pre className="tool-content">{formatContent(message.result)}</pre>
                        </div>
                    ) : null)}
            </div>
        </div>
    );
}
