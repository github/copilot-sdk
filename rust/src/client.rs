//! Client for managing Copilot CLI connections and sessions

use crate::error::{Error, Result};
use crate::generated::SessionEvent;
use crate::jsonrpc::{JsonRpcClient, NotificationHandler, RequestHandler};
use crate::sdk_protocol_version::SDK_PROTOCOL_VERSION;
use crate::session::{Session, SessionConfig};
use crate::types::{ClientOptions, ConnectionState, PermissionRequest};
use serde_json::Value;
use std::collections::HashMap;
use std::process::Stdio;
use std::sync::Arc;
use tokio::io::AsyncRead;
use tokio::net::TcpStream;
use tokio::process::{Child, Command};
use tokio::sync::{mpsc, Mutex};

pub use crate::types::ClientOptions as Options;

/// Copilot CLI client
pub struct Client {
    options: ClientOptions,
    client: Arc<Mutex<Option<Arc<JsonRpcClient>>>>,
    state: Arc<Mutex<ConnectionState>>,
    process: Arc<Mutex<Option<Child>>>,
    sessions: Arc<Mutex<HashMap<String, Arc<Session>>>>,
    session_event_channels: Arc<Mutex<HashMap<String, mpsc::UnboundedSender<SessionEvent>>>>,
}

impl Client {
    /// Create a new Copilot client
    pub async fn new(options: ClientOptions) -> Result<Self> {
        let client = Self {
            options: options.clone(),
            client: Arc::new(Mutex::new(None)),
            state: Arc::new(Mutex::new(ConnectionState::Disconnected)),
            process: Arc::new(Mutex::new(None)),
            sessions: Arc::new(Mutex::new(HashMap::new())),
            session_event_channels: Arc::new(Mutex::new(HashMap::new())),
        };

        // Auto-start if enabled
        if options.auto_start {
            client.start().await?;
        }

        Ok(client)
    }

    /// Start the CLI connection
    pub async fn start(&self) -> Result<()> {
        let mut state = self.state.lock().await;
        if *state == ConnectionState::Connected {
            return Err(Error::AlreadyConnected);
        }

        *state = ConnectionState::Connecting;
        drop(state);

        // Connect based on options
        let rpc_client = if let Some(ref url) = self.options.cli_url {
            self.connect_external(url).await?
        } else if self.options.use_stdio {
            self.spawn_stdio_process().await?
        } else {
            self.spawn_tcp_process().await?
        };

        // Set up notification and request handlers
        self.setup_handlers(&rpc_client).await;

        // Store client
        *self.client.lock().await = Some(Arc::new(rpc_client));

        // Send initialize request
        self.initialize().await?;

        *self.state.lock().await = ConnectionState::Connected;

        Ok(())
    }

    /// Spawn CLI process with stdio transport
    async fn spawn_stdio_process(&self) -> Result<JsonRpcClient> {
        let mut cmd = Command::new(&self.options.cli_path);
        cmd.arg("serve")
            .arg("--log-level")
            .arg(&self.options.log_level)
            .stdin(Stdio::piped())
            .stdout(Stdio::piped())
            .stderr(Stdio::inherit());

        if let Some(ref cwd) = self.options.cwd {
            cmd.current_dir(cwd);
        }

        if let Some(ref env) = self.options.env {
            for (key, value) in env {
                cmd.env(key, value);
            }
        }

        let mut child = cmd
            .spawn()
            .map_err(|e| Error::ProcessError(format!("Failed to spawn CLI process: {}", e)))?;

        let stdin = child
            .stdin
            .take()
            .ok_or_else(|| Error::ProcessError("Failed to get stdin".to_string()))?;

        let stdout = child
            .stdout
            .take()
            .ok_or_else(|| Error::ProcessError("Failed to get stdout".to_string()))?;

        *self.process.lock().await = Some(child);

        Ok(JsonRpcClient::new(stdout, stdin))
    }

