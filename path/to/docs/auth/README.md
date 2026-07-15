# complete code
## In-Process Transport

The in-process transport does not honor environment variables set by the `CopilotClientOptions`. To set environment variables, you need to set them on the host/parent process before creating the client.

### Example