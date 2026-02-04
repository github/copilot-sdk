// Source: getting-started.md:476
// Subscribe to all events
var unsubscribe = session.On(ev => Console.WriteLine($"Event: {ev.Type}"));

// Filter by event type using pattern matching
session.On(ev =>
{
    switch (ev)
    {
        case SessionIdleEvent:
            Console.WriteLine("Session is idle");
            break;
        case AssistantMessageEvent msg:
            Console.WriteLine($"Message: {msg.Data.Content}");
            break;
    }
});

// Later, to unsubscribe:
unsubscribe.Dispose();