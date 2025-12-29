import { fileURLToPath } from "url";
import vue from "@vitejs/plugin-vue";

export default {
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url))
    }
  },
  server: {
    port: 5173
  }
};
