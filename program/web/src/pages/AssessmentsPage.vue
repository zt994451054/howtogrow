<script setup lang="ts">
import { onMounted, ref } from "vue";
import { listAssessments, type AssessmentView } from "@/api/admin/assessments";
import { getApiBaseUrl } from "@/config/runtimeConfig";
import { formatDateTime } from "@/utils/format";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

const loading = ref(false);
const items = ref<AssessmentView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });

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

async function reload() {
  loading.value = true;
  try {
    const res = await listAssessments({ page: page.value.page, pageSize: page.value.pageSize });
    items.value = res.items;
    page.value.total = res.total;
  } finally {
    loading.value = false;
  }
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
      </div>
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
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
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
