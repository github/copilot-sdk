// Source: hooks/pre-tool-use.md:15
type PreToolUseHandler = (
  input: PreToolUseHookInput,
  invocation: HookInvocation
) => Promise<PreToolUseHookOutput | null | undefined>;