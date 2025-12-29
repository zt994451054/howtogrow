import type { AppRuntimeConfig } from "@/types/runtime-config";

export function getRuntimeConfig(): AppRuntimeConfig {
  const cfg = window.__APP_CONFIG__;
  if (!cfg || !cfg.apiBaseUrl) {
    throw new Error("Missing runtime config: window.__APP_CONFIG__.apiBaseUrl");
  }
  return cfg;
}

export function getApiBaseUrl(): string {
  const baseUrl = getRuntimeConfig().apiBaseUrl.trim();
  return baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
}

