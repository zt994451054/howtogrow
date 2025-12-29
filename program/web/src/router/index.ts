import { createRouter, createWebHistory, type NavigationGuardNext, type RouteLocationNormalized } from "vue-router";
import { routes } from "@/router/routes";
import { useAuthStore } from "@/stores/auth";
import { usePermissionStore } from "@/stores/permission";

export const router = createRouter({
  history: createWebHistory(),
  routes
});

async function ensurePermissionBootstrap(): Promise<void> {
  const permission = usePermissionStore();
  if (permission.initialized) return;
  await permission.bootstrap();
}

function requiresAuth(to: RouteLocationNormalized): boolean {
  return Boolean(to.meta.requiresAuth);
}

router.beforeEach(async (to, _from, next: NavigationGuardNext) => {
  const auth = useAuthStore();
  if (!requiresAuth(to)) {
    if (to.name === "login" && auth.token) {
      next({ path: "/" });
      return;
    }
    next();
    return;
  }

  if (!auth.token) {
    next({ name: "login" });
    return;
  }

  try {
    await ensurePermissionBootstrap();
  } catch {
    next({ name: "login" });
    return;
  }

  const permissionCode = typeof to.meta.permission === "string" ? to.meta.permission : undefined;
  if (permissionCode) {
    const permission = usePermissionStore();
    if (!permission.hasPermission(permissionCode)) {
      next({ name: "forbidden" });
      return;
    }
  }

  next();
});
