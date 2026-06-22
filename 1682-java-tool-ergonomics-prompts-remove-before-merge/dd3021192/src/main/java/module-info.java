/**
 * Named-module test for issue #1682 Phase 3.7.
 *
 * Demonstrates that Class.forName() can locate a generated $$CopilotToolMeta
 * companion class from within a named JPMS module, without requiring extra exports.
 */
module com.github.dd3021192 {
    requires com.github.copilot.java;
}
