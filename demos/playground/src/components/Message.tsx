/**
 * Message component - Display a single user/assistant/error message
 */

import type { Message as MessageType } from "../types";

interface MessageProps {
    message: MessageType;
}

export function Message({ message }: MessageProps) {
    const getHeader = () => {
        switch (message.type) {
            case "user":
                return "You";
            case "assistant":
                return "Assistant";
            case "error":
                return "⚠️ Error";
            default:
                return "System";
        }
    };

    return (
        <div className={`message ${message.type}`}>
            <div className="message-header">{getHeader()}</div>
            <div className="message-content">{message.content}</div>
            {message.details && (
                <div
                    className="message-content"
                    style={{ fontSize: 12, opacity: 0.7, marginTop: 8 }}
                >
                    {message.details}
                </div>
            )}
        </div>
    );
}
