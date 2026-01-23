/**
 * SessionItem component - A single session in the sidebar
 */

import type { Session } from "../types";

interface SessionItemProps {
    session: Session;
    isActive: boolean;
    onSelect: () => void;
    onDestroy: () => void;
}

export function SessionItem({ session, isActive, onSelect, onDestroy }: SessionItemProps) {
    const handleDestroy = (e: React.MouseEvent) => {
        e.stopPropagation();
        if (confirm(`Destroy session ${session.id}?`)) {
            onDestroy();
        }
    };

    return (
        <div className={`session-item ${isActive ? "active" : ""}`}>
            <div className="session-item-info" onClick={onSelect}>
                <div className="session-item-id">{session.id}</div>
                <div className="session-item-model">Model: {session.model}</div>
            </div>
            <button className="session-destroy-btn" onClick={handleDestroy} title="Destroy session">
                ✕
            </button>
        </div>
    );
}
