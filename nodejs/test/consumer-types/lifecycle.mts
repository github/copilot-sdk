import type {
    CopilotClient,
    SessionBackgroundEvent,
    SessionCreatedEvent,
    SessionDeletedEvent,
    SessionForegroundEvent,
    SessionLifecycleEvent,
    SessionLifecycleEventMetadata,
    SessionUpdatedEvent,
} from "@github/copilot-sdk";

const metadata: SessionLifecycleEventMetadata = {
    startTime: new Date(),
    modifiedTime: new Date(),
};
const deleted: SessionDeletedEvent = { type: "session.deleted", sessionId: "test" };
const deletedWithUndefined: SessionDeletedEvent = { ...deleted, metadata: undefined };
const created: SessionCreatedEvent = { type: "session.created", sessionId: "test", metadata };
const updated: SessionUpdatedEvent = { type: "session.updated", sessionId: "test", metadata };
const foreground: SessionForegroundEvent = {
    type: "session.foreground",
    sessionId: "test",
    metadata,
};
const background: SessionBackgroundEvent = {
    type: "session.background",
    sessionId: "test",
    metadata,
};
const events: SessionLifecycleEvent[] = [
    deleted,
    deletedWithUndefined,
    created,
    updated,
    foreground,
    background,
];
for (const event of events) {
    switch (event.type) {
        case "session.deleted": {
            const absent: undefined = event.metadata;
            void absent;
            break;
        }
        case "session.created":
        case "session.updated":
        case "session.foreground":
        case "session.background": {
            const present: SessionLifecycleEventMetadata = event.metadata;
            const timestamp: Date = present.startTime;
            void timestamp;
            break;
        }
        default: {
            const exhaustive: never = event;
            void exhaustive;
        }
    }
}

// These errors are required regression assertions, not library diagnostic suppressions.
// @ts-expect-error Deleted events cannot carry metadata objects.
const invalidDeleted: SessionDeletedEvent = { ...deleted, metadata };
// @ts-expect-error Created events require metadata.
const missingCreated: SessionCreatedEvent = { type: "session.created", sessionId: "test" };
// @ts-expect-error Updated events require metadata.
const missingUpdated: SessionUpdatedEvent = { type: "session.updated", sessionId: "test" };
// @ts-expect-error Foreground events require metadata.
const missingForeground: SessionForegroundEvent = { type: "session.foreground", sessionId: "test" };
// @ts-expect-error Background events require metadata.
const missingBackground: SessionBackgroundEvent = { type: "session.background", sessionId: "test" };
// @ts-expect-error Created metadata cannot be explicitly undefined.
const undefinedCreated: SessionCreatedEvent = { ...created, metadata: undefined };
// @ts-expect-error Updated metadata cannot be explicitly undefined.
const undefinedUpdated: SessionUpdatedEvent = { ...updated, metadata: undefined };
// @ts-expect-error Foreground metadata cannot be explicitly undefined.
const undefinedForeground: SessionForegroundEvent = { ...foreground, metadata: undefined };
// @ts-expect-error Background metadata cannot be explicitly undefined.
const undefinedBackground: SessionBackgroundEvent = { ...background, metadata: undefined };
// @ts-expect-error Unknown discriminants are not lifecycle events.
const invalidType: SessionLifecycleEvent = { type: "session.unknown", sessionId: "test" };
void [
    invalidDeleted,
    missingCreated,
    missingUpdated,
    missingForeground,
    missingBackground,
    undefinedCreated,
    undefinedUpdated,
    undefinedForeground,
    undefinedBackground,
    invalidType,
];

declare const client: CopilotClient;
client.onLifecycle("session.deleted", (event) => {
    const absent: undefined = event.metadata;
    void absent;
});
client.onLifecycle("session.created", (event) => {
    const present: SessionLifecycleEventMetadata = event.metadata;
    void present;
});
