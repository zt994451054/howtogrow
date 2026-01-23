<script setup lang="ts">
import { onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import {
  exportAssessmentsExcel,
  exportAssessmentWord,
  getAssessmentDetail,
  listAssessments,
  type AssessmentDetailItemView,
  type AssessmentDetailView,
  type AssessmentListParams,
  type AssessmentView
} from "@/api/admin/assessments";
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
const exportingWordId = ref<number | null>(null);
const items = ref<AssessmentView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });
const filters = reactive<FilterState>({
  bizDateRange: null,
  userId: undefined,
  childId: undefined,
  keyword: ""
});

const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<AssessmentDetailView | null>(null);
let detailSeq = 0;

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

function buildWordFilename(assessmentId: number, bizDate?: string | null): string {
  const date = (bizDate || "").trim();
  if (date) return `自测结果_${date}_${assessmentId}.docx`;
  return `自测结果_${assessmentId}.docx`;
}

async function exportWordById(assessmentId: number, fallbackFilename: string) {
  exportingWordId.value = assessmentId;
  try {
    const { blob, filename } = await exportAssessmentWord(assessmentId);
    downloadBlob(blob, filename || fallbackFilename);
    ElMessage.success("已开始下载");
  } finally {
    if (exportingWordId.value === assessmentId) exportingWordId.value = null;
  }
}

function exportWord(row: AssessmentView) {
  const filename = buildWordFilename(row.assessmentId, row.bizDate);
  void exportWordById(row.assessmentId, filename);
}

function exportWordFromDetail() {
  if (!detail.value) return;
  const filename = buildWordFilename(detail.value.assessmentId, detail.value.bizDate);
  void exportWordById(detail.value.assessmentId, filename);
}

async function openDetail(row: AssessmentView) {
  detailVisible.value = true;
  const seq = ++detailSeq;
  detailLoading.value = true;
  detail.value = null;
  try {
    const res = await getAssessmentDetail(row.assessmentId);
    if (seq !== detailSeq) return;
    detail.value = res;
  } catch (e) {
    if (seq !== detailSeq) return;
    ElMessage.error("加载详情失败");
  } finally {
    if (seq === detailSeq) detailLoading.value = false;
  }
}

function selectedOptions(item: AssessmentDetailItemView) {
  const selected = new Set(item.selectedOptionIds || []);
  return (item.options || []).filter((o) => selected.has(o.optionId));
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

watch(
  () => detailVisible.value,
  (v) => {
    if (v) return;
    detailSeq++;
    detail.value = null;
    detailLoading.value = false;
  }
);
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
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
          <el-button
            link
            :loading="exportingWordId === row.assessmentId"
            :disabled="exportingWordId !== null && exportingWordId !== row.assessmentId"
            @click="exportWord(row)"
          >
            导出Word
          </el-button>
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

    <el-drawer v-model="detailVisible" title="自测详情" size="720px" destroy-on-close>
      <div class="detail" v-loading="detailLoading">
        <template v-if="detail">
          <div class="detail-header">
            <div class="detail-user">
              <el-avatar :size="40" :src="resolveAvatarUrl(detail.userAvatarUrl)">
                {{ (detail.userNickname || `#${detail.userId}`).slice(0, 1) }}
              </el-avatar>
              <div class="detail-user__meta">
                <div class="detail-user__name">{{ detail.userNickname || `用户 #${detail.userId}` }}</div>
                <div class="detail-user__sub">
                  用户ID：{{ detail.userId }} · 孩子：{{ detail.childNickname || `孩子 #${detail.childId}` }}（ID: {{ detail.childId }}）
                </div>
              </div>
            </div>
            <div class="detail-actions">
              <el-button
                type="primary"
                :loading="exportingWordId === detail.assessmentId"
                :disabled="exportingWordId !== null && exportingWordId !== detail.assessmentId"
                @click="exportWordFromDetail"
              >
                导出Word
              </el-button>
            </div>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="自测ID">{{ detail.assessmentId }}</el-descriptions-item>
            <el-descriptions-item label="日期">{{ detail.bizDate }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatDateTime(detail.submittedAt) }}</el-descriptions-item>
            <el-descriptions-item label="题目数">{{ detail.items?.length || 0 }}</el-descriptions-item>
          </el-descriptions>

          <div class="section">
            <div class="section-title">维度得分</div>
            <div class="dims">
              <el-tag
                v-for="d in detail.dimensionScores"
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
          </div>

          <div class="section">
            <div class="section-title">AI 总结</div>
            <el-alert v-if="detail.aiSummary" :title="detail.aiSummary" type="success" :closable="false" show-icon />
            <el-text v-else type="info">暂无 AI 总结</el-text>
          </div>

          <div class="section">
            <div class="section-title">题目与作答</div>
            <div v-for="it in detail.items" :key="`${it.questionId}_${it.displayOrder}`" class="qa-item">
              <div class="qa-title">
                <div class="qa-no">Q{{ it.displayOrder }}</div>
                <el-tag size="small" effect="light" type="info">{{ it.questionType === "MULTI" ? "多选" : "单选" }}</el-tag>
              </div>
              <div class="qa-content">{{ it.questionContent }}</div>
              <div class="qa-answers">
                <div v-for="opt in selectedOptions(it)" :key="opt.optionId" class="qa-answer">
                  <el-tag size="small" :type="opt.suggestFlag === 1 ? 'success' : 'danger'">
                    {{ opt.suggestFlag === 1 ? "建议" : "不建议" }}
                  </el-tag>
                  <div class="qa-answer__body">
                    <div class="qa-answer__content">{{ opt.content }}</div>
                    <div v-if="opt.suggestFlag === 0 && opt.improvementTip" class="qa-answer__tip">
                      改进建议：{{ opt.improvementTip }}
                    </div>
                  </div>
                </div>
                <el-text v-if="selectedOptions(it).length === 0" type="info">未记录作答</el-text>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <el-empty description="请选择一条自测记录查看详情" />
        </template>
      </div>
    </el-drawer>
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

.detail {
  padding: 8px 4px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.detail-user {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.detail-user__meta {
  min-width: 0;
}

.detail-user__name {
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.detail-user__sub {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.section {
  margin-top: 14px;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.qa-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
}

.qa-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.qa-no {
  font-weight: 600;
}

.qa-content {
  line-height: 1.5;
  margin-bottom: 10px;
  color: #303133;
}

.qa-answers {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.qa-answer {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.qa-answer__body {
  flex: 1;
  min-width: 0;
}

.qa-answer__content {
  line-height: 1.5;
}

.qa-answer__tip {
  margin-top: 2px;
  color: #e6a23c;
  font-size: 12px;
  line-height: 1.4;
}
</style>