    /// Spawn CLI process with TCP transport
    async fn spawn_tcp_process(&self) -> Result<JsonRpcClient> {
        // Start CLI in TCP mode
        let port = if self.options.port > 0 {
            self.options.port
        } else {
            0 // Random port
        };

        let mut cmd = Command::new(&self.options.cli_path);
        cmd.arg("serve")
            .arg("--log-level")
            .arg(&self.options.log_level)
            .arg("--port")
            .arg(port.to_string())
            .stdout(Stdio::piped())
            .stderr(Stdio::inherit());

        if let Some(ref cwd) = self.options.cwd {
            cmd.current_dir(cwd);
        }

        let mut child = cmd
            .spawn()
            .map_err(|e| Error::ProcessError(format!("Failed to spawn CLI process: {}", e)))?;

        // Read port from stdout
        let stdout = child
            .stdout
            .take()
            .ok_or_else(|| Error::ProcessError("Failed to get stdout".to_string()))?;

        let actual_port = self.read_port_from_stdout(stdout).await?;

        *self.process.lock().await = Some(child);

        // Connect to the TCP server
        let stream = TcpStream::connect(format!("127.0.0.1:{}", actual_port))
            .await
            .map_err(|e| {
                Error::ConnectionError(format!("Failed to connect to TCP server: {}", e))
            })?;

        let (reader, writer) = stream.into_split();
        Ok(JsonRpcClient::new(reader, writer))
    }

    /// Connect to external CLI server
    async fn connect_external(&self, url: &str) -> Result<JsonRpcClient> {
        // Parse URL to extract host and port
        let addr = if url.starts_with("http://") {
            url.trim_start_matches("http://")
        } else {
            url
        };

        let addr = if !addr.contains(':') {
            format!("127.0.0.1:{}", addr)
        } else {
            addr.to_string()
        };

        let stream = TcpStream::connect(&addr)
            .await
            .map_err(|e| Error::ConnectionError(format!("Failed to connect to {}: {}", addr, e)))?;

        let (reader, writer) = stream.into_split();
        Ok(JsonRpcClient::new(reader, writer))
    }

    /// Read port from CLI stdout
    async fn read_port_from_stdout<R>(&self, mut reader: R) -> Result<u16>
    where
        R: AsyncRead + Unpin,
    {
        use tokio::io::AsyncBufReadExt;
        let mut buf_reader = tokio::io::BufReader::new(&mut reader);
        let mut line = String::new();

        // Read until we find the port line
        loop {
            line.clear();
            buf_reader.read_line(&mut line).await?;

            if line.contains("listening on port") {
                // Extract port number
                if let Some(port_str) = line.split_whitespace().last() {
                    if let Ok(port) = port_str.trim().parse() {
                        return Ok(port);
                    }
                }
            }

            if line.is_empty() {
                break;
            }
        }

        Err(Error::ProcessError(
            "Failed to read port from CLI".to_string(),
        ))
    }

    /// Set up notification and request handlers
    async fn setup_handlers(&self, client: &JsonRpcClient) {
        let sessions = Arc::clone(&self.sessions);
        let channels = Arc::clone(&self.session_event_channels);

        // Handle session events
        let notification_handler: NotificationHandler = Arc::new(move |method, params| {
            let _sessions = Arc::clone(&sessions);
            let channels = Arc::clone(&channels);

            tokio::spawn(async move {
                if method == "session/event" {
                    if let Some(session_id) = params.get("sessionId").and_then(|v| v.as_str()) {
                        if let Some(event_data) = params.get("event") {
                            if let Ok(event) =
                                serde_json::from_value::<SessionEvent>(event_data.clone())
                            {
                                // Send to session event channel
                                if let Some(tx) = channels.lock().await.get(session_id) {
                                    let _ = tx.send(event);
                                }
                            }
                        }
                    }
                }
            });
        });

        client.set_notification_handler(notification_handler).await;

        // Handle tool calls
        let sessions_clone = Arc::clone(&self.sessions);
        let tool_handler: RequestHandler = Arc::new(move |params| {
            let session_id = params
                .get("sessionId")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let tool_name = params
                .get("toolName")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let tool_call_id = params
                .get("toolCallId")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let arguments = params
                .get("arguments")
                .and_then(|v| v.as_object())
                .map(|obj| {
                    obj.iter()
                        .map(|(k, v)| (k.clone(), v.clone()))
                        .collect::<HashMap<_, _>>()
                })
                .unwrap_or_default();

            let sessions = Arc::clone(&sessions_clone);

            // Spawn async handler
            tokio::spawn(async move {
                if let Some(session) = sessions.lock().await.get(&session_id) {
                    let _ = session
                        .handle_tool_call(tool_name, tool_call_id, arguments)
                        .await;
                }
            });

            Ok(HashMap::new())
        });

        client
            .register_request_handler("tool/execute".to_string(), tool_handler)
            .await;

        // Handle permission requests
        let sessions_clone = Arc::clone(&self.sessions);
        let permission_handler: RequestHandler = Arc::new(move |params| {
            let sessions = Arc::clone(&sessions_clone);

            let session_id = params
                .get("sessionId")
                .and_then(|v| v.as_str())
                .unwrap_or("")
                .to_string();

            let request = serde_json::from_value::<PermissionRequest>(
                serde_json::to_value(&params).unwrap_or_default(),
            )
            .unwrap_or_else(|_| PermissionRequest {
                kind: "unknown".to_string(),
                tool_call_id: None,
                extra: HashMap::new(),
            });

            // Handle synchronously for now
            let result = tokio::task::block_in_place(|| {
                tokio::runtime::Handle::current().block_on(async {
                    if let Some(session) = sessions.lock().await.get(&session_id) {
                        session.handle_permission_request(request).await
                    } else {
                        Ok(crate::types::PermissionRequestResult {
                            kind: "allow".to_string(),
                            rules: None,
                        })
                    }
                })
            });

            match result {
                Ok(res) => {
                    let mut map = HashMap::new();
                    if let Ok(value) = serde_json::to_value(&res) {
                        if let Some(obj) = value.as_object() {
                            for (k, v) in obj {
                                map.insert(k.clone(), v.clone());
                            }
                        }
                    }
                    Ok(map)
                }
                Err(e) => Err(crate::error::Error::PermissionDenied(e.to_string())),
            }
        });

        client
            .register_request_handler("permission/request".to_string(), permission_handler)
            .await;
    }

