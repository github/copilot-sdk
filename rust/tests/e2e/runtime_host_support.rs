use std::collections::BTreeMap;
use std::future::Future;
use std::path::{Path, PathBuf};
use std::process::Command;
use std::sync::{Arc, LazyLock, Mutex};
use std::time::Duration;

use ahp::{Client as AhpClient, ClientConfig, SessionSubscription, SubscriptionEvent};
use ahp_types::version::PROTOCOL_VERSION;
use ahp_ws::WebSocketTransport;
use futures_util::FutureExt;
use github_copilot_sdk::{
    AhpHost, AhpHostExit, AhpHostOptions, CliProgram, Client, ClientOptions, Transport,
};
use serde::Deserialize;
use serde_json::{Value, json};

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
    lite_path: PathBuf,
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
        lite_path: artifact("COPILOTD_LITE_PATH"),
        bundled: false,
    }
});

pub async fn run<F>(test: F)
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
    let mut ctx =
        E2eContext::new_with_cli("session", SNAPSHOT, Some(ARTIFACTS.runtime_path.clone()))
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
            ![
                "COPILOT_CLI_PATH",
                "COPILOTD_LITE_PATH",
                "COPILOT_RUNTIME_PROVIDER_LIB",
            ]
            .iter()
            .any(|key| name == key)
        });
        options = options.with_env_remove([
            "COPILOT_CLI_PATH",
            "COPILOTD_LITE_PATH",
            "COPILOT_RUNTIME_PROVIDER_LIB",
        ]);
    } else {
        options.env.extend([
            (
                "COPILOTD_LITE_PATH".into(),
                ARTIFACTS.lite_path.as_os_str().to_owned(),
            ),
            (
                "COPILOT_RUNTIME_PROVIDER_LIB".into(),
                ARTIFACTS.provider_path.as_os_str().to_owned(),
            ),
        ]);
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

pub fn exit_observer() -> (AhpHostOptions, Arc<Mutex<Vec<AhpHostExit>>>) {
    let exits = Arc::new(Mutex::new(Vec::new()));
    let seen = exits.clone();
    let options = AhpHostOptions::default().with_on_exit(move |exit| {
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

pub async fn deadline<T>(future: impl Future<Output = T>) -> T {
    tokio::time::timeout(DEADLINE, future)
        .await
        .expect("AHP operation deadline")
}

pub struct Ahp {
    pub client: AhpClient,
    pub client_id: String,
}

pub async fn connect(host: &AhpHost) -> Ahp {
    connect_url(&host.url, host.token.as_deref()).await
}

pub async fn connect_url(url: &str, token: Option<&str>) -> Ahp {
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
    let client_id = uuid::Uuid::new_v4().to_string();
    let initialized = client
        .initialize(client_id.clone(), vec![PROTOCOL_VERSION.into()], vec![])
        .await
        .unwrap();
    assert_eq!(initialized.protocol_version, "0.9.0");
    Ahp { client, client_id }
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
    let _: Value = ahp
        .client
        .request(
            "authenticate",
            json!({
                "channel": ROOT, "resource": resource, "token": DEFAULT_TEST_TOKEN,
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
    authenticate(ahp).await;
    let uri = format!("ahp-session:/{}", uuid::Uuid::new_v4());
    let _: Value = ahp
        .client
        .request("createSession", create_params(ahp, ctx, &uri))
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

pub async fn turn(ahp: &Ahp, chat: &str, mut subscription: SessionSubscription) {
    let id = uuid::Uuid::new_v4().to_string();
    let action = serde_json::from_value(json!({
        "type": "chat/turnStarted", "turnId": id, "startedAt": "2026-09-21T00:00:00.000Z",
        "message": {"text": PROMPT, "origin": {"kind": "user"}, "model": {"id": "claude-sonnet-5"}}
    }))
    .unwrap();
    ahp.client.dispatch(chat.into(), action).await.unwrap();
    deadline(async {
        let mut parts = BTreeMap::<String, String>::new();
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
                    assert!(deltas > 0, "must stream real deltas");
                    assert!(parts.values().cloned().collect::<String>().contains('4'));
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

pub fn topology(host: &AhpHost, owner: &Client, catalog: &Path) {
    let pid = host.pid;
    let runtime = owner.pid().expect("owned out-of-process runtime");
    let status = std::fs::read_to_string(format!("/proc/{pid}/status")).unwrap();
    let parent = status
        .lines()
        .find_map(|line| line.strip_prefix("PPid:"))
        .unwrap()
        .trim()
        .parse::<u32>()
        .unwrap();
    assert_eq!(parent, runtime, "lite must be the runtime child");
    let command = std::fs::read_to_string(format!("/proc/{pid}/cmdline")).unwrap();
    let args: Vec<_> = command.split('\0').collect();
    assert_eq!(Path::new(args[0]), ARTIFACTS.lite_path);
    if let Some(token) = &host.token {
        assert!(
            !args.iter().any(|argument| argument.contains(token)),
            "listener tokens must use framed RPC, not child argv"
        );
    }
    let index = args
        .iter()
        .position(|arg| *arg == "--catalog-path")
        .unwrap();
    assert_eq!(Path::new(args[index + 1]), catalog);
    assert_eq!(
        std::fs::read_link(format!("/proc/{runtime}/exe")).unwrap(),
        ARTIFACTS.runtime_path
    );
    let maps = std::fs::read_to_string(format!("/proc/{runtime}/maps")).unwrap();
    assert!(
        maps.contains(ARTIFACTS.provider_path.to_str().unwrap()),
        "runtime must load selected provider"
    );
    assert_eq!(
        std::fs::read_to_string(format!("/proc/{pid}/task/{pid}/children"))
            .unwrap()
            .trim(),
        "",
        "lite must not spawn another runtime"
    );
    if ARTIFACTS.bundled {
        let env = std::fs::read_to_string(format!("/proc/{runtime}/environ")).unwrap();
        for key in ["COPILOTD_LITE_PATH=", "COPILOT_RUNTIME_PROVIDER_LIB="] {
            assert!(
                !env.split('\0').any(|entry| entry.starts_with(key)),
                "candidate must use adjacent assets"
            );
        }
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

pub async fn stopped(host: &AhpHost, ahp: &Ahp) {
    reaped(host.pid.try_into().unwrap()).await;
    assert!(
        deadline(ahp.client.ping()).await.is_err(),
        "existing AHP client must disconnect"
    );
    let url = reqwest::Url::parse(&host.url).unwrap();
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

pub fn kill(host: &AhpHost) {
    assert!(
        Command::new("sh")
            .args([
                "-c",
                "kill -KILL \"$1\"",
                "kill-host",
                &host.pid.to_string()
            ])
            .status()
            .unwrap()
            .success()
    );
}
