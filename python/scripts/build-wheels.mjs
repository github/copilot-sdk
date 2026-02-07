#!/usr/bin/env node
/**
 * Build platform-specific Python wheels with bundled Copilot CLI binaries.
 *
 * Downloads the Copilot CLI binary for each platform from the npm registry
 * and builds a wheel that includes it.
 *
 * Usage:
 *   node scripts/build-wheels.mjs [--platform PLATFORM] [--output-dir DIR]
 *
 *   --platform: Build for specific platform only (linux-x64, linux-arm64, darwin-x64,
 *               darwin-arm64, win32-x64, win32-arm64). If not specified, builds all.
 *   --output-dir: Directory for output wheels (default: dist/)
 */

import { execSync } from "node:child_process";
import {
    createWriteStream,
    existsSync,
    mkdirSync,
    readFileSync,
    writeFileSync,
    chmodSync,
    rmSync,
    cpSync,
    readdirSync,
    statSync,
} from "node:fs";
import { dirname, join } from "node:path";
import { pipeline } from "node:stream/promises";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const pythonDir = dirname(__dirname);
const repoRoot = dirname(pythonDir);

// Platform mappings: npm package suffix -> [wheel platform tag, binary name]
const PLATFORMS = {
    "linux-x64": ["manylinux_2_17_x86_64", "copilot"],
    "linux-arm64": ["manylinux_2_17_aarch64", "copilot"],
    "darwin-x64": ["macosx_10_9_x86_64", "copilot"],
    "darwin-arm64": ["macosx_11_0_arm64", "copilot"],
    "win32-x64": ["win_amd64", "copilot.exe"],
    "win32-arm64": ["win_arm64", "copilot.exe"],
};

function getCliVersion() {
    const packageLockPath = join(repoRoot, "nodejs", "package-lock.json");
    if (!existsSync(packageLockPath)) {
        throw new Error(
            `package-lock.json not found at ${packageLockPath}. Run 'npm install' in nodejs/ first.`
        );
    }

    const packageLock = JSON.parse(readFileSync(packageLockPath, "utf-8"));
    const version = packageLock.packages?.["node_modules/@github/copilot"]?.version;

    if (!version) {
        throw new Error("Could not find @github/copilot version in package-lock.json");
    }

    return version;
}

function getPkgVersion() {
    const pyprojectPath = join(pythonDir, "pyproject.toml");
    const content = readFileSync(pyprojectPath, "utf-8");
    const match = content.match(/version\s*=\s*"([^"]+)"/);
    if (!match) {
        throw new Error("Could not find version in pyproject.toml");
    }
    return match[1];
}

async function downloadCliBinary(platform, cliVersion, cacheDir) {
    const [, binaryName] = PLATFORMS[platform];
    const cachedBinary = join(cacheDir, binaryName);

    // Check cache
    if (existsSync(cachedBinary)) {
        console.log(`  Using cached ${binaryName}`);
        return cachedBinary;
    }

    const tarballUrl = `https://registry.npmjs.org/@github/copilot-${platform}/-/copilot-${platform}-${cliVersion}.tgz`;
    console.log(`  Downloading from ${tarballUrl}...`);

    // Download tarball
    const response = await fetch(tarballUrl);
    if (!response.ok) {
        throw new Error(`Failed to download: ${response.status} ${response.statusText}`);
    }

    // Extract to cache dir
    mkdirSync(cacheDir, { recursive: true });

    const tarballPath = join(cacheDir, `copilot-${platform}-${cliVersion}.tgz`);
    const fileStream = createWriteStream(tarballPath);

    await pipeline(response.body, fileStream);

    // Extract binary from tarball using system tar
    // On Windows, use the system32 tar to avoid Git Bash tar issues
    const tarCmd = process.platform === "win32"
        ? `"${process.env.SystemRoot}\\System32\\tar.exe"`
        : "tar";
    
    try {
        execSync(`${tarCmd} -xzf "${tarballPath}" -C "${cacheDir}" --strip-components=1 "package/${binaryName}"`, {
            stdio: "inherit",
        });
    } catch (e) {
        // Clean up on failure
        if (existsSync(tarballPath)) {
            rmSync(tarballPath);
        }
        throw new Error(`Failed to extract binary: ${e.message}`);
    }

    // Clean up tarball
    rmSync(tarballPath);

    // Verify binary exists
    if (!existsSync(cachedBinary)) {
        throw new Error(`Binary not found after extraction: ${cachedBinary}`);
    }

    // Make executable on Unix
    if (!binaryName.endsWith(".exe")) {
        chmodSync(cachedBinary, 0o755);
    }

    const size = statSync(cachedBinary).size / 1024 / 1024;
    console.log(`  Downloaded ${binaryName} (${size.toFixed(1)} MB)`);

    return cachedBinary;
}

