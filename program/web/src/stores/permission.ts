import { defineStore } from "pinia";
import { adminMe } from "@/api/admin/auth";
import { listPermissions, type PermissionView } from "@/api/admin/rbac";

export type AdminMeView = {
  adminUserId: number;
  username: string;
  permissionCodes: string[];
};

export const usePermissionStore = defineStore("permission", {
  state: () => ({
    me: null as AdminMeView | null,
    permissions: [] as PermissionView[],
    permissionNameMap: {} as Record<string, string>,
    initialized: false
  }),
  actions: {
    async bootstrap() {
      const me = await adminMe();
      this.me = me;

      try {
        const permissions = await listPermissions();
        this.permissions = permissions;
        this.permissionNameMap = Object.fromEntries(permissions.map((p) => [p.code, p.name]));
      } catch {
        this.permissions = [];
        this.permissionNameMap = {};
      }

      this.initialized = true;
    },
    hasPermission(code: string): boolean {
      if (!this.me) return false;
      return this.me.permissionCodes.includes(code);
    }
  }
});
