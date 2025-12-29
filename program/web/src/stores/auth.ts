import { defineStore } from "pinia";

const STORAGE_KEY = "howtogrow.admin.jwt";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem(STORAGE_KEY) ?? ""
  }),
  actions: {
    setToken(token: string) {
      this.token = token;
      localStorage.setItem(STORAGE_KEY, token);
    },
    clear() {
      this.token = "";
      localStorage.removeItem(STORAGE_KEY);
    }
  }
});

