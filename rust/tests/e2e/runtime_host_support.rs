use std::collections::BTreeMap;
use std::future::Future;
use std::path::{Path, PathBuf};
use std::process::Command;
use std::sync::{Arc, LazyLock, Mutex};
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use ahp::{Client as AhpClient, ClientConfig, SessionSubscription, SubscriptionEvent};
use ahp_types::version::PROTOCOL_VERSION;
use ahp_ws::WebSocketTransport;
use base64::Engine as _;
use base64::engine::general_purpose::{STANDARD, URL_SAFE_NO_PAD};
use futures_util::FutureExt;
use github_copilot_sdk::{
    AhpHost, AhpHostExit, AhpHostOptions, CliProgram, Client, ClientOptions, Transport,
};
use hpke::{Deserializable, Serializable};
use rand_core::{OsRng, RngCore};
use serde::Deserialize;
use serde_json::{Value, json};
use sha2::{Digest, Sha256};

use super::support::{DEFAULT_TEST_TOKEN, E2eContext};

pub const PROMPT: &str = "What is 2+2?";
pub const SNAPSHOT: &str =
    "sendandwait_blocks_until_session_idle_and_returns_final_assistant_message";
pub const ROOT: &str = "ahp-root://";
pub const RUNTIME_TOKEN: &str = "rust-runtime-host-e2e-owner";
const DEADLINE: Duration = Duration::from_secs(30);

#[derive(Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Artifacts {
    runtime_path: PathBuf,
    provider_path: PathBuf,
    bundled: bool,
}

static ARTIFACTS: LazyLock<Artifacts> = LazyLock::new(|| {
    if let Ok(manifest) = std::env::var("COPILOT_RUNTIME_HOST_CANDIDATE_MANIFEST") {
        let sdk = Path::new(env!("CARGO_MANIFEST_DIR")).parent().unwrap();
        let output = Command::new(sdk.join("nodejs/node_modules/.bin/tsx"))
            .arg(sdk.join("rust/tests/e2e/runtime_host_candidate.mts"))
            .arg(manifest)
            .output()
            .expect("run existing Node candidate materializer");
        assert!(
            output.status.success(),
            "candidate materializer: {}",
            String::from_utf8_lossy(&output.stderr)
        );
        return serde_json::from_slice(&output.stdout).expect("candidate artifact metadata");
    }
    fn artifact(name: &str) -> PathBuf {
        let path = PathBuf::from(
            std::env::var(name).unwrap_or_else(|_| panic!("set {name} to a local build")),
        );
        assert!(path.is_absolute(), "{name} must be absolute");
        let path = path.canonicalize().expect("local build exists");
        assert!(
            !path.to_string_lossy().contains("/node_modules/"),
            "no released runtime packages"
        );
        assert!(path.is_file());
        path
    }
    Artifacts {
        runtime_path: artifact("COPILOT_CLI_PATH"),
        provider_path: artifact("COPILOT_RUNTIME_PROVIDER_LIB"),
        bundled: false,
    }
});

pub async fn run<F>(test: F)
where
    F: for<'a> FnOnce(&'a E2eContext) -> std::pin::Pin<Box<dyn Future<Output = ()> + 'a>>,
{
    run_snapshot("session", SNAPSHOT, test).await;
}

pub async fn run_snapshot<F>(group: &str, snapshot: &str, test: F)
where
    F: for<'a> FnOnce(&'a E2eContext) -> std::pin::Pin<Box<dyn Future<Output = ()> + 'a>>,
{
    assert_eq!(
        std::env::var("COPILOT_RUNTIME_HOST_E2E").as_deref(),
        Ok("1")
    );
    assert_eq!(
        std::env::var("GITHUB_ACTIONS").as_deref(),
        Ok("true"),
        "canonical snapshot must be read-only"
    );
    let work = PathBuf::from(
        std::env::var_os("TMPDIR").expect("set TMPDIR to a workspace-local scratch directory"),
    );
    assert!(work.is_absolute() && !work.starts_with("/tmp") && !work.starts_with("/var/tmp"));
    let mut ctx = E2eContext::new_with_cli(group, snapshot, Some(ARTIFACTS.runtime_path.clone()))
        .await
        .unwrap();
    let outcome =
        std::panic::AssertUnwindSafe(tokio::time::timeout(Duration::from_secs(180), test(&ctx)))
            .catch_unwind()
            .await;
    ctx.cleanup(true)
        .await
        .expect("stop existing replay proxy without recording");
    match outcome {
        Ok(result) => result.expect("runtime host E2E deadline"),
        Err(panic) => std::panic::resume_unwind(panic),
    }
}