function getCliLicensePath() {
    // Use license from node_modules (requires npm ci in nodejs/ first)
    const licensePath = join(repoRoot, "nodejs", "node_modules", "@github", "copilot", "LICENSE.md");
    if (!existsSync(licensePath)) {
        throw new Error(
            `CLI LICENSE.md not found at ${licensePath}. Run 'npm ci' in nodejs/ first.`
        );
    }
    return licensePath;
}

async function buildWheel(platform, pkgVersion, cliVersion, outputDir, licensePath) {
    const [wheelTag, binaryName] = PLATFORMS[platform];
    console.log(`\nBuilding wheel for ${platform}...`);

    // Cache directory includes version
    const cacheDir = join(pythonDir, ".cli-cache", cliVersion, platform);

    // Download/get cached binary
    const binaryPath = await downloadCliBinary(platform, cliVersion, cacheDir);

    // Create temp build directory
    const buildDir = join(pythonDir, ".build-temp", platform);
    if (existsSync(buildDir)) {
        rmSync(buildDir, { recursive: true });
    }
    mkdirSync(buildDir, { recursive: true });

    // Copy package source
    const pkgDir = join(buildDir, "copilot");
    cpSync(join(pythonDir, "copilot"), pkgDir, { recursive: true });

    // Create bin directory and copy binary
    const binDir = join(pkgDir, "bin");
    mkdirSync(binDir, { recursive: true });
    cpSync(binaryPath, join(binDir, binaryName));

    // Create VERSION file
    writeFileSync(join(binDir, "VERSION"), cliVersion);

    // Create __init__.py
    writeFileSync(join(binDir, "__init__.py"), '"""Bundled Copilot CLI binary."""\n');

    // Copy and modify pyproject.toml - replace license reference with file
    let pyprojectContent = readFileSync(join(pythonDir, "pyproject.toml"), "utf-8");

    // Replace the license specification with file reference
    pyprojectContent = pyprojectContent.replace(
        'license = {text = "MIT"}',
        'license = {file = "CLI-LICENSE.md"}'
    );

    // Add package-data configuration
    const packageDataConfig = `
[tool.setuptools.package-data]
"copilot.bin" = ["*"]
`;
    pyprojectContent = pyprojectContent.replace("\n[tool.ruff]", `${packageDataConfig}\n[tool.ruff]`);
    writeFileSync(join(buildDir, "pyproject.toml"), pyprojectContent);

    // Copy README
    if (existsSync(join(pythonDir, "README.md"))) {
        cpSync(join(pythonDir, "README.md"), join(buildDir, "README.md"));
    }

    // Copy CLI LICENSE
    cpSync(licensePath, join(buildDir, "CLI-LICENSE.md"));

    // Build wheel using uv (faster and doesn't require build package to be installed)
    const distDir = join(buildDir, "dist");
    execSync("uv build --wheel", {
        cwd: buildDir,
        stdio: "inherit",
    });

    // Find built wheel
    const wheels = readdirSync(distDir).filter((f) => f.endsWith(".whl"));
    if (wheels.length === 0) {
        throw new Error("No wheel found after build");
    }

    const srcWheel = join(distDir, wheels[0]);
    const newName = wheels[0].replace("-py3-none-any.whl", `-py3-none-${wheelTag}.whl`);
    const destWheel = join(outputDir, newName);

    // Repack wheel with correct platform tag
    await repackWheelWithPlatform(srcWheel, destWheel, wheelTag);

    // Clean up build dir
    rmSync(buildDir, { recursive: true });

    const size = statSync(destWheel).size / 1024 / 1024;
    console.log(`  Built ${newName} (${size.toFixed(1)} MB)`);

    return destWheel;
}

