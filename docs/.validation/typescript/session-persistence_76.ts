// Source: guides/session-persistence.md:296
try {
  // Do work...
  await session.sendAndWait({ prompt: "Complete the task" });
  
  // Task complete - clean up
  await session.destroy();
} catch (error) {
  // Clean up even on error
  await session.destroy();
  throw error;
}