pub fn options(ctx: &E2eContext) -> ClientOptions {
    let mut options = ctx
        .client_options()
        .with_program(CliProgram::Path(ARTIFACTS.runtime_path.clone()))
        .with_transport(Transport::Stdio);
    if ARTIFACTS.bundled {
        options.env.retain(|(name, _)| {
            !["COPILOT_CLI_PATH", "COPILOT_RUNTIME_PROVIDER_LIB"]
                .iter()
                .any(|key| name == key)
        });
        options = options.with_env_remove(["COPILOT_CLI_PATH", "COPILOT_RUNTIME_PROVIDER_LIB"]);
    } else {
        options.env.extend([(
            "COPILOT_RUNTIME_PROVIDER_LIB".into(),
            ARTIFACTS.provider_path.as_os_str().to_owned(),
        )]);
    }
    options
}

pub async fn start(ctx: &E2eContext) -> Client {
    Client::start(options(ctx))
        .await
        .expect("start local runtime")
}

pub fn options_with_base_directory(ctx: &E2eContext, base: &Path) -> ClientOptions {
    let mut options = options(ctx).with_base_directory(base);
    // The shared harness sets COPILOT_HOME explicitly; Rust's explicit env
    // overrides typed defaults. Remove that conflict to exercise base_directory.
    options.env.retain(|(name, _)| name != "COPILOT_HOME");
    options
}

pub fn home(ctx: &E2eContext) -> PathBuf {
    ctx.client_options()
        .env
        .iter()
        .find(|(key, _)| key == "COPILOT_HOME")
        .map(|(_, value)| PathBuf::from(value))
        .unwrap()
}

pub fn local_options() -> AhpHostOptions {
    AhpHostOptions::new().with_local_server(Default::default())
}

pub fn exit_observer() -> (AhpHostOptions, Arc<Mutex<Vec<AhpHostExit>>>) {
    let exits = Arc::new(Mutex::new(Vec::new()));
    let seen = exits.clone();
    let options = local_options().with_on_exit(move |exit| {
        seen.lock().unwrap().push(exit);
    });
    (options, exits)
}

pub async fn exit(exits: &Arc<Mutex<Vec<AhpHostExit>>>) -> AhpHostExit {
    deadline(async {
        loop {
            if let Some(exit) = exits.lock().unwrap().first().cloned() {
                return exit;
            }
            tokio::time::sleep(Duration::from_millis(20)).await;
        }
    })
    .await
}

/// Boxes eagerly so callers do not embed large wrapped futures, such as
/// `WebSocketTransport::connect`, whose size depends on consumer TLS features.
pub fn deadline<T>(future: impl Future<Output = T>) -> impl Future<Output = T> {
    let future = Box::pin(future);
    async move {
        tokio::time::timeout(DEADLINE, future)
            .await
            .expect("AHP operation deadline")
    }
}

pub struct Ahp {
    pub client: AhpClient,
    pub client_id: String,
    initialize_meta: Value,
}

pub async fn connect(host: &AhpHost) -> Ahp {
    connect_url(
        host.url.as_deref().expect("local listener URL"),
        host.token.as_deref(),
    )
    .await
}

pub async fn connect_url(url: &str, token: Option<&str>) -> Ahp {
    connect_url_as(url, token, uuid::Uuid::new_v4().to_string()).await
}

pub async fn connect_url_as(url: &str, token: Option<&str>, client_id: String) -> Ahp {
    let mut url = reqwest::Url::parse(url).unwrap();
    if let Some(token) = token {
        url.query_pairs_mut().append_pair("tkn", token);
    }
    let transport = deadline(WebSocketTransport::connect(url.as_str()))
        .await
        .expect("AHP websocket");
    let client = AhpClient::connect(
        transport,
        ClientConfig {
            default_request_timeout: Some(Duration::from_secs(15)),
            ..Default::default()
        },
    )
    .await
    .unwrap();
    let initialized = client
        .initialize(client_id.clone(), vec![PROTOCOL_VERSION.into()], vec![])
        .await
        .unwrap();
    assert_eq!(initialized.protocol_version, "0.9.0");
    let initialize_meta = serde_json::to_value(initialized).unwrap()["_meta"].clone();
    Ahp {
        client,
        client_id,
        initialize_meta,
    }
}

