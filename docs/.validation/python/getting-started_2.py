# Source: getting-started.md:429
# Subscribe to all events
unsubscribe = session.on(lambda event: print(f"Event: {event.type}"))

# Filter by event type in your handler
def handle_event(event):
    if event.type == SessionEventType.SESSION_IDLE:
        print("Session is idle")
    elif event.type == SessionEventType.ASSISTANT_MESSAGE:
        print(f"Message: {event.data.content}")

unsubscribe = session.on(handle_event)

# Later, to unsubscribe:
unsubscribe()