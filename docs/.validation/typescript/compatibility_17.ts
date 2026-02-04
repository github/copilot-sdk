// Source: compatibility.md:161
const session = await client.createSession({
  infiniteSessions: {
    enabled: true,
    backgroundCompactionThreshold: 0.80,  // Start background compaction at 80% context utilization
    bufferExhaustionThreshold: 0.95,      // Block and compact at 95% context utilization
  },
});