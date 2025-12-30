<script setup lang="ts">
import { onMounted, ref } from "vue";
import { listAssessments, type AssessmentView } from "@/api/admin/assessments";
import { formatDateTime } from "@/utils/format";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

const loading = ref(false);
const items = ref<AssessmentView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });

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
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="userId" label="用户ID" width="90" align="center" />
      <el-table-column prop="userNickname" label="用户昵称" width="140" show-overflow-tooltip />
      <el-table-column prop="childId" label="孩子ID" width="90" align="center" />
      <el-table-column prop="childNickname" label="孩子昵称" width="140" show-overflow-tooltip />
      <el-table-column label="开始时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.startedAt) }}</template>
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
</style>
