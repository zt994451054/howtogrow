export type WebEnv = "dev" | "test" | "prod";

export type AppRuntimeConfig = {
  env: WebEnv;
  apiBaseUrlMap?: Record<string, string>;
  apiBaseUrl: string;
};

declare global {
  interface Window {
    __APP_CONFIG__?: AppRuntimeConfig;
  }
}

