<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { useAuthStore } from "@/stores/auth";
import { usePermissionStore } from "@/stores/permission";
import { routes } from "@/router/routes";

type MenuItem = {
  path: string;
  title: string;
  permission: string;
};

type MenuGroup = {
  permission: string;
  title: string;
  items: MenuItem[];
};

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const permission = usePermissionStore();

const menuGroups = computed<MenuGroup[]>(() => {
  const root = routes.find((r) => r.path === "/");
  const children = root && "children" in root ? (root.children ?? []) : [];

  const groups = new Map<string, MenuGroup>();

  for (const r of children) {
    if (typeof r.path !== "string" || r.path.length === 0) continue;
    const permissionCode = typeof r.meta?.permission === "string" ? r.meta.permission : undefined;
    if (!permissionCode) continue;
    if (!permission.hasPermission(permissionCode)) continue;

    const groupTitle = permission.permissionNameMap[permissionCode] ?? permissionCode;
    const group = groups.get(permissionCode) ?? { permission: permissionCode, title: groupTitle, items: [] };
    const itemTitle = (r.meta?.title as string | undefined) ?? r.path;
    group.items.push({ path: `/${r.path}`, title: itemTitle, permission: permissionCode });
    groups.set(permissionCode, group);
  }

  return Array.from(groups.values());
});

const activeMenu = computed(() => route.path);

async function logout() {
  auth.clear();
  ElMessage.success("已退出登录");
  await router.replace({ name: "login" });
}
</script>

<template>
  <div class="layout">
    <aside class="sider">
      <div class="brand">HowToGrow</div>
      <nav class="menu">
        <div v-for="group in menuGroups" :key="group.permission" class="menu-group">
          <div class="menu-group-title">{{ group.title }}</div>
          <a
            v-for="item in group.items"
            :key="item.path"
            class="menu-item"
            :class="{ active: activeMenu === item.path }"
            @click.prevent="router.push(item.path)"
          >
            {{ item.title }}
          </a>
        </div>
      </nav>
    </aside>
    <main class="main">
      <header class="header">
        <div class="me">
          <span class="username">{{ permission.me?.username }}</span>
          <a class="logout" @click.prevent="logout">退出</a>
        </div>
      </header>
      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<style scoped>
.layout {
  height: 100%;
  display: grid;
  grid-template-columns: 220px 1fr;
}

.sider {
  background: #001529;
  color: #fff;
  padding: 12px;
}

.brand {
  font-weight: 700;
  padding: 10px 8px 14px;
}

.menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-bottom: 10px;
}

.menu-group-title {
  padding: 10px 8px 2px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.menu-item {
  color: rgba(255, 255, 255, 0.82);
  text-decoration: none;
  padding: 8px 10px;
  border-radius: 6px;
}

.menu-item.active,
.menu-item:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header {
  height: 52px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 16px;
}

.me {
  display: flex;
  gap: 10px;
  align-items: center;
}

.username {
  color: #303133;
}

.logout {
  color: #409eff;
  cursor: pointer;
  text-decoration: none;
}

.content {
  padding: 16px;
  overflow: auto;
}
</style>
