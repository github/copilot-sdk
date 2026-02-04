// Source: auth/byok.md:312
// ❌ Error: Model required with custom provider
const session = await client.createSession({
    provider: { type: "openai", baseUrl: "..." },
});

// ✅ Correct: Model specified
const session = await client.createSession({
    model: "gpt-4",  // Required!
    provider: { type: "openai", baseUrl: "..." },
});