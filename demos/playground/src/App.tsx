import { useState } from "react";
import { useCopilotSDK } from "./hooks/useCopilotSDK";
import { Sidebar } from "./components/Sidebar";
import { ChatArea } from "./components/ChatArea";
import { StatusBar } from "./components/StatusBar";

/**
 * Copilot SDK Playground
 *
 * An interactive demo for testing the Copilot SDK.
 * Shows how to create sessions, send messages, and handle streaming responses.
 */
export default function App() {
    const [showLogs, setShowLogs] = useState(false);

    const sdk = useCopilotSDK();

    return (
        <div className="app">
            <header className="header">
                <h1>
                    🤖 <span className="header-text">Copilot SDK Playground</span>
                </h1>
            </header>

            <div className="container">
                <Sidebar
                    sessions={sdk.sessions}
                    activeSessionId={sdk.activeSessionId}
                    onCreateSession={sdk.createSession}
                    onSelectSession={sdk.selectSession}
                    onDestroySession={sdk.destroySession}
                />

                <main className="main-content">
                    <ChatArea
                        session={sdk.activeSession}
                        messages={sdk.messages}
                        onSendMessage={sdk.sendMessage}
                    />
                </main>
            </div>

            <StatusBar
                isConnected={sdk.isConnected}
                logs={sdk.logs}
                showLogs={showLogs}
                onToggleLogs={() => setShowLogs(!showLogs)}
            />
        </div>
    );
}
