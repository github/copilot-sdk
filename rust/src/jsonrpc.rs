use anyhow::Result;
use serde::{de::DeserializeOwned, Serialize};
use tokio::io::{AsyncBufReadExt, AsyncWriteExt, BufReader};
use tokio::sync::mpsc;

pub struct JsonRpcTransport {
    write_tx: mpsc::Sender<String>,
}

impl JsonRpcTransport {
    pub fn new<In>(incoming_handler: mpsc::Sender<In>) -> Self
    where
        In: DeserializeOwned + Send + 'static,
    {
        let (write_tx, mut write_rx) = mpsc::channel::<String>(32);

        tokio::spawn(async move {
            let mut stdout = tokio::io::stdout();
            while let Some(msg) = write_rx.recv().await {
                let _ = stdout.write_all(msg.as_bytes()).await;
                let _ = stdout.write_all(b"\n").await;
                let _ = stdout.flush().await;
            }
        });

        tokio::spawn(async move {
            let stdin = tokio::io::stdin();
            let mut reader = BufReader::new(stdin).lines();

            while let Ok(Some(line)) = reader.next_line().await {
                if line.trim().is_empty() { continue; }

                match serde_json::from_str::<In>(&line) {
                    Ok(event) => {
                        if incoming_handler.send(event).await.is_err() {
                            break;
                        }
                    }
                    Err(e) => {
                        eprintln!("Error parsing JSON input: {} | Line: {}", e, line);
                    }
                }
            }
        });

        Self { write_tx }
    }

    pub async fn send<T: Serialize>(&self, message: &T) -> Result<()> {
        let json = serde_json::to_string(message)?;
        self.write_tx.send(json).await.map_err(|e| anyhow::anyhow!("Channel closed: {}", e))
    }
}
