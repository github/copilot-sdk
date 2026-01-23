/**
 * StatusBar component - Connection status and logs
 */

import type { LogEntry } from "../types";

interface StatusBarProps {
    isConnected: boolean;
    logs: LogEntry[];
    showLogs: boolean;
    onToggleLogs: () => void;
}

export function StatusBar({ isConnected, logs, showLogs, onToggleLogs }: StatusBarProps) {
    return (
        <>
            <div className="status-bar">
                <div className="status-indicator">
                    <div className={`status-dot ${isConnected ? "" : "disconnected"}`} />
                    <span>{isConnected ? "Connected to SDK" : "Disconnected"}</span>
                </div>
                <button
                    className="btn-secondary"
                    onClick={onToggleLogs}
                    style={{ fontSize: 11, padding: "4px 8px" }}
                >
                    {showLogs ? "Hide Logs" : "Show Logs"}
                </button>
            </div>

            {showLogs && (
                <div className="logs-container">
                    {logs.length === 0 ? (
                        <div style={{ color: "var(--text-secondary)" }}>No logs yet</div>
                    ) : (
                        logs.map((log, index) => (
                            <div key={index} className={`log-entry ${log.level}`}>
                                [{log.timestamp}] [{log.level.toUpperCase()}] {log.message}
                            </div>
                        ))
                    )}
                </div>
            )}
        </>
    );
}
