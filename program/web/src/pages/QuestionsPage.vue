<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import StatusTag from "@/components/StatusTag.vue";
import { listDimensions, type DimensionView } from "@/api/admin/dimensions";
import { listAllTroubleScenes, type TroubleSceneView } from "@/api/admin/trouble-scenes";
import {
  createQuestion,
  deleteQuestion,
  getQuestion,
  listQuestions,
  updateQuestion,
  type QuestionDetailView,
  type QuestionSummaryView,
  type QuestionUpsertRequest
} from "@/api/admin/questions";
import QuestionEditorDialog from "@/pages/questions/QuestionEditorDialog.vue";
import QuestionImportDialog from "@/pages/questions/QuestionImportDialog.vue";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

const loading = ref(false);
const items = ref<QuestionSummaryView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });

const dimensions = ref<DimensionView[]>([]);
const troubleScenes = ref<TroubleSceneView[]>([]);

const editorVisible = ref(false);
const editorTitle = ref("新增题目");
const editorInitial = ref<QuestionDetailView | null>(null);

const importVisible = ref(false);
const templateUrl = computed(() => `${import.meta.env.BASE_URL}question-import-template.xlsx`);

async function reload() {
  loading.value = true;
  try {
    const res = await listQuestions({ page: page.value.page, pageSize: page.value.pageSize });
    items.value = res.items;
    page.value.total = res.total;
  } finally {
    loading.value = false;
  }
}

async function loadReferenceData() {
  dimensions.value = await listDimensions();
  troubleScenes.value = await listAllTroubleScenes();
}

function openCreate() {
  editorTitle.value = "新增题目";
  editorInitial.value = null;
  editorVisible.value = true;
}

function openImport() {
  importVisible.value = true;
}

function questionTypeLabel(questionType: string) {
  const t = (questionType ?? "").toUpperCase();
  if (t === "SINGLE") return "单选";
  if (t === "MULTI") return "多选";
  return questionType;
}

function downloadTemplate() {
  const a = document.createElement("a");
  a.href = templateUrl.value;
  a.download = "题库导入模板.xlsx";
  document.body.appendChild(a);
  a.click();
  a.remove();
}

async function openEdit(row: QuestionSummaryView) {
  editorTitle.value = "编辑题目";
  editorVisible.value = true;
  editorInitial.value = await getQuestion(row.questionId);
}

async function onSubmit(payload: { request: QuestionUpsertRequest; questionId?: number }) {
  if (payload.questionId) {
    await updateQuestion(payload.questionId, payload.request);
    ElMessage.success("已保存");
  } else {
    await createQuestion(payload.request);
    ElMessage.success("已创建");
  }
  editorVisible.value = false;
  await reload();
}

async function remove(row: QuestionSummaryView) {
  await ElMessageBox.confirm("确认删除该题目？", "提示", { type: "warning" });
  await deleteQuestion(row.questionId);
  ElMessage.success("已删除");
  await reload();
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

onMounted(async () => {
  await loadReferenceData();
  await reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>题库</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
        <el-button @click="downloadTemplate">下载导入模板</el-button>
        <el-button @click="openImport">导入题库</el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%">
      <el-table-column prop="questionId" label="ID" width="90" />
      <el-table-column label="年龄范围(岁)" width="140">
        <template #default="{ row }">{{ row.minAge }}-{{ row.maxAge }}</template>
      </el-table-column>
      <el-table-column label="题型" width="100">
        <template #default="{ row }">{{ questionTypeLabel(row.questionType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="content" label="问题" />
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
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

    <QuestionEditorDialog
      v-model="editorVisible"
      :title="editorTitle"
      :initial="editorInitial"
      :dimensions="dimensions"
      :trouble-scenes="troubleScenes.map((s) => ({ id: s.id, name: s.name }))"
      @submit="onSubmit"
    />

    <QuestionImportDialog v-model="importVisible" @imported="reload" />
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
