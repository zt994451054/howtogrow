<script setup lang="ts">
import { onMounted, ref } from "vue";
import { listPermissions, type PermissionView } from "@/api/admin/rbac";

const loading = ref(false);
const items = ref<PermissionView[]>([]);

async function load() {
  loading.value = true;
  try {
    items.value = await listPermissions();
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void load();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>权限列表</h3>
      <el-button v-permission="'RBAC:MANAGE'" @click="load">刷新</el-button>
    </div>
    <el-table :data="items" v-loading="loading" style="width: 100%">
      <el-table-column prop="permissionId" label="ID" width="90" />
      <el-table-column prop="code" label="Code" />
      <el-table-column prop="name" label="名称" />
    </el-table>
  </div>
</template>

<style scoped>
.page {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>

