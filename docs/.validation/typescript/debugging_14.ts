// Source: debugging.md:416
session.on("tool.execution_error", (event) => {
  console.error("Tool error:", event.data);
});

session.on("error", (event) => {
  console.error("Session error:", event.data);
});