async function repackWheelWithPlatform(srcWheel, destWheel, platformTag) {
    // Write Python script to temp file to avoid shell escaping issues
    const script = `
import sys
import zipfile
import tempfile
from pathlib import Path

src_wheel = Path(sys.argv[1])
dest_wheel = Path(sys.argv[2])
platform_tag = sys.argv[3]

with tempfile.TemporaryDirectory() as tmpdir:
    tmpdir = Path(tmpdir)
    
    # Extract wheel
    with zipfile.ZipFile(src_wheel, 'r') as zf:
        zf.extractall(tmpdir)
    
    # Find and update WHEEL file
    wheel_info_dirs = list(tmpdir.glob('*.dist-info'))
    if not wheel_info_dirs:
        raise RuntimeError('No .dist-info directory found in wheel')
    
    wheel_info_dir = wheel_info_dirs[0]
    wheel_file = wheel_info_dir / 'WHEEL'
    
    with open(wheel_file) as f:
        wheel_content = f.read()
    
    wheel_content = wheel_content.replace('Tag: py3-none-any', f'Tag: py3-none-{platform_tag}')
    
    with open(wheel_file, 'w') as f:
        f.write(wheel_content)
    
    # Regenerate RECORD file
    record_file = wheel_info_dir / 'RECORD'
    records = []
    for path in tmpdir.rglob('*'):
        if path.is_file() and path.name != 'RECORD':
            rel_path = path.relative_to(tmpdir)
            records.append(f'{rel_path},,')
    records.append(f'{wheel_info_dir.name}/RECORD,,')
    
    with open(record_file, 'w') as f:
        f.write('\\n'.join(records))
    
    # Create new wheel
    dest_wheel.parent.mkdir(parents=True, exist_ok=True)
    if dest_wheel.exists():
        dest_wheel.unlink()
    
    with zipfile.ZipFile(dest_wheel, 'w', zipfile.ZIP_DEFLATED) as zf:
        for path in tmpdir.rglob('*'):
            if path.is_file():
                zf.write(path, path.relative_to(tmpdir))
`;

    // Write script to temp file
    const scriptPath = join(pythonDir, ".build-temp", "repack_wheel.py");
    mkdirSync(dirname(scriptPath), { recursive: true });
    writeFileSync(scriptPath, script);

    try {
        execSync(`python "${scriptPath}" "${srcWheel}" "${destWheel}" "${platformTag}"`, {
            stdio: "inherit",
        });
    } finally {
        // Clean up script
        rmSync(scriptPath);
    }
}

async function main() {
    const args = process.argv.slice(2);
    let platform = null;
    let outputDir = join(pythonDir, "dist");

    // Parse args
    for (let i = 0; i < args.length; i++) {
        if (args[i] === "--platform" && args[i + 1]) {
            platform = args[++i];
            if (!PLATFORMS[platform]) {
                console.error(`Invalid platform: ${platform}`);
                console.error(`Valid platforms: ${Object.keys(PLATFORMS).join(", ")}`);
                process.exit(1);
            }
        } else if (args[i] === "--output-dir" && args[i + 1]) {
            outputDir = args[++i];
        }
    }

    const cliVersion = getCliVersion();
    const pkgVersion = getPkgVersion();

    console.log(`CLI version: ${cliVersion}`);
    console.log(`Package version: ${pkgVersion}`);

    mkdirSync(outputDir, { recursive: true });

    // Get CLI license from node_modules
    const licensePath = getCliLicensePath();

    const platforms = platform ? [platform] : Object.keys(PLATFORMS);
    const wheels = [];

    for (const p of platforms) {
        try {
            const wheel = await buildWheel(p, pkgVersion, cliVersion, outputDir, licensePath);
            wheels.push(wheel);
        } catch (e) {
            console.error(`Error building wheel for ${p}:`, e.message);
            if (platform) {
                process.exit(1);
            }
        }
    }

    console.log(`\nBuilt ${wheels.length} wheel(s):`);
    for (const wheel of wheels) {
        console.log(`  ${wheel}`);
    }
}

main().catch((e) => {
    console.error(e);
    process.exit(1);
});
