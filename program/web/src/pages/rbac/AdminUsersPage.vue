<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  createAdminUser,
  listAdminUsers,
  listRoles,
  updateAdminUserRoles,
  type AdminUserCreateRequest,
  type AdminUserView,
  type RoleView
} from "@/api/admin/rbac";

const loading = ref(false);
const users = ref<AdminUserView[]>([]);
const roles = ref<RoleView[]>([]);
const roleNameMap = computed(() => Object.fromEntries(roles.value.map((r) => [r.code, r.name])));

const createVisible = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = reactive<AdminUserCreateRequest>({ username: "", password: "", roleCodes: [] });
const createRules: FormRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入初始密码", trigger: "blur" }]
};

const roleVisible = ref(false);
const roleSaving = ref(false);
const currentUser = ref<AdminUserView | null>(null);
const selectedRoleCodes = ref<string[]>([]);

async function reload() {
  loading.value = true;
  try {
    const [u, r] = await Promise.all([listAdminUsers(), listRoles()]);
    users.value = u;
    roles.value = r;
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  createForm.username = "";
  createForm.password = "";
  createForm.roleCodes = [];
  createVisible.value = true;
}

async function submitCreate() {
  const ok = await createFormRef.value?.validate();
  if (!ok) return;
  await createAdminUser({
    username: createForm.username.trim(),
    password: createForm.password,
    roleCodes: createForm.roleCodes
  });
  ElMessage.success("已创建");
  createVisible.value = false;
  await reload();
}

function openRoles(user: AdminUserView) {
  currentUser.value = user;
  selectedRoleCodes.value = [...user.roleCodes];
  roleVisible.value = true;
}

async function saveRoles() {
  if (!currentUser.value) return;
  roleSaving.value = true;
  try {
    await updateAdminUserRoles(currentUser.value.adminUserId, { roleCodes: selectedRoleCodes.value });
    ElMessage.success("已保存");
    roleVisible.value = false;
    await reload();
  } finally {
    roleSaving.value = false;
  }
}

async function warnNoDelete() {
  await ElMessageBox.alert("当前 API.md 未提供删除管理员接口，暂不支持删除。", "提示", { type: "info" });
}

function renderRoleName(code: string): string {
  return roleNameMap.value[code] ?? code;
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>管理员</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <el-table :data="users" v-loading="loading" style="width: 100%">
      <el-table-column prop="adminUserId" label="ID" width="90" />
      <el-table-column prop="username" label="用户名" width="180" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">{{ row.status === 1 ? "启用" : "禁用" }}</template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="220" />
      <el-table-column label="角色">
        <template #default="{ row }">
          <el-tag v-for="c in row.roleCodes" :key="c" size="small" style="margin-right: 6px; margin-bottom: 6px">
            {{ renderRoleName(c) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRoles(row)">编辑角色</el-button>
          <el-button link type="danger" @click="warnNoDelete">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createVisible" title="新增管理员" width="520px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="初始密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.roleCodes" multiple filterable style="width: 100%">
            <el-option v-for="r in roles" :key="r.code" :label="`${r.name}（${r.code}）`" :value="r.code" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleVisible" title="编辑管理员角色" width="720px">
      <div style="margin-bottom: 10px">
        <el-text type="info">管理员：{{ currentUser?.username }}</el-text>
      </div>
      <el-select v-model="selectedRoleCodes" multiple filterable style="width: 100%" placeholder="选择角色">
        <el-option v-for="r in roles" :key="r.code" :label="`${r.name}（${r.code}）`" :value="r.code" />
      </el-select>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleSaving" @click="saveRoles">保存</el-button>
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

