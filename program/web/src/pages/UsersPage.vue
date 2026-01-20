<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { extendUserSubscription, listUsers, type UserListParams, type UserView } from "@/api/admin/users";
import { formatDateTime } from "@/utils/format";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

type FilterState = {
  userId?: number;
  keyword: string;
  freeTrialUsed?: boolean;
  subscriptionStatus?: "ACTIVE" | "EXPIRED" | "NONE";
};

const loading = ref(false);
const items = ref<UserView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });
const filters = reactive<FilterState>({
  userId: undefined,
  keyword: "",
  freeTrialUsed: undefined,
  subscriptionStatus: undefined
});

const extendDialogVisible = ref(false);
const extendingUser = ref<UserView | null>(null);
const extendDays = ref(30);

const extendPreviewEndAt = computed(() => {
  if (!extendingUser.value) return "-";
  const now = Date.now();
  const current = extendingUser.value.subscriptionEndAt
    ? new Date(extendingUser.value.subscriptionEndAt).getTime()
    : NaN;
  const base = Number.isFinite(current) && current > now ? current : now;
  const to = base + extendDays.value * 24 * 60 * 60 * 1000;
  return formatDateTime(new Date(to).toISOString());
});

async function reload() {
  loading.value = true;
  try {
    const params: UserListParams = {
      page: page.value.page,
      pageSize: page.value.pageSize
    };
    if (Number.isFinite(filters.userId) && (filters.userId ?? 0) > 0) {
      params.userId = filters.userId;
    }
    const keyword = filters.keyword.trim();
    if (keyword) params.keyword = keyword;
    if (filters.freeTrialUsed !== undefined) params.freeTrialUsed = filters.freeTrialUsed;
    if (filters.subscriptionStatus) params.subscriptionStatus = filters.subscriptionStatus;

    const res = await listUsers(params);
    items.value = res.items;
    page.value.total = res.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  page.value.page = 1;
  void reload();
}

function resetFilters() {
  filters.userId = undefined;
  filters.keyword = "";
  filters.freeTrialUsed = undefined;
  filters.subscriptionStatus = undefined;
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

function openExtend(row: UserView) {
  extendingUser.value = row;
  extendDays.value = 30;
  extendDialogVisible.value = true;
}

async function confirmExtend() {
  if (!extendingUser.value) return;

  const res = await extendUserSubscription(extendingUser.value.userId, { days: extendDays.value });
  ElMessage.success(`已延长，新的订阅到期时间：${formatDateTime(res.subscriptionEndAt)}`);

  extendDialogVisible.value = false;
  extendingUser.value = null;
  await reload();
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>用户</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
      </div>
    </div>

    <div class="filters">
      <el-form inline label-width="80px" @submit.prevent>
        <el-form-item label="用户ID">
          <el-input-number v-model="filters.userId" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="filters.keyword"
            clearable
            placeholder="昵称 / openid"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="免费体验">
          <el-select v-model="filters.freeTrialUsed" clearable placeholder="全部" style="width: 120px">
            <el-option label="是" :value="true" />
            <el-option label="否" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="订阅状态">
          <el-select v-model="filters.subscriptionStatus" clearable placeholder="全部" style="width: 140px">
            <el-option label="有效" value="ACTIVE" />
            <el-option label="已过期" value="EXPIRED" />
            <el-option label="未订阅" value="NONE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%" table-layout="fixed">
      <el-table-column prop="userId" label="ID" width="90" align="center" />
      <el-table-column label="头像" width="70" align="center">
        <template #default="{ row }">
          <el-avatar :size="32" :src="row.avatarUrl || undefined">
            {{ String(row.nickname || row.userId).slice(0, 1) }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="昵称" width="160" show-overflow-tooltip />
      <el-table-column prop="wechatOpenid" label="openid" min-width="240" show-overflow-tooltip />
      <el-table-column prop="freeTrialUsed" label="免费体验" width="90" align="center">
        <template #default="{ row }">{{ row.freeTrialUsed ? "是" : "否" }}</template>
      </el-table-column>
      <el-table-column label="订阅到期" width="180">
        <template #default="{ row }">{{ formatDateTime(row.subscriptionEndAt) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openExtend(row)">延长订阅</el-button>
        </template>
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

    <el-dialog v-model="extendDialogVisible" title="手动延长订阅" width="520px">
      <div v-if="extendingUser" class="extend-body">
        <div class="extend-row">用户ID：{{ extendingUser.userId }}</div>
        <div class="extend-row">当前到期：{{ formatDateTime(extendingUser.subscriptionEndAt) }}</div>
        <div class="extend-row">延长天数：</div>
        <el-input-number v-model="extendDays" :min="1" :max="3650" :step="1" />
        <div class="extend-row">预计到期：{{ extendPreviewEndAt }}</div>
      </div>
      <template #footer>
        <el-button @click="extendDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmExtend">确认延长</el-button>
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
.filters {
  margin-bottom: 12px;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
.extend-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.extend-row {
  color: #303133;
}
</style>
