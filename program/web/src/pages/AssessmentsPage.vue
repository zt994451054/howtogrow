<script setup lang="ts">
import { onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { exportAssessmentsExcel, listAssessments, type AssessmentListParams, type AssessmentView } from "@/api/admin/assessments";
import { listChildren, type AdminChildView } from "@/api/admin/children";
import { listUsers, type UserView } from "@/api/admin/users";
import { getApiBaseUrl } from "@/config/runtimeConfig";
import { downloadBlob } from "@/utils/download";
import { formatDateTime } from "@/utils/format";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

type FilterState = {
  bizDateRange: [string, string] | null;
  userId?: number;
  childId?: number;
  keyword: string;
};

type UserOption = {
  userId: number;
  nickname: string | null;
  avatarUrl: string | null;
};

type ChildOption = {
  childId: number;
  childNickname: string;
  userId: number;
  userNickname: string | null;
};

const loading = ref(false);
const exporting = ref(false);
const items = ref<AssessmentView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });
const filters = reactive<FilterState>({
  bizDateRange: null,
  userId: undefined,
  childId: undefined,
  keyword: ""
});

const userSearching = ref(false);
const userOptions = ref<UserOption[]>([]);
let userSearchSeq = 0;

const childSearching = ref(false);
const childOptions = ref<ChildOption[]>([]);
let childSearchSeq = 0;

const DIM_SHORT: Record<string, string> = {
  EMOTION_MANAGEMENT: "情绪",
  COMMUNICATION_EXPRESSION: "沟通",
  RULE_GUIDANCE: "规则",
  RELATIONSHIP_BUILDING: "关系",
  LEARNING_SUPPORT: "学习",
};

