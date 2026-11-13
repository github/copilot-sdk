//! Repository-private construction for the native CLI, not a published SDK mode.

use crate::{Client, ClientOptions, Error, ErrorKind, Transport};

/// Start the native CLI's connection after normal SDK setup.
///
/// The caller checks image identity before the SDK captures CLI authority.
/// Ordinary SDK builds do not compile this entry point.
pub async fn start<E>(
    options: ClientOptions,
    validate_image: impl FnOnce(&Client) -> Result<(), E>,
) -> Result<Client, E>
where
    E: From<Error>,
{
    if !matches!(options.transport, Transport::InProcess) {
        return Err(Error::with_message(
            ErrorKind::InvalidConfig,
            "native CLI construction requires the in-process transport",
        )
        .into());
    }
    let client = Client::start(options).await.map_err(E::from)?;
    finish_start(client, validate_image)
}

fn finish_start<E>(
    client: Client,
    validate_image: impl FnOnce(&Client) -> Result<(), E>,
) -> Result<Client, E>
where
    E: From<Error>,
{
    let _span = tracing::debug_span!("session.timing.cli_host_admission").entered();
    let admission = validate_image(&client).and_then(|()| {
        let host = client.inner.ffi_host.lock();
        let host = host.as_ref().ok_or_else(|| {
            E::from(Error::with_message(
                ErrorKind::InvalidConfig,
                "native CLI construction requires an active in-process connection",
            ))
        })?;
        host.admit_cli_host().map_err(E::from)
    });
    if let Err(error) = admission {
        client.force_stop();
        return Err(error);
    }
    Ok(client)
}

#[cfg(test)]
mod tests;
