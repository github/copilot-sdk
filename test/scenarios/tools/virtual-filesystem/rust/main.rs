use std::collections::HashMap;
use std::sync::Arc;

use async_trait::async_trait;
use copilot::handler::ApproveAllHandler;
use copilot::tool::{ToolHandler, ToolHandlerRouter};
use copilot::{Client, ClientOptions, Error, SessionConfig, Tool, ToolInvocation, ToolResult, MessageOptions};
use parking_lot::Mutex;

type VirtualFs = Arc<Mutex<HashMap<String, String>>>;

struct WriteFileTool(VirtualFs);

#[async_trait]
impl ToolHandler for WriteFileTool {
    fn tool(&self) -> Tool {
        Tool {
            name: "write_file".into(),
            description: "Create or overwrite a file in the virtual filesystem".into(),
            parameters: Some(serde_json::json!({
                "type": "object",
                "properties": {
                    "path": { "type": "string", "description": "File path" },
                    "content": { "type": "string", "description": "File content" }
                },
                "required": ["path", "content"]
            })),
            overrides_built_in_tool: None,
        }
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let path = invocation.arguments["path"].as_str().unwrap_or("").to_string();
        let content = invocation.arguments["content"].as_str().unwrap_or("").to_string();
        let len = content.len();
        self.0.lock().insert(path.clone(), content);
        Ok(ToolResult::Text(format!("Created {path} ({len} bytes)")))
    }
}

struct ReadFileTool(VirtualFs);

#[async_trait]
impl ToolHandler for ReadFileTool {
    fn tool(&self) -> Tool {
        Tool {
            name: "read_file".into(),
            description: "Read a file from the virtual filesystem".into(),
            parameters: Some(serde_json::json!({
                "type": "object",
                "properties": {
                    "path": { "type": "string", "description": "File path" }
                },
                "required": ["path"]
            })),
            overrides_built_in_tool: None,
        }
    }

    async fn call(&self, invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let path = invocation.arguments["path"].as_str().unwrap_or("");
        let fs = self.0.lock();
        match fs.get(path) {
            Some(content) => Ok(ToolResult::Text(content.clone())),
            None => Ok(ToolResult::Text(format!("Error: file not found: {path}"))),
        }
    }
}

struct ListFilesTool(VirtualFs);

#[async_trait]
impl ToolHandler for ListFilesTool {
    fn tool(&self) -> Tool {
        Tool {
            name: "list_files".into(),
            description: "List all files in the virtual filesystem".into(),
            parameters: Some(serde_json::json!({
                "type": "object",
                "properties": {}
            })),
            overrides_built_in_tool: None,
        }
    }

    async fn call(&self, _invocation: ToolInvocation) -> Result<ToolResult, Error> {
        let fs = self.0.lock();
        if fs.is_empty() {
            return Ok(ToolResult::Text("No files".into()));
        }
        let paths: Vec<&String> = fs.keys().collect();
        Ok(ToolResult::Text(
            paths.into_iter().cloned().collect::<Vec<_>>().join("\n"),
        ))
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;

    let fs: VirtualFs = Arc::new(Mutex::new(HashMap::new()));

    let router = ToolHandlerRouter::new(
        vec![
            Box::new(WriteFileTool(fs.clone())),
            Box::new(ReadFileTool(fs.clone())),
            Box::new(ListFilesTool(fs.clone())),
        ],
        Arc::new(ApproveAllHandler),
    );
    let tools = router.tools();

    let session = client
        .create_session(
            SessionConfig {
                model: Some("claude-haiku-4.5".into()),
                available_tools: Some(vec![]),
                tools: Some(tools),
                ..Default::default()
            },
            Arc::new(router),
            None,
            None,
        )
        .await?;

    let response = session
        .send_and_wait(
            MessageOptions::new("Create a file called plan.md with a brief 3-item project plan \
             for building a CLI tool. Then read it back and tell me what you wrote."),
            None,
        )
        .await?;

    if let Some(event) = response.event {
        println!("{}", event.data);
    }

    println!("\n--- Virtual filesystem contents ---");
    for (path, content) in fs.lock().iter() {
        println!("\n[{path}]");
        println!("{content}");
    }

    Ok(())
}