// Matches protocol_base/ahp/sealing.rs: HPKE base mode, empty info/AAD,
// enc || ciphertext, and the structured connection-bound auth-token plaintext.
// The harness owns the direct endpoint; no remote advertisement is trusted.
fn sealed_auth_token(initialize_meta: &Value, root_meta: &Value, resource: &str) -> String {
    let required = initialize_meta["copilot.encryptionRequired"]
        .as_array()
        .unwrap();
    assert!(required.iter().any(|purpose| purpose == "auth-token"));
    let binding = &initialize_meta["copilot.authChallenge"];
    assert_eq!(binding["required"], true);
    assert!(binding["responseMaxAgeSeconds"].as_u64().unwrap() > 0);
    let challenge = binding["challenge"].as_str().unwrap();
    assert_eq!(challenge.len(), 32);
    assert!(
        challenge
            .bytes()
            .all(|byte| byte.is_ascii_digit() || (b'a'..=b'f').contains(&byte))
    );
    let key = root_meta["copilot.encryptionKeys"]
        .as_array()
        .unwrap()
        .iter()
        .find(|key| {
            key["use"] == "auth-token" && key["algorithm"] == "hpke-x25519-hkdf-sha256-aes256gcm"
        })
        .expect("host advertises auth-token HPKE key");
    let public_key = STANDARD.decode(key["publicKey"].as_str().unwrap()).unwrap();
    assert_eq!(public_key.len(), 32);
    let key_id = key["keyId"].as_str().unwrap();
    assert_eq!(
        key_id,
        URL_SAFE_NO_PAD.encode(&Sha256::digest(&public_key)[..8])
    );
    let mut nonce = [0_u8; 16];
    OsRng.fill_bytes(&mut nonce);
    let nonce: String = nonce.iter().map(|byte| format!("{byte:02x}")).collect();
    let plaintext = serde_json::to_vec(&json!({
        "cty": "text",
        "ctx": {
            "purpose": "auth-token",
            "resource": resource,
            "connection": {
                "challenge": challenge,
                "nonce": nonce,
                "issuedAt": SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_secs(),
            },
        },
        "value": DEFAULT_TEST_TOKEN,
    }))
    .unwrap();
    type Kem = hpke::kem::X25519HkdfSha256;
    let recipient = <Kem as hpke::Kem>::PublicKey::from_bytes(&public_key).unwrap();
    let (enc, mut sender) = hpke::setup_sender::<
        hpke::aead::AesGcm256,
        hpke::kdf::HkdfSha256,
        Kem,
        _,
    >(&hpke::OpModeS::Base, &recipient, &[], &mut OsRng)
    .unwrap();
    let mut ciphertext = enc.to_bytes().to_vec();
    ciphertext.extend(sender.seal(&plaintext, &[]).unwrap());
    format!(
        "copilot-sealed.v1.{key_id}.{}",
        URL_SAFE_NO_PAD.encode(ciphertext)
    )
}

