// Source: compatibility.md:148
session.on("assistant.usage", (event) => {
  console.log("Tokens used:", {
    input: event.data.inputTokens,
    output: event.data.outputTokens,
  });
});