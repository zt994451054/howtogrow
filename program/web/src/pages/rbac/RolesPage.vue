<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  createRole,
  listPermissions,
  listRoles,
  updateRolePermissions,
  type PermissionView,
  type RoleCreateRequest,
  type RoleView
} from "@/api/admin/rbac";

const loading = ref(false);
const roles = ref<RoleView[]>([]);
const permissions = ref<PermissionView[]>([]);
const permissionNameMap = computed(() => Object.fromEntries(permissions.value.map((p) => [p.code, p.name])));

const createVisible = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = reactive<RoleCreateRequest>({ code: "", name: "" });
const createRules: FormRules = {
  code: [{ required: true, message: "请输入角色编码", trigger: "blur" }],
  name: [{ required: true, message: "请输入角色名称", trigger: "blur" }]
};

const permVisible = ref(false);
const permSaving = ref(false);
const currentRole = ref<RoleView | null>(null);
const selectedPermissionCodes = ref<string[]>([]);

async function reload() {
  loading.value = true;
  try {
    const [r, p] = await Promise.all([listRoles(), listPermissions()]);
    roles.value = r;
    permissions.value = p;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  createForm.code = "";
  createForm.name = "";
  createVisible.value = true;
}

async function submitCreate() {
  const ok = await createFormRef.value?.validate();
  if (!ok) return;
  await createRole({ code: createForm.code.trim(), name: createForm.name.trim() });
  ElMessage.success("已创建");
  createVisible.value = false;
  await reload();
}

function openPermissions(role: RoleView) {
  currentRole.value = role;
  selectedPermissionCodes.value = [...role.permissionCodes];
  permVisible.value = true;
}

async function savePermissions() {
  if (!currentRole.value) return;
  permSaving.value = true;
  try {
    await updateRolePermissions(currentRole.value.roleId, { permissionCodes: selectedPermissionCodes.value });
    ElMessage.success("已保存");
    permVisible.value = false;
    await reload();
  } finally {
    permSaving.value = false;
  }
}

async function warnNoDelete() {
  await ElMessageBox.alert("当前 API.md 未提供删除角色接口，暂不支持删除。", "提示", { type: "info" });
}

function renderPermissionName(code: string): string {
  return permissionNameMap.value[code] ?? code;
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>角色</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <el-table :data="roles" v-loading="loading" style="width: 100%">
      <el-table-column prop="roleId" label="ID" width="90" />
      <el-table-column prop="code" label="编码" width="180" />
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column label="权限">
        <template #default="{ row }">
          <el-tag v-for="c in row.permissionCodes" :key="c" size="small" style="margin-right: 6px; margin-bottom: 6px">
            {{ renderPermissionName(c) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPermissions(row)">编辑权限</el-button>
          <el-button link type="danger" @click="warnNoDelete">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createVisible" title="新增角色" width="520px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="createForm.code" placeholder="如 OPERATOR" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="createForm.name" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permVisible" title="编辑角色权限" width="720px">
      <div style="margin-bottom: 10px">
        <el-text type="info">角色：{{ currentRole?.name }}（{{ currentRole?.code }}）</el-text>
      </div>
      <el-select v-model="selectedPermissionCodes" multiple filterable style="width: 100%" placeholder="选择权限">
        <el-option v-for="p in permissions" :key="p.code" :label="`${p.name}（${p.code}）`" :value="p.code" />
      </el-select>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="savePermissions">保存</el-button>
      </template>
    </el-dialog>
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
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.actions {
  display: flex;
  gap: 8px;
}
</style>