#[test]
fn fixture_authentication_is_sealed_and_bound_to_the_live_challenge() {
    type Kem = hpke::kem::X25519HkdfSha256;
    let (secret, public) = <Kem as hpke::Kem>::gen_keypair(&mut OsRng);
    let key_id = URL_SAFE_NO_PAD.encode(&Sha256::digest(public.to_bytes())[..8]);
    let challenge = "00112233445566778899aabbccddeeff";
    let initialized = json!({
        "copilot.encryptionRequired": ["auth-token", "mcp-auth-token"],
        "copilot.authChallenge": {
            "challenge": challenge, "responseMaxAgeSeconds": 300, "required": true,
        },
    });
    let root = json!({
        "copilot.encryptionKeys": [{
            "keyId": key_id, "use": "auth-token",
            "algorithm": "hpke-x25519-hkdf-sha256-aes256gcm",
            "publicKey": STANDARD.encode(public.to_bytes()),
        }],
    });
    let mut nonces = std::collections::HashSet::new();
    for _ in 0..2 {
        let before = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs();
        let sealed = sealed_auth_token(&initialized, &root, "https://api.github.com");
        assert!(!sealed.contains(DEFAULT_TEST_TOKEN));
        let prefix = format!("copilot-sealed.v1.{key_id}.");
        let bytes = URL_SAFE_NO_PAD
            .decode(sealed.strip_prefix(&prefix).unwrap())
            .unwrap();
        let enc = <Kem as hpke::Kem>::EncappedKey::from_bytes(&bytes[..32]).unwrap();
        let mut recipient =
            hpke::setup_receiver::<hpke::aead::AesGcm256, hpke::kdf::HkdfSha256, Kem>(
                &hpke::OpModeR::Base,
                &secret,
                &enc,
                &[],
            )
            .unwrap();
        let plaintext: Value =
            serde_json::from_slice(&recipient.open(&bytes[32..], &[]).unwrap()).unwrap();
        assert_eq!(plaintext["cty"], "text");
        assert_eq!(plaintext["value"], DEFAULT_TEST_TOKEN);
        assert_eq!(plaintext["ctx"]["purpose"], "auth-token");
        assert_eq!(plaintext["ctx"]["resource"], "https://api.github.com");
        let connection = &plaintext["ctx"]["connection"];
        assert_eq!(connection["challenge"], challenge);
        assert!(connection["issuedAt"].as_u64().unwrap() >= before);
        assert!(
            connection["issuedAt"].as_u64().unwrap()
                <= SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .unwrap()
                    .as_secs()
        );
        let nonce = connection["nonce"].as_str().unwrap();
        assert_eq!(nonce.len(), 32);
        assert!(
            nonce
                .bytes()
                .all(|byte| byte.is_ascii_digit() || (b'a'..=b'f').contains(&byte))
        );
        assert!(
            nonces.insert(nonce.to_owned()),
            "each authentication has a fresh nonce"
        );
    }
}

pub async fn authenticate(ahp: &Ahp) {
    let (root, _) = ahp.client.subscribe(ROOT.into()).await.unwrap();
    let root = serde_json::to_value(root).unwrap();
    let resource = root["snapshot"]["state"]["agents"]
        .as_array()
        .unwrap()
        .iter()
        .find(|agent| agent["provider"] == "copilot")
        .unwrap()["protectedResources"]
        .as_array()
        .unwrap()
        .iter()
        .find(|resource| resource["resource_name"] == "GitHub API")
        .unwrap()["resource"]
        .clone();
    let token = sealed_auth_token(
        &ahp.initialize_meta,
        &root["snapshot"]["state"]["_meta"],
        resource.as_str().unwrap(),
    );
    let _: Value = ahp
        .client
        .request(
            "authenticate",
            json!({
                "channel": ROOT, "resource": resource, "token": token,
            }),
        )
        .await
        .expect("authenticate GitHub AHP resource");
}

pub fn create_params(ahp: &Ahp, ctx: &E2eContext, uri: &str) -> Value {
    json!({
        "channel": uri,
        "provider": "copilot",
        "workingDirectories": [reqwest::Url::from_directory_path(ctx.work_dir()).unwrap().as_str()],
        "activeClient": {"clientId": ahp.client_id, "displayName": "Rust runtime host E2E", "tools": []},
    })
}

pub async fn create(ahp: &Ahp, ctx: &E2eContext) -> (String, String, SessionSubscription) {
    create_with_tools(ahp, ctx, json!([])).await
}

pub async fn create_with_tools(
    ahp: &Ahp,
    ctx: &E2eContext,
    tools: Value,
) -> (String, String, SessionSubscription) {
    authenticate(ahp).await;
    let uri = format!("ahp-session:/{}", uuid::Uuid::new_v4());
    let mut params = create_params(ahp, ctx, &uri);
    params["activeClient"]["tools"] = tools;
    let _: Value = ahp
        .client
        .request("createSession", params)
        .await
        .expect("create AHP session");
    let (session, _) = ahp.client.subscribe(uri.clone()).await.unwrap();
    let session = serde_json::to_value(session).unwrap();
    let chat = session["snapshot"]["state"]["defaultChat"]
        .as_str()
        .expect("default AHP chat")
        .to_string();
    let (_, subscription) = ahp.client.subscribe(chat.clone()).await.unwrap();
    (uri, chat, subscription)
}

