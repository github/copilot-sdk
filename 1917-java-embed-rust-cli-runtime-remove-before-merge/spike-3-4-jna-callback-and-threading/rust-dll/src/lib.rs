use std::ffi::c_void;
use std::thread;

/// Type alias matching the C ABI callback signature:
///   void on_outbound(void* user_data, const uint8_t* data, size_t len)
type OutboundCallback = extern "C" fn(user_data: *mut c_void, data: *const u8, len: usize);

/// Simulates `copilot_runtime_host_start`.
/// Returns a dummy server handle (always 42).
#[no_mangle]
pub extern "C" fn host_start() -> u32 {
    println!("[rust] host_start called on thread {:?}", thread::current().id());
    let handle: u32 = 42;
    println!("[rust] host_start returning server handle = {}", handle);
    handle
}

/// Simulates `copilot_runtime_host_shutdown`.
#[no_mangle]
pub extern "C" fn host_shutdown(server_handle: u32) -> bool {
    println!(
        "[rust] host_shutdown called on thread {:?}, server_handle={}",
        thread::current().id(),
        server_handle
    );
    println!("[rust] host_shutdown returning true");
    true
}

/// Simulates `copilot_runtime_connection_open`.
///
/// Registers the callback, then spawns a **new native thread** that invokes the
/// callback multiple times with JSON-RPC-like payloads. This is the key behavior
/// we want to observe from the Java side:
///   - JNA automatically attaches the native thread to the JVM
///   - The callback runs on this native thread, not the Java caller's thread
///
/// `burst_count` controls how many messages the native thread sends.
/// Returns a dummy connection handle (always 7).
#[no_mangle]
pub extern "C" fn connection_open(
    server_handle: u32,
    callback: OutboundCallback,
    user_data: *mut c_void,
    burst_count: u32,
) -> u32 {
    println!(
        "[rust] connection_open called on thread {:?}, server_handle={}, burst_count={}",
        thread::current().id(),
        server_handle,
        burst_count
    );

    // Safety: we trust the caller keeps user_data and callback alive for the
    // duration of the spawned thread. In production the Rust runtime guarantees
    // this via CallbackState + AtomicUsize tracking.
    let ud = user_data as usize; // make it Send
    let count = burst_count;

    thread::spawn(move || {
        let user_data = ud as *mut c_void;
        println!(
            "[rust]   native thread {:?} started, will send {} messages",
            thread::current().id(),
            count
        );

        for i in 0..count {
            let msg = format!(
                "{{\"jsonrpc\":\"2.0\",\"id\":{},\"result\":\"hello from rust thread\"}}",
                i
            );
            let bytes = msg.as_bytes();
            println!(
                "[rust]   native thread {:?} invoking callback #{}, {} bytes",
                thread::current().id(),
                i,
                bytes.len()
            );
            callback(user_data, bytes.as_ptr(), bytes.len());
            println!(
                "[rust]   native thread {:?} callback #{} returned",
                thread::current().id(),
                i
            );
        }

        println!(
            "[rust]   native thread {:?} done, all {} messages sent",
            thread::current().id(),
            count
        );
    });

    let conn_handle: u32 = 7;
    println!(
        "[rust] connection_open returning connection handle = {} (native thread spawned in background)",
        conn_handle
    );
    conn_handle
}

/// Simulates `copilot_runtime_connection_write`.
/// Just logs the data it receives from Java.
#[no_mangle]
pub extern "C" fn connection_write(connection_handle: u32, data: *const u8, len: usize) -> bool {
    println!(
        "[rust] connection_write called on thread {:?}, connection_handle={}, len={}",
        thread::current().id(),
        connection_handle,
        len
    );
    if !data.is_null() && len > 0 {
        let slice = unsafe { std::slice::from_raw_parts(data, len) };
        if let Ok(s) = std::str::from_utf8(slice) {
            println!("[rust]   received from Java: {}", s);
        }
    }
    println!("[rust] connection_write returning true");
    true
}

/// Simulates `copilot_runtime_connection_close`.
#[no_mangle]
pub extern "C" fn connection_close(connection_handle: u32) -> bool {
    println!(
        "[rust] connection_close called on thread {:?}, connection_handle={}",
        thread::current().id(),
        connection_handle
    );
    println!("[rust] connection_close returning true");
    true
}
