#![allow(clippy::unwrap_used)]

use github_copilot_sdk::rpc::{
    CatalogCapability, CatalogClientContract, CatalogRecommendRequest, CatalogSearchResult,
};
use github_copilot_sdk::{Client, Error, ErrorKind};
use serde_json::{Value, json};
use tokio::io::{AsyncBufReadExt, AsyncReadExt, AsyncWriteExt, BufReader, duplex};

fn request() -> CatalogRecommendRequest {
    CatalogRecommendRequest {
        contract: CatalogClientContract {
            protocol_version: 3,
            required_capabilities: vec![
                "catalog-recommend".into(),
                "catalog-search-session-bound".into(),
                "catalog-search-credential-required".into(),
            ],
        },
        policy_session_id: "existing-session".into(),
        query: "  Inspect my database\nand save the report.  ".into(),
        product: "  PostgreSQL  ".into(),
    }
}

fn wire_request() -> Value {
    json!({
        "contract": {
            "protocolVersion": 3,
            "requiredCapabilities": [
                "catalog-recommend",
                "catalog-search-session-bound",
                "catalog-search-credential-required"
            ]
        },
        "policySessionId": "existing-session",
        "query": "  Inspect my database\nand save the report.  ",
        "product": "  PostgreSQL  "
    })
}

#[test]
fn recommend_request_has_only_required_fields_and_preserves_text() {
    assert_eq!(serde_json::to_value(request()).unwrap(), wire_request());
    for field in ["contract", "policySessionId", "query", "product"] {
        let mut wire = wire_request();
        wire.as_object_mut().unwrap().remove(field);
        assert!(
            serde_json::from_value::<CatalogRecommendRequest>(wire).is_err(),
            "accepted missing {field}"
        );
    }
    assert_eq!(
        serde_json::to_value(CatalogCapability::CatalogRecommend).unwrap(),
        json!("catalog-recommend")
    );
}

async fn exchange(response_payload: Value) -> Result<CatalogSearchResult, Error> {
    let (client_write, server_read) = duplex(8192);
    let (mut server_write, client_read) = duplex(8192);
    let client = Client::from_streams(client_read, client_write, std::env::temp_dir()).unwrap();
    let server = tokio::spawn(async move {
        let mut reader = BufReader::new(server_read);
        let mut header = String::new();
        reader.read_line(&mut header).await.unwrap();
        let length: usize = header
            .trim()
            .strip_prefix("Content-Length: ")
            .unwrap()
            .parse()
            .unwrap();
        let mut separator = String::new();
        reader.read_line(&mut separator).await.unwrap();
        assert_eq!(separator, "\r\n");
        let mut body = vec![0; length];
        reader.read_exact(&mut body).await.unwrap();
        let received: Value = serde_json::from_slice(&body).unwrap();
        assert_eq!(received["method"], "catalog.recommend");
        assert_eq!(received["params"], wire_request());
        let mut response = response_payload;
        response["jsonrpc"] = json!("2.0");
        response["id"] = received["id"].clone();
        let body = serde_json::to_vec(&response).unwrap();
        server_write
            .write_all(format!("Content-Length: {}\r\n\r\n", body.len()).as_bytes())
            .await
            .unwrap();
        server_write.write_all(&body).await.unwrap();
        server_write.flush().await.unwrap();
    });
    let result = tokio::time::timeout(
        std::time::Duration::from_secs(5),
        client.rpc().catalog().recommend(request()),
    )
    .await
    .unwrap();
    server.await.unwrap();
    result
}

#[tokio::test]
async fn recommend_returns_the_existing_catalog_result() {
    let wire = json!({
        "kind": "succeeded",
        "candidates": [],
        "negotiated": {
            "runtimeProtocolVersion": 3,
            "grantedCapabilities": [
                "catalog-recommend",
                "catalog-search-session-bound",
                "catalog-search-credential-required"
            ]
        },
        "searchId": "recommendation-search",
        "truncated": false
    });
    let result = exchange(json!({"result": wire})).await.unwrap();
    assert!(matches!(result, CatalogSearchResult::Succeeded(_)));
    assert_eq!(serde_json::to_value(result).unwrap(), wire);
}

#[tokio::test]
async fn recommend_preserves_typed_refusals() {
    for wire in [
        json!({"kind": "invalid-request", "field": "query", "message": "Invalid query"}),
        json!({
            "kind": "authentication-required",
            "reason": "credential-rejected",
            "message": "Sign in required"
        }),
        json!({
            "kind": "unavailable",
            "reason": "search-unavailable",
            "message": "Recommendations unavailable"
        }),
    ] {
        let result = exchange(json!({"result": wire})).await.unwrap();
        assert!(!matches!(result, CatalogSearchResult::Succeeded(_)));
        assert_eq!(serde_json::to_value(result).unwrap(), wire);
    }
}

#[tokio::test]
async fn recommend_surfaces_unsupported_runtime_without_search_fallback() {
    let error = exchange(json!({
        "error": {"code": -32601, "message": "Method not found"}
    }))
    .await
    .unwrap_err();
    assert_eq!(error.kind(), &ErrorKind::Rpc { code: -32601 });
}