pub async fn turn(ahp: &Ahp, chat: &str, subscription: SessionSubscription) {
    turn_with_prompt(ahp, chat, subscription, PROMPT, "4").await;
}

pub async fn turn_with_prompt(
    ahp: &Ahp,
    chat: &str,
    subscription: SessionSubscription,
    prompt: &str,
    expected: &str,
) {
    turn_with_client_tools(ahp, chat, subscription, prompt, expected, None).await;
}

type ClientToolHandler<'a> = &'a dyn Fn(&str, Value) -> String;

pub async fn turn_with_client_tools(
    ahp: &Ahp,
    chat: &str,
    mut subscription: SessionSubscription,
    prompt: &str,
    expected: &str,
    on_client_tool: Option<ClientToolHandler<'_>>,
) {
    let id = uuid::Uuid::new_v4().to_string();
    let action = serde_json::from_value(json!({
        "type": "chat/turnStarted", "turnId": id, "startedAt": "2026-09-21T00:00:00.000Z",
        "message": {"text": prompt, "origin": {"kind": "user"}, "model": {"id": "claude-sonnet-5"}}
    }))
    .unwrap();
    ahp.client.dispatch(chat.into(), action).await.unwrap();
    deadline(async {
        let mut parts = BTreeMap::<String, String>::new();
        let mut client_calls = BTreeMap::<String, String>::new();
        let mut completed = std::collections::BTreeSet::new();
        let mut deltas = 0;
        while let Some(event) = subscription.recv().await {
            let SubscriptionEvent::Action(envelope) = event else {
                continue;
            };
            let envelope = serde_json::to_value(envelope).unwrap();
            assert!(envelope["rejectionReason"].is_null(), "{envelope}");
            let action = &envelope["action"];
            if action["turnId"] != id {
                continue;
            }
            match action["type"].as_str().unwrap_or_default() {
                "chat/toolCallStart"
                    if action["contributor"]["kind"] == "client" && on_client_tool.is_some() =>
                {
                    assert_eq!(action["contributor"]["clientId"], ahp.client_id);
                    let call_id = action["toolCallId"].as_str().unwrap().to_owned();
                    assert!(!completed.contains(&call_id));
                    assert!(
                        client_calls
                            .insert(call_id, action["toolName"].as_str().unwrap().into())
                            .is_none()
                    );
                }
                "chat/toolCallReady"
                    if client_calls.contains_key(action["toolCallId"].as_str().unwrap()) =>
                {
                    let call_id = action["toolCallId"].as_str().unwrap();
                    let name = client_calls.remove(call_id).unwrap();
                    assert!(completed.insert(call_id.to_owned()));
                    assert_eq!(action["confirmed"], "not-needed");
                    let input =
                        serde_json::from_str(action["toolInput"].as_str().unwrap()).unwrap();
                    let text = on_client_tool.unwrap()(&name, input);
                    ahp.client.dispatch(chat.into(), serde_json::from_value(json!({
                        "type": "chat/toolCallComplete", "turnId": id, "toolCallId": call_id,
                        "result": {"success": true, "pastTenseMessage": format!("Ran {name}"),
                            "content": [{"type": "text", "text": text}]}
                    })).unwrap()).await.unwrap();
                }
                "chat/responsePart" if action["part"]["kind"] == "markdown" => {
                    parts.insert(
                        action["part"]["id"].as_str().unwrap().into(),
                        action["part"]["content"].as_str().unwrap().into(),
                    );
                }
                "chat/delta" => {
                    deltas += 1;
                    parts
                        .entry(action["partId"].as_str().unwrap().into())
                        .or_default()
                        .push_str(action["content"].as_str().unwrap());
                }
                "chat/error" => panic!("AHP turn error: {action}"),
                "chat/turnComplete" => {
                    assert!(
                        client_calls.is_empty(),
                        "all client tool calls must complete"
                    );
                    assert!(deltas > 0, "must stream real deltas");
                    assert!(
                        parts
                            .values()
                            .cloned()
                            .collect::<String>()
                            .contains(expected)
                    );
                    return;
                }
                _ => {}
            }
        }
        panic!("subscription ended before turn completion");
    })
    .await;
}

