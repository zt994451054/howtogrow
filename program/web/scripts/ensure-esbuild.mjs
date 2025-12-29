import fs from "fs";
import childProcess from "child_process";

function expectedEsbuildPackage() {
  const platform = process.platform;
  const arch = process.arch;

  if (platform === "darwin") {
    if (arch === "arm64") return "esbuild-darwin-arm64";
    if (arch === "x64") return "esbuild-darwin-64";
  }

  if (platform === "linux") {
    if (arch === "arm64") return "esbuild-linux-arm64";
    if (arch === "x64") return "esbuild-linux-64";
  }

  if (platform === "win32") {
    if (arch === "arm64") return "esbuild-windows-arm64";
    if (arch === "x64") return "esbuild-windows-64";
  }

  return null;
}

function hasInstalled(pkg) {
  return fs.existsSync(`node_modules/${pkg}/package.json`);
}

function reinstall() {
  const res = childProcess.spawnSync("npm", ["run", "reinstall"], {
    stdio: "inherit",
    shell: process.platform === "win32"
  });
  process.exit(res.status ?? 1);
}

const expected = expectedEsbuildPackage();
if (!expected) {
  process.exit(0);
}

if (hasInstalled(expected)) {
  process.exit(0);
}

// Platform/arch mismatch is the most common reason Vite fails to start.
// Auto-fix by reinstalling deps on the current platform.
console.log(`[preflight] esbuild platform mismatch; expected ${expected}. Reinstalling dependencies...`);
reinstall();
