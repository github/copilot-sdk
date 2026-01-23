/**
 * ChatArea component - Message display and input
 */

import { useEffect, useRef, useState } from "react";
import type { Message as MessageType, Session } from "../types";
import { CompactionMessage } from "./CompactionMessage";
import { Message } from "./Message";
import { ToolMessage } from "./ToolMessage";
import { UsageMessage } from "./UsageMessage";

interface ChatAreaProps {
    session: Session | null;
    messages: MessageType[];
    onSendMessage: (prompt: string) => Promise<void>;
}

export function ChatArea({ session, messages, onSendMessage }: ChatAreaProps) {
    const [input, setInput] = useState("");
    const [isSending, setIsSending] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    // Auto-scroll to bottom when new messages arrive
    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }, [messages]);

    const handleSend = async () => {
        if (!input.trim() || isSending) return;

        const prompt = input;
        setInput("");
        setIsSending(true);

        try {
            await onSendMessage(prompt);
        } finally {
            setIsSending(false);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    // Show empty state if no session is selected
    if (!session) {
        return (
            <div className="chat-area">
                <div className="empty-state">
                    <span className="empty-state-icon">💬</span>
                    <span>Create a session to get started</span>
                </div>
            </div>
        );
    }

    return (
        <div className="chat-area">
            {/* Messages */}
            <div className="messages-container">
                {messages.length === 0 ? (
                    <div className="empty-state">
                        <span className="empty-state-icon">✨</span>
                        <span>Send a message to start the conversation</span>
                    </div>
                ) : (
                    messages.map((msg, index) =>
                        msg.type === "tool" ? (
                            <ToolMessage key={index} message={msg} />
                        ) : msg.type === "usage" ? (
                            <UsageMessage key={index} message={msg} />
                        ) : msg.type === "compaction" ? (
                            <CompactionMessage key={index} message={msg} />
                        ) : (
                            <Message key={index} message={msg} />
                        )
                    )
                )}
                <div ref={messagesEndRef} />
            </div>

            {/* Input */}
            <div className="input-area">
                <textarea
                    value={input}
                    onChange={(e) => setInput(e.target.value)}
                    onKeyDown={handleKeyDown}
                    placeholder="Type your message... (Enter to send, Shift+Enter for newline)"
                    disabled={isSending}
                />
                <div className="input-group">
                    <button onClick={handleSend} disabled={!input.trim() || isSending}>
                        {isSending ? "Sending..." : "Send Message"}
                    </button>
                </div>
            </div>
        </div>
    );
}
