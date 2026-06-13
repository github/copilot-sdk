API reference
=============

.. module:: copilot

Client
------

.. autoclass:: copilot.CopilotClient
   :members:
   :undoc-members:

.. autoclass:: copilot.RuntimeConnection
   :members:

.. autoclass:: copilot.StdioRuntimeConnection
   :members:

.. autoclass:: copilot.TcpRuntimeConnection
   :members:

.. autoclass:: copilot.UriRuntimeConnection
   :members:

.. autoclass:: copilot.ChildProcessRuntimeConnection
   :members:

Sessions
--------

.. autoclass:: copilot.CopilotSession
   :members:
   :undoc-members:

.. autoclass:: copilot.SessionCapabilities
   :members:

.. autoclass:: copilot.SessionContext
   :members:

.. autoclass:: copilot.InfiniteSessionConfig
   :members:

.. autoclass:: copilot.ProviderConfig
   :members:

.. autoclass:: copilot.SystemMessageConfig
   :members:

Tools
-----

.. autofunction:: copilot.define_tool

.. autoclass:: copilot.Tool
   :members:

.. autoclass:: copilot.ToolInvocation
   :members:

.. autoclass:: copilot.ToolResult
   :members:

.. autoclass:: copilot.ToolBinaryResult
   :members:

.. autoclass:: copilot.ToolSet
   :members:

Modes
-----

.. autoclass:: copilot.CopilotClientMode
   :members:
   :undoc-members:

Events
------

.. autoclass:: copilot.SessionEvent
   :members:

.. autoclass:: copilot.SessionEventType
   :members:
   :undoc-members:

.. autoclass:: copilot.SessionEventHandler
   :members:

Hooks
-----

.. autoclass:: copilot.SessionHooks
   :members:

.. autoclass:: copilot.PreToolUseHookInput
   :members:

.. autoclass:: copilot.PreToolUseHookOutput
   :members:

.. autoclass:: copilot.PostToolUseHookInput
   :members:

.. autoclass:: copilot.PostToolUseHookOutput
   :members:

.. autoclass:: copilot.SessionStartHookInput
   :members:

.. autoclass:: copilot.SessionStartHookOutput
   :members:

.. autoclass:: copilot.SessionEndHookInput
   :members:

.. autoclass:: copilot.SessionEndHookOutput
   :members:

Canvas
------

.. autoclass:: copilot.CanvasDeclaration
   :members:

.. autoclass:: copilot.CanvasHandler
   :members:

.. autoclass:: copilot.CanvasAction
   :members:

MCP
---

.. autoclass:: copilot.MCPServerConfig
   :members:

.. autoclass:: copilot.MCPStdioServerConfig
   :members:

.. autoclass:: copilot.MCPHTTPServerConfig
   :members:

Telemetry
---------

.. autoclass:: copilot.TelemetryConfig
   :members:

Session filesystem
------------------

.. autoclass:: copilot.SessionFsProvider
   :members:

.. autoclass:: copilot.SessionFsSqliteProvider
   :members:

.. autofunction:: copilot.create_session_fs_adapter
