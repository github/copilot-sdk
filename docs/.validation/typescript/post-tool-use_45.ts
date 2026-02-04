// Source: hooks/post-tool-use.md:15
type PostToolUseHandler = (
  input: PostToolUseHookInput,
  invocation: HookInvocation
) => Promise<PostToolUseHookOutput | null | undefined>;