pub async fn resume(ahp: &Ahp, uri: &str, excluded_sdk_id: Option<&str>) {
    authenticate(ahp).await;
    let listed: Value = ahp
        .client
        .request("listSessions", json!({"channel": ROOT}))
        .await
        .unwrap();
    let resources: Vec<_> = listed["items"]
        .as_array()
        .unwrap()
        .iter()
        .map(|item| item["resource"].as_str().unwrap())
        .collect();
    assert!(resources.contains(&uri));
    if let Some(id) = excluded_sdk_id {
        assert!(!resources.contains(&format!("ahp-session:/{id}").as_str()));
    }
    let (session, _) = ahp
        .client
        .subscribe(uri.into())
        .await
        .expect("resume durable AHP session");
    let session = serde_json::to_value(session).unwrap();
    assert_eq!(session["snapshot"]["state"]["lifecycle"], "ready");
    let chat_uri = session["snapshot"]["state"]["defaultChat"]
        .as_str()
        .unwrap();
    let (chat, _) = ahp.client.subscribe(chat_uri.into()).await.unwrap();
    let chat = serde_json::to_value(chat).unwrap();
    let turn = chat["snapshot"]["state"]["turns"]
        .as_array()
        .unwrap()
        .iter()
        .find(|turn| turn["message"]["text"] == PROMPT)
        .unwrap();
    assert_eq!(turn["state"], "complete");
    assert!(
        turn["responseParts"]
            .as_array()
            .unwrap()
            .iter()
            .filter(|part| part["kind"] == "markdown")
            .map(|part| part["content"].as_str().unwrap())
            .collect::<String>()
            .contains('4')
    );
}