function resolveAvatarUrl(url: string | null | undefined): string | undefined {
  const raw = (url || "").trim();
  if (!raw) return undefined;
  if (/^https?:\/\//i.test(raw) || raw.startsWith("data:")) return raw;
  const base = getApiBaseUrl().replace(/\/+$/, "");
  const path = raw.startsWith("/") ? raw : `/${raw}`;
  return `${base}${path}`;
}

function buildQueryParams(): Omit<AssessmentListParams, "page" | "pageSize"> {
  const params: Omit<AssessmentListParams, "page" | "pageSize"> = {};
  if (filters.bizDateRange?.[0]) params.bizDateFrom = filters.bizDateRange[0];
  if (filters.bizDateRange?.[1]) params.bizDateTo = filters.bizDateRange[1];
  if (Number.isFinite(filters.userId) && (filters.userId ?? 0) > 0) params.userId = filters.userId;
  if (Number.isFinite(filters.childId) && (filters.childId ?? 0) > 0) params.childId = filters.childId;
  const keyword = filters.keyword.trim();
  if (keyword) params.keyword = keyword;
  return params;
}

async function reload() {
  loading.value = true;
  try {
    const res = await listAssessments({
      page: page.value.page,
      pageSize: page.value.pageSize,
      ...buildQueryParams()
    });
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
  filters.bizDateRange = null;
  filters.userId = undefined;
  filters.childId = undefined;
  filters.keyword = "";
  page.value.page = 1;
  void reload();
}

function buildExportFilename(): string {
  const from = filters.bizDateRange?.[0];
  const to = filters.bizDateRange?.[1];
  if (from && to) return `自测记录_${from}_${to}.xlsx`;
  if (from) return `自测记录_${from}.xlsx`;
  if (to) return `自测记录_截至${to}.xlsx`;
  return "自测记录.xlsx";
}

async function exportExcel() {
  exporting.value = true;
  try {
    const { blob, filename } = await exportAssessmentsExcel(buildQueryParams());
    downloadBlob(blob, filename || buildExportFilename());
    ElMessage.success("已开始下载");
  } finally {
    exporting.value = false;
  }
}

function toUserOption(u: UserView): UserOption {
  return { userId: u.userId, nickname: u.nickname, avatarUrl: u.avatarUrl };
}

function toChildOption(c: AdminChildView): ChildOption {
  return { childId: c.childId, childNickname: c.childNickname, userId: c.userId, userNickname: c.userNickname };
}

async function searchUsers(query: string) {
  const q = query.trim();
  if (!q) {
    userOptions.value = [];
    return;
  }

  userSearching.value = true;
  const seq = ++userSearchSeq;
  try {
    const res = await listUsers({ page: 1, pageSize: 20, keyword: q });
    if (seq !== userSearchSeq) return;
    userOptions.value = res.items.map(toUserOption);
  } finally {
    if (seq === userSearchSeq) userSearching.value = false;
  }
}

async function loadChildren(query: string) {
  const q = query.trim();

  // If neither user selected nor keyword provided, don't query to avoid returning a huge arbitrary list.
  if (!filters.userId && !q) {
    childOptions.value = [];
    return;
  }

  childSearching.value = true;
  const seq = ++childSearchSeq;
  try {
    const res = await listChildren({
      page: 1,
      pageSize: 20,
      userId: filters.userId,
      childNickname: q || undefined
    });
    if (seq !== childSearchSeq) return;
    childOptions.value = res.items.map(toChildOption);
  } finally {
    if (seq === childSearchSeq) childSearching.value = false;
  }
}

function onChildDropdownVisible(visible: boolean) {
  if (!visible) return;
  if (!filters.userId) {
    childOptions.value = [];
    return;
  }
  // If user already selected, show their children without requiring typing.
  if (filters.userId) {
    void loadChildren("");
  }
}

async function ensureUserOption(userId: number | undefined) {
  if (!userId) return;
  if (userOptions.value.some((u) => u.userId === userId)) return;
  const res = await listUsers({ page: 1, pageSize: 1, userId });
  const u = res.items?.[0];
  if (u) userOptions.value = [toUserOption(u), ...userOptions.value];
}

async function ensureChildOption(childId: number | undefined) {
  if (!childId) return;
  if (childOptions.value.some((c) => c.childId === childId)) return;
  const res = await listChildren({ page: 1, pageSize: 1, childId });
  const c = res.items?.[0];
  if (c) childOptions.value = [toChildOption(c), ...childOptions.value];
}

watch(
  () => filters.userId,
  () => {
    // childId is only meaningful within the selected user; reset to avoid conflicting filters.
    filters.childId = undefined;
    childOptions.value = [];
  }
);

function onSizeChange(size: number) {
  page.value.pageSize = size;
  page.value.page = 1;
  void reload();
}

function onCurrentChange(p: number) {
  page.value.page = p;
  void reload();
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>自测记录</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
        <el-button :loading="exporting" @click="exportExcel">导出Excel</el-button>
      </div>
    </div>

    <div class="filters">
      <el-form inline label-width="90px" @submit.prevent>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.bizDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="用户">
          <el-select
            v-model="filters.userId"
            clearable
            filterable
            remote
            :remote-method="searchUsers"
            :loading="userSearching"
            placeholder="输入昵称 / openid 搜索"
            style="width: 260px"
            @change="(v) => ensureUserOption(v as number | undefined)"
          >
            <el-option
              v-for="u in userOptions"
              :key="u.userId"
              :label="u.nickname ? `${u.nickname} (#${u.userId})` : `用户 #${u.userId}`"
              :value="u.userId"
            >
              <div class="select-option">
                <el-avatar :size="22" :src="resolveAvatarUrl(u.avatarUrl)" />
                <div class="select-option__text">
                  <div class="select-option__title">{{ u.nickname || `用户 #${u.userId}` }}</div>
                  <div class="select-option__sub">ID: {{ u.userId }}</div>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="孩子">
          <el-select
            v-model="filters.childId"
            clearable
            filterable
            remote
            :remote-method="loadChildren"
            :loading="childSearching"
            placeholder="先选用户，或输入孩子昵称搜索"
            style="width: 260px"
            @visible-change="onChildDropdownVisible"
            @change="(v) => ensureChildOption(v as number | undefined)"
          >
            <el-option
              v-for="c in childOptions"
              :key="c.childId"
              :label="`${c.childNickname} (#${c.childId})`"
              :value="c.childId"
            >
              <div class="select-option">
                <div class="select-option__text">
                  <div class="select-option__title">{{ c.childNickname }}（ID: {{ c.childId }}）</div>
                  <div class="select-option__sub">
                    用户：{{ c.userNickname || `#${c.userId}` }}（ID: {{ c.userId }}）
                  </div>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="昵称关键词">
          <el-input v-model="filters.keyword" clearable placeholder="用户/孩子昵称（模糊匹配）" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">筛选</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%" table-layout="fixed">
      <el-table-column prop="assessmentId" label="ID" width="80" align="center" />
      <el-table-column prop="bizDate" label="日期" width="110" />
      <el-table-column label="用户" width="220">
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
      <el-table-column label="孩子" width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="child">
            <div class="child__name">{{ row.childNickname || `孩子 #${row.childId}` }}</div>
            <div class="child__id">ID: {{ row.childId }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="5维度得分" min-width="340">
        <template #default="{ row }">
          <div class="dims">
            <el-tag
              v-for="d in row.dimensionScores"
              :key="d.dimensionCode"
              size="small"
              effect="light"
              :type="d.score > 0 ? 'success' : 'info'"
              class="dims__tag"
              :title="`${d.dimensionName}（${d.dimensionCode}）`"
            >
              {{ DIM_SHORT[d.dimensionCode] || d.dimensionName }} {{ d.score }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
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

.select-option {
  display: flex;
  align-items: center;
  gap: 8px;
}
.select-option__text {
  min-width: 0;
}
.select-option__title {
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.select-option__sub {
  font-size: 12px;
  color: #9aa4b2;
  line-height: 16px;
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

.child__name {
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.child__id {
  font-size: 12px;
  color: #9aa4b2;
  line-height: 16px;
}

.dims {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.dims__tag {
  margin: 0;
}
</style>
