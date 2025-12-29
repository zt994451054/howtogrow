import fs from "fs";
import childProcess from "child_process";

function safeRm(path) {
  try {
    if (!fs.existsSync(path)) return;
    const stat = fs.lstatSync(path);
    if (stat.isDirectory()) {
      // Node 14 compatibility: use rmdirSync(recursive) instead of rmSync.
      fs.rmdirSync(path, { recursive: true });
      return;
    }
    fs.unlinkSync(path);
  } catch {
    // ignore
  }
}

safeRm("node_modules");

const res = childProcess.spawnSync("npm", ["install"], { stdio: "inherit", shell: process.platform === "win32" });
process.exit(res.status ?? 1);
