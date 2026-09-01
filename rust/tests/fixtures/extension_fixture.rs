use std::fs::OpenOptions;
use std::io::{BufRead, BufReader, Write};

use serde_json::{Value, json};

fn main() {
    let session_id = std::env::var("SESSION_ID").expect("SESSION_ID");
    record_state(json!({
        "kind": "start",
        "pid": std::process::id(),
        "sessionId": session_id,
        "sdkPath": std::env::var("COPILOT_SDK_PATH").ok(),
        "parentPid": std::env::var("COPILOT_EXTENSION_PARENT_PID").ok(),
        "extensionPath": std::env::var("EXTENSION_PATH").ok(),
        "autoUpdate": std::env::var("COPILOT_AUTO_UPDATE").ok(),
        "cliDistDir": std::env::var("COPILOT_CLI_DIST_DIR").ok(),
    }));

    let stdin = std::io::stdin();
    let mut reader = BufReader::new(stdin.lock());
    let stdout = std::io::stdout();
    let mut writer = stdout.lock();
    write_message(
        &mut writer,
        &json!({
            "jsonrpc": "2.0",
            "id": 1,
            "method": "session.resume",
            "params": {
                "sessionId": session_id,
                "tools": [{
                    "name": "fixture_echo",
                    "description": "Returns a deterministic fixture result",
                    "parameters": {
                        "type": "object",
                        "properties": { "text": { "type": "string" } },
                        "required": ["text"]
                    }
                }],
                "requestPermission": false,
                "enableConfigDiscovery": false,
                "disableResume": true,
                "streaming": true
            }
        }),
    );

    let mut next_id = 2_u64;
    while let Some(message) = read_message(&mut reader) {
        if message.get("id") == Some(&json!(1)) {
            assert!(
                message.get("error").is_none(),
                "extension resume failed: {message}"
            );
            continue;
        }
        if message.pointer("/params/event/type") != Some(&json!("external_tool.requested")) {
            continue;
        }
        let Some(request_id) = message
            .pointer("/params/event/data/requestId")
            .and_then(Value::as_str)
        else {
            continue;
        };
        record_state(json!({
            "kind": "invoke",
            "pid": std::process::id(),
            "requestId": request_id,
        }));
        write_message(
            &mut writer,
            &json!({
                "jsonrpc": "2.0",
                "id": next_id,
                "method": "session.tools.handlePendingToolCall",
                "params": {
                    "sessionId": session_id,
                    "requestId": request_id,
                    "result": "echoed"
                }
            }),
        );
        next_id += 1;
    }
}

fn record_state(value: Value) {
    let path = std::env::var("FIXTURE_STATE_PATH").expect("FIXTURE_STATE_PATH");
    let mut file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(path)
        .expect("open fixture state");
    writeln!(file, "{value}").expect("write fixture state");
}

fn write_message(writer: &mut impl Write, value: &Value) {
    let body = serde_json::to_vec(value).expect("serialize fixture message");
    write!(writer, "Content-Length: {}\r\n\r\n", body.len()).expect("write fixture header");
    writer.write_all(&body).expect("write fixture body");
    writer.flush().expect("flush fixture message");
}

fn read_message(reader: &mut impl BufRead) -> Option<Value> {
    let mut content_length = None;
    loop {
        let mut line = String::new();
        if reader.read_line(&mut line).expect("read fixture header") == 0 {
            return None;
        }
        if line == "\r\n" {
            break;
        }
        if let Some(value) = line.strip_prefix("Content-Length:") {
            content_length = Some(
                value
                    .trim()
                    .parse::<usize>()
                    .expect("parse fixture content length"),
            );
        }
    }
    let mut body = vec![0; content_length.expect("fixture content length")];
    reader.read_exact(&mut body).expect("read fixture body");
    Some(serde_json::from_slice(&body).expect("parse fixture message"))
}
