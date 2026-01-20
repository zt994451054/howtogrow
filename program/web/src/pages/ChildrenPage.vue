<script setup lang="ts">
import { ElMessage } from "element-plus";
import { computed, onMounted, reactive, ref } from "vue";
import { listChildren, type AdminChildView } from "@/api/admin/children";
import { getApiBaseUrl } from "@/config/runtimeConfig";
import { formatDateTime } from "@/utils/format";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

type Filters = {
  userId: string;
  userNickname: string;
  childId: string;
  childNickname: string;
  gender: "" | "0" | "1" | "2";
  ageMin: number | null;
  ageMax: number | null;
  status: "" | "0" | "1";
};

const loading = ref(false);
const items = ref<AdminChildView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });

const filters = reactive<Filters>({
  userId: "",
  userNickname: "",
  childId: "",
  childNickname: "",
  gender: "",
  ageMin: null,
  ageMax: null,
  status: "1"
});

function resolveAvatarUrl(url: string | null | undefined): string | undefined {
  const raw = (url || "").trim();
  if (!raw) return undefined;
  if (/^https?:\/\//i.test(raw) || raw.startsWith("data:")) return raw;
  const base = getApiBaseUrl().replace(/\/+$/, "");
  const path = raw.startsWith("/") ? raw : `/${raw}`;
  return `${base}${path}`;
}

function toOptionalNumber(value: string): number | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;
  const n = Number(trimmed);
  return Number.isFinite(n) && n > 0 ? n : undefined;
}

const requestParams = computed(() => ({
  page: page.value.page,
  pageSize: page.value.pageSize,
  userId: toOptionalNumber(filters.userId),
  userNickname: filters.userNickname.trim() || undefined,
  childId: toOptionalNumber(filters.childId),
  childNickname: filters.childNickname.trim() || undefined,
  gender: filters.gender === "" ? undefined : Number(filters.gender),
  ageMin: filters.ageMin ?? undefined,
  ageMax: filters.ageMax ?? undefined,
  status: filters.status === "" ? undefined : Number(filters.status)
}));

function validateAgeRange(): boolean {
  if (filters.ageMin == null || filters.ageMax == null) return true;
  if (filters.ageMin <= filters.ageMax) return true;
  ElMessage.error("年龄范围不合法：下限不能大于上限");
  return false;
}

async function reload() {
  if (!validateAgeRange()) return;
  loading.value = true;
  try {
    const res = await listChildren(requestParams.value);
    items.value = res.items;
    page.value.total = res.total;
  } finally {
    loading.value = false;
  }
}

function onSearch() {
  if (!validateAgeRange()) return;
  page.value.page = 1;
  void reload();
}

function onReset() {
  filters.userId = "";
  filters.userNickname = "";
  filters.childId = "";
  filters.childNickname = "";
  filters.gender = "";
  filters.ageMin = null;
  filters.ageMax = null;
  filters.status = "1";
  page.value.page = 1;
  void reload();
}

function onSizeChange(size: number) {
  page.value.pageSize = size;
  page.value.page = 1;
  void reload();
}

function onCurrentChange(p: number) {
  page.value.page = p;
  void reload();
}

function genderLabel(gender: number): string {
  if (gender === 1) return "男";
  if (gender === 2) return "女";
  return "未知";
}

function statusLabel(status: number): string {
  return status === 1 ? "启用" : "删除";
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>孩子</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
      </div>
    </div>

    <div class="filters">
      <el-form :inline="true" label-width="90px">
        <el-form-item label="用户ID">
          <el-input v-model="filters.userId" placeholder="精确匹配" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="用户昵称">
          <el-input v-model="filters.userNickname" placeholder="模糊匹配" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="孩子ID">
          <el-input v-model="filters.childId" placeholder="精确匹配" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="孩子昵称">
          <el-input v-model="filters.childNickname" placeholder="模糊匹配" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="filters.gender" placeholder="全部" clearable style="width: 140px">
            <el-option label="未知" value="0" />
            <el-option label="男" value="1" />
            <el-option label="女" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄范围">
          <div class="age-range">
            <el-input-number
              v-model="filters.ageMin"
              :min="0"
              :max="18"
              :step="1"
              step-strictly
              controls-position="right"
              style="width: 120px"
            />
            <span class="age-range__sep">-</span>
            <el-input-number
              v-model="filters.ageMax"
              :min="0"
              :max="18"
              :step="1"
              step-strictly
              controls-position="right"
              style="width: 120px"
            />
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="启用" value="1" />
            <el-option label="删除" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">筛选</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%" table-layout="fixed">
      <el-table-column prop="childId" label="孩子ID" width="100" align="center" />
      <el-table-column label="所属用户" width="260">
        <template #default="{ row }">
          <div class="user">
            <el-avatar :size="28" :src="resolveAvatarUrl(row.userAvatarUrl)">
              {{ (row.userNickname || `#${row.userId}`).slice(0, 1) }}
            </el-avatar>
            <div class="user__meta">
              <div class="user__name">{{ row.userNickname || `用户 #${row.userId}` }}</div>
              <div class="user__id">ID: {{ row.userId }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="childNickname" label="孩子昵称" min-width="180" show-overflow-tooltip />
      <el-table-column label="性别" width="90" align="center">
        <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
      </el-table-column>
      <el-table-column prop="birthDate" label="出生日期" width="130" align="center" />
      <el-table-column prop="ageYear" label="年龄(岁)" width="90" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        :current-page="page.page"
        :page-size="page.pageSize"
        :total="page.total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        @size-change="onSizeChange"
        @current-change="onCurrentChange"
      />
    </div>
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
.filters {
  margin-bottom: 12px;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

.age-range {
  display: flex;
  align-items: center;
  gap: 8px;
}
.age-range__sep {
  color: #9aa4b2;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user__meta {
  min-width: 0;
}
.user__name {
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user__id {
  font-size: 12px;
  color: #9aa4b2;
  line-height: 16px;
}
</style>