    /// Initialize the connection
    async fn initialize(&self) -> Result<()> {
        let client = self.client.lock().await;
        let client = client.as_ref().ok_or(Error::NotConnected)?;

        let mut params = HashMap::new();
        params.insert(
            "sdkProtocolVersion".to_string(),
            Value::Number(SDK_PROTOCOL_VERSION.into()),
        );

        client.request("initialize".to_string(), params).await?;

        Ok(())
    }

    /// Create a new session
    pub async fn create_session(&self, config: SessionConfig) -> Result<Arc<Session>> {
        let client = self.client.lock().await;
        let client = client.as_ref().ok_or(Error::NotConnected)?;

        let mut params = HashMap::new();
        if let Ok(config_value) = serde_json::to_value(&config) {
            if let Some(obj) = config_value.as_object() {
                for (k, v) in obj {
                    params.insert(k.clone(), v.clone());
                }
            }
        }

        let result = client.request("session/create".to_string(), params).await?;

        let session_id = result
            .get("sessionId")
            .and_then(|v| v.as_str())
            .ok_or_else(|| Error::Other("No session ID in response".to_string()))?
            .to_string();

        // Create event channel
        let (tx, rx) = mpsc::unbounded_channel();
        self.session_event_channels
            .lock()
            .await
            .insert(session_id.clone(), tx);

        // Create session
        let session = Arc::new(Session::new(session_id.clone(), Arc::clone(client), rx));

        // Start event loop
        Session::start_event_loop(Arc::clone(&session)).await;

        // Store session
        self.sessions
            .lock()
            .await
            .insert(session_id, Arc::clone(&session));

        Ok(session)
    }

    /// Get connection state
    pub async fn state(&self) -> ConnectionState {
        *self.state.lock().await
    }

    /// Stop the client
    pub async fn stop(&self) -> Result<()> {
        // Close all sessions
        self.sessions.lock().await.clear();
        self.session_event_channels.lock().await.clear();

        // Shutdown JSON-RPC client
        if let Some(_client) = self.client.lock().await.as_ref() {
            // Note: JsonRpcClient doesn't have shutdown yet, would need to add
        }

        // Kill process if we spawned it
        if let Some(mut child) = self.process.lock().await.take() {
            let _ = child.kill().await;
        }

        *self.state.lock().await = ConnectionState::Disconnected;

        Ok(())
    }
}

impl Drop for Client {
    fn drop(&mut self) {
        // Best effort cleanup
        if let Some(mut child) = self.process.blocking_lock().take() {
            let _ = child.start_kill();
        }
    }
}
