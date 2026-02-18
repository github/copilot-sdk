# syntax=docker/dockerfile:1

# Runtime image for copilot-core
# The final image contains ONLY the binary — no source code, no credentials.
# Requires a pre-built copilot-core binary to be copied in.

FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && rm -rf /var/lib/apt/lists/*

# Copy a pre-built copilot-core binary
# Set COPILOT_CLI_PATH build arg or provide the binary at build context root
ARG COPILOT_CLI_PATH=copilot-core
COPY ${COPILOT_CLI_PATH} /usr/local/bin/copilot-core
RUN chmod +x /usr/local/bin/copilot-core

EXPOSE 3000

ENTRYPOINT ["copilot-core", "--headless", "--port", "3000", "--bind", "0.0.0.0", "--auth-token-env", "GITHUB_TOKEN"]
