#![allow(clippy::unwrap_used)]

use std::path::PathBuf;

use github_copilot_sdk::{CliProgram, Client, ClientOptions, ErrorKind, Transport};
use tokio::io::{AsyncRead, AsyncReadExt, AsyncWrite, AsyncWriteExt};
use tokio::net::TcpListener;

async fn read_framed(reader: &mut (impl AsyncRead + Unpin)) -> serde_json::Value {
    let mut header = String::new();
    loop {
        let mut byte = [0u8; 1];
        reader.read_exact(&mut byte).await.unwrap();
        header.push(byte[0] as char);
        if header.ends_with("\r\n\r\n") {
            break;
        }
    }
    let length = header
        .trim()
        .strip_prefix("Content-Length: ")
        .unwrap()
        .parse()
        .unwrap();
    let mut body = vec![0; length];
    reader.read_exact(&mut body).await.unwrap();
    serde_json::from_slice(&body).unwrap()
}

async fn write_result(
    writer: &mut (impl AsyncWrite + Unpin),
    request: &serde_json::Value,
    result: serde_json::Value,
) {
    let body = serde_json::to_vec(&serde_json::json!({
        "jsonrpc": "2.0",
        "id": request["id"],
        "result": result,
    }))
    .unwrap();
    writer
        .write_all(format!("Content-Length: {}\r\n\r\n", body.len()).as_bytes())
        .await
        .unwrap();
    writer.write_all(&body).await.unwrap();
    writer.flush().await.unwrap();
}

async fn run_start(paths: Option<Vec<PathBuf>>) -> Vec<serde_json::Value> {
    let listener = TcpListener::bind(("127.0.0.1", 0)).await.unwrap();
    let address = listener.local_addr().unwrap();
    let expect_builtin = paths.as_ref().is_some_and(|paths| !paths.is_empty());

    let server = tokio::spawn(async move {
        let (stream, _) = listener.accept().await.unwrap();
        let (mut reader, mut writer) = tokio::io::split(stream);
        let mut requests = Vec::new();

        let connect = read_framed(&mut reader).await;
        write_result(
            &mut writer,
            &connect,
            serde_json::json!({ "ok": true, "protocolVersion": 3, "version": "test" }),
        )
        .await;
        requests.push(connect);

        if expect_builtin {
            let builtin = read_framed(&mut reader).await;
            write_result(&mut writer, &builtin, serde_json::json!({})).await;
            requests.push(builtin);
        }
        requests
    });

    let mut options = ClientOptions::new()
        .with_program(CliProgram::Path(std::env::current_exe().unwrap()))
        .with_transport(Transport::External {
            host: address.ip().to_string(),
            port: address.port(),
            connection_token: None,
        });
    if let Some(paths) = paths {
        options = options.with_builtin_plugin_directories(paths);
    }
    let client = Client::start(options).await.unwrap();
    let requests = server.await.unwrap();
    client.force_stop();
    requests
}

#[tokio::test]
async fn default_and_empty_do_not_call_rpc() {
    for paths in [None, Some(Vec::new())] {
        let requests = run_start(paths).await;
        assert_eq!(requests.len(), 1);
        assert_eq!(requests[0]["method"], "connect");
    }
}

#[tokio::test]
async fn configured_directories_call_rpc_once_before_start_completes() {
    let cwd = std::env::current_dir().unwrap();
    let paths = vec![cwd.join("plugins/core"), cwd.join("plugins/github")];

    let requests = run_start(Some(paths.clone())).await;

    assert_eq!(requests.len(), 2);
    assert_eq!(requests[0]["method"], "connect");
    assert_eq!(requests[1]["method"], "plugins.builtin.set");
    assert_eq!(
        requests[1]["params"],
        serde_json::json!({
            "paths": paths
                .iter()
                .map(|path| path.to_str().unwrap())
                .collect::<Vec<_>>()
        })
    );
}

#[tokio::test]
async fn relative_directory_is_rejected() {
    let options = ClientOptions::new()
        .with_program(CliProgram::Path(std::env::current_exe().unwrap()))
        .with_builtin_plugin_directories(["plugins/core"]);

    let error = match Client::start(options).await {
        Ok(_) => panic!("relative path unexpectedly accepted"),
        Err(error) => error,
    };

    assert_eq!(error.kind(), &ErrorKind::InvalidConfig);
    assert!(error.to_string().contains("absolute paths"));
}
