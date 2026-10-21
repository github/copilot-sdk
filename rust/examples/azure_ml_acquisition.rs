// Copyright (c) Microsoft Corporation. All rights reserved.

//! Hold an Azure ML family mutation lease before starting a chat session.
//!
//! Set `COPILOT_CLI_PATH` to a built CLI before running this example. The
//! example makes no Azure ML network request and downloads no model.

use github_copilot_sdk::rpc::{
    AzureMlDownloadIdRequest, AzureMlFamilyReleaseRequest, AzureMlFamilyRequest, AzureMlRegistry,
};
use github_copilot_sdk::{Client, ClientOptions};

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = Client::start(ClientOptions::default()).await?;
    let lease = client
        .rpc()
        .azure_ml()
        .acquire_family(AzureMlFamilyRequest {
            registry: AzureMlRegistry::Azureml,
            name: "example-model".to_owned(),
        })
        .await?;

    let status = client
        .rpc()
        .azure_ml()
        .status(AzureMlDownloadIdRequest {
            request_id: "not-started".to_owned().into(),
        })
        .await;
    let release = client
        .rpc()
        .azure_ml()
        .release_family(AzureMlFamilyReleaseRequest {
            lease_id: lease.lease_id,
            registry: AzureMlRegistry::Azureml,
            name: "example-model".to_owned(),
        })
        .await;
    let stopped = client.stop().await;

    let status = status?;
    release?;
    stopped?;
    println!(
        "Azure ML family lease released; no download active: {}",
        status.is_none()
    );
    Ok(())
}