pub fn topology(host: &AhpHost, owner: &Client, home: &Path) {
    let runtime = owner.pid().expect("owned out-of-process runtime");
    let settings: Value =
        serde_json::from_slice(&std::fs::read(home.join("settings.json")).unwrap()).unwrap();
    let compute_id = settings["ahpEnvironment"]["computeId"]
        .as_str()
        .expect("host must save its default compute identity");
    uuid::Uuid::parse_str(compute_id).expect("default compute identity must be a UUID");
    let catalog = home.join("ahp").join(compute_id).join("sessions");
    assert!(catalog.is_dir(), "host must create the resolved catalog");
    assert_eq!(
        std::fs::read_to_string(catalog.join(".attached-host.owner"))
            .expect("catalog ownership metadata")
            .parse::<u32>()
            .unwrap(),
        runtime,
        "the SDK runtime owns the compute-scoped catalog"
    );
    let command = std::fs::read_to_string(format!("/proc/{runtime}/cmdline")).unwrap();
    let args: Vec<_> = command.split('\0').collect();
    assert!(!command.contains("copilotd"), "no companion host process");
    if let Some(token) = &host.token {
        assert!(
            !args.iter().any(|argument| argument.contains(token)),
            "listener tokens must not enter runtime argv"
        );
    }
    assert_eq!(
        std::fs::read_link(format!("/proc/{runtime}/exe")).unwrap(),
        ARTIFACTS.runtime_path
    );
    let maps = std::fs::read_to_string(format!("/proc/{runtime}/maps")).unwrap();
    assert!(
        maps.contains(ARTIFACTS.provider_path.to_str().unwrap()),
        "runtime must load selected provider"
    );
    let mut descendants = vec![runtime];
    while let Some(parent) = descendants.pop() {
        let mut children = std::collections::BTreeSet::new();
        let tasks = match std::fs::read_dir(format!("/proc/{parent}/task")) {
            Ok(tasks) => tasks,
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => continue,
            Err(error) => panic!("read descendant tasks: {error}"),
        };
        for task in tasks {
            let task = task.unwrap();
            match std::fs::read_to_string(task.path().join("children")) {
                Ok(list) => children.extend(list.split_whitespace().map(str::to_owned)),
                Err(error) if error.kind() == std::io::ErrorKind::NotFound => continue,
                Err(error) => panic!("read runtime children: {error}"),
            }
        }
        for child in children {
            let inspect = || -> std::io::Result<_> {
                Ok((
                    std::fs::read_to_string(format!("/proc/{child}/cmdline"))?,
                    std::fs::read_link(format!("/proc/{child}/exe"))?,
                    std::fs::read_to_string(format!("/proc/{child}/maps"))?,
                ))
            };
            let (command, executable, maps) = match inspect() {
                Ok(details) => details,
                Err(error) if error.kind() == std::io::ErrorKind::NotFound => continue,
                Err(error) => panic!("inspect runtime descendant: {error}"),
            };
            assert!(
                !command.contains("copilotd") && !command.contains("copilot-runtime"),
                "no companion host or second runtime"
            );
            assert_ne!(executable, ARTIFACTS.runtime_path);
            assert!(!maps.contains(ARTIFACTS.provider_path.to_str().unwrap()));
            descendants.push(child.parse::<u32>().unwrap());
        }
    }
    let sockets: std::collections::BTreeSet<_> = std::fs::read_dir(format!("/proc/{runtime}/fd"))
        .unwrap()
        .filter_map(|entry| match std::fs::read_link(entry.unwrap().path()) {
            Ok(target) => Some(target.to_string_lossy().into_owned()),
            Err(error) if error.kind() == std::io::ErrorKind::NotFound => None,
            Err(error) => panic!("read runtime socket: {error}"),
        })
        .collect();
    let port = reqwest::Url::parse(host.url.as_deref().expect("local listener URL"))
        .unwrap()
        .port()
        .unwrap();
    assert!(
        ["tcp", "tcp6"].iter().any(|table| {
            std::fs::read_to_string(format!("/proc/{runtime}/net/{table}"))
                .unwrap()
                .lines()
                .skip(1)
                .any(|line| {
                    let fields: Vec<_> = line.split_whitespace().collect();
                    fields.len() > 9
                        && fields[3] == "0A"
                        && fields[1]
                            .rsplit(':')
                            .next()
                            .and_then(|value| u16::from_str_radix(value, 16).ok())
                            == Some(port)
                        && sockets.contains(&format!("socket:[{}]", fields[9]))
                })
        }),
        "runtime must own the listening TCP socket"
    );
    if ARTIFACTS.bundled {
        let env = std::fs::read_to_string(format!("/proc/{runtime}/environ")).unwrap();
        assert!(
            !env.split('\0')
                .any(|entry| entry.starts_with("COPILOT_RUNTIME_PROVIDER_LIB=")),
            "candidate must use adjacent assets"
        );
    }
}

pub async fn reaped(pid: u32) {
    deadline(async {
        while Path::new(&format!("/proc/{pid}")).exists() {
            tokio::time::sleep(Duration::from_millis(20)).await;
        }
    })
    .await;
}

pub async fn stopped(host: &AhpHost, ahp: &Ahp, runtime: &Client) {
    listener_closed(host, ahp).await;
    assert!(
        Path::new(&format!("/proc/{}", runtime.pid().expect("owned runtime"))).exists(),
        "owning runtime must survive"
    );
}

pub async fn stopped_after_owner_disconnect(host: &AhpHost, ahp: &Ahp, runtime: &Client) {
    // Local owner-loss callbacks do not acknowledge the runtime's asynchronous cleanup.
    tokio::time::timeout(DEADLINE, async {
        while ahp.client.ping().await.is_ok() {
            tokio::time::sleep(Duration::from_millis(20)).await;
        }
    })
    .await
    .expect("existing AHP client must disconnect after owner loss");
    stopped(host, ahp, runtime).await;
}

pub async fn listener_closed(host: &AhpHost, ahp: &Ahp) {
    assert!(
        deadline(ahp.client.ping()).await.is_err(),
        "existing AHP client must disconnect"
    );
    let url = reqwest::Url::parse(host.url.as_deref().expect("local listener URL")).unwrap();
    let result = deadline(tokio::net::TcpStream::connect((
        url.host_str().unwrap().trim_matches(['[', ']']),
        url.port().unwrap(),
    )))
    .await;
    assert_eq!(
        result.unwrap_err().kind(),
        std::io::ErrorKind::ConnectionRefused
    );
}
