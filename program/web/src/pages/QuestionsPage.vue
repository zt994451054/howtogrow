<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import StatusTag from "@/components/StatusTag.vue";
import { listDimensions, type DimensionView } from "@/api/admin/dimensions";
import { listAllTroubleScenes, type TroubleSceneView } from "@/api/admin/trouble-scenes";
import {
  batchDeleteQuestions,
  batchUpdateQuestionTroubleScenes,
  createQuestion,
  deleteQuestion,
  getQuestion,
  listQuestions,
  updateQuestion,
  type QuestionDetailView,
  type QuestionSummaryView,
  type TroubleSceneBatchUpdateMode,
  type QuestionType,
  type QuestionUpsertRequest
} from "@/api/admin/questions";
import QuestionBatchTroubleSceneDialog from "@/pages/questions/QuestionBatchTroubleSceneDialog.vue";
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
const selected = ref<QuestionSummaryView[]>([]);
const batchDeleting = ref(false);

type QuestionFilter = {
  minAge: string;
  maxAge: string;
  questionType: "" | QuestionType;
  status: number | null;
  troubleSceneId: number | null;
  keyword: string;
};

const filter = reactive<QuestionFilter>({
  minAge: "",
  maxAge: "",
  questionType: "",
  status: null,
  troubleSceneId: null,
  keyword: ""
});

const dimensions = ref<DimensionView[]>([]);
const troubleScenes = ref<TroubleSceneView[]>([]);
const troubleSceneOptions = computed(() => troubleScenes.value.map((s) => ({ id: s.id, name: s.name })));
const troubleSceneNameById = computed(() => {
  const out = new Map<number, string>();
  for (const s of troubleScenes.value) {
    out.set(s.id, s.name);
  }
  return out;
});

function formatTroubleSceneNames(troubleSceneIds: number[] | undefined) {
  const ids = Array.isArray(troubleSceneIds) ? troubleSceneIds : [];
  if (ids.length === 0) return "-";
  return ids.map((id) => troubleSceneNameById.value.get(id) ?? String(id)).join("、");
}

const editorVisible = ref(false);
const editorTitle = ref("新增题目");
const editorInitial = ref<QuestionDetailView | null>(null);

const importVisible = ref(false);
const templateUrl = computed(() => `${import.meta.env.BASE_URL}question-import-template.xlsx`);

const batchTroubleSceneVisible = ref(false);
const batchTroubleSceneMode = ref<TroubleSceneBatchUpdateMode>("APPEND");
const batchTroubleSceneSubmitting = ref(false);

async function reload() {
  const minAgeText = filter.minAge.trim();
  const maxAgeText = filter.maxAge.trim();
  const minAge = minAgeText ? Number(minAgeText) : undefined;
  const maxAge = maxAgeText ? Number(maxAgeText) : undefined;

  const invalidAge = (age: number) => !Number.isInteger(age) || age < 0 || age > 18;
  if (minAge !== undefined && invalidAge(minAge)) {
    ElMessage.warning("最小年龄请输入 0-18 的整数");
    return;
  }
  if (maxAge !== undefined && invalidAge(maxAge)) {
    ElMessage.warning("最大年龄请输入 0-18 的整数");
    return;
  }
  if (minAge !== undefined && maxAge !== undefined && minAge > maxAge) {
    ElMessage.warning("最小年龄不能大于最大年龄");
    return;
  }

  loading.value = true;
  try {
    const res = await listQuestions({
      page: page.value.page,
      pageSize: page.value.pageSize,
      minAge,
      maxAge,
      status: filter.status ?? undefined,
      questionType: filter.questionType || undefined,
      troubleSceneId: filter.troubleSceneId ?? undefined,
      keyword: filter.keyword.trim() || undefined
    });
    items.value = res.items;
    page.value.total = res.total;
    if (page.value.page > 1 && page.value.total > 0 && items.value.length === 0) {
      page.value.page -= 1;
      await reload();
      return;
    }
    selected.value = [];
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

function openBatchTroubleScene(mode: TroubleSceneBatchUpdateMode | string) {
  if (!selected.value.length) return;
  const normalized = String(mode || "").toUpperCase();
  if (normalized !== "APPEND" && normalized !== "REPLACE") return;
  batchTroubleSceneMode.value = normalized as TroubleSceneBatchUpdateMode;
  batchTroubleSceneVisible.value = true;
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

function onSelectionChange(rows: QuestionSummaryView[]) {
  selected.value = Array.isArray(rows) ? rows : [];
}

async function removeSelected() {
  if (!selected.value.length) return;
  const ids = Array.from(new Set(selected.value.map((x) => x.questionId)));
  await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 道题目？`, "提示", { type: "warning" });

  batchDeleting.value = true;
  try {
    await batchDeleteQuestions(ids);
    ElMessage.success("已批量删除");
    await reload();
  } finally {
    batchDeleting.value = false;
  }
}

async function onBatchTroubleSceneSubmit(payload: { mode: TroubleSceneBatchUpdateMode; troubleSceneIds: number[] }) {
  if (!selected.value.length) return;
  const ids = Array.from(new Set(selected.value.map((x) => x.questionId)));

  batchTroubleSceneSubmitting.value = true;
  try {
    await batchUpdateQuestionTroubleScenes({
      ids,
      troubleSceneIds: payload.troubleSceneIds,
      mode: payload.mode
    });
    ElMessage.success("已更新烦恼场景");
    batchTroubleSceneVisible.value = false;
    await reload();
  } finally {
    batchTroubleSceneSubmitting.value = false;
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

function onSearch() {
  page.value.page = 1;
  void reload();
}

function onReset() {
  filter.minAge = "";
  filter.maxAge = "";
  filter.questionType = "";
  filter.status = null;
  filter.troubleSceneId = null;
  filter.keyword = "";
  page.value.page = 1;
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
        <el-dropdown
          trigger="click"
          :disabled="!selected.length || batchTroubleSceneSubmitting"
          @command="openBatchTroubleScene"
        >
          <el-button :disabled="!selected.length || batchTroubleSceneSubmitting">
            批量场景<span v-if="selected.length">({{ selected.length }})</span>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="APPEND">增加烦恼场景</el-dropdown-item>
              <el-dropdown-item command="REPLACE">修改烦恼场景</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button type="danger" :disabled="!selected.length" :loading="batchDeleting" @click="removeSelected">
          批量删除<span v-if="selected.length">({{ selected.length }})</span>
        </el-button>
        <el-button @click="downloadTemplate">下载导入模板</el-button>
        <el-button @click="openImport">导入题库</el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <div class="filters">
      <el-form :inline="true" label-width="60px" @submit.prevent>
        <el-form-item label="年龄范围">
          <div class="age-range">
            <el-input v-model="filter.minAge" placeholder="最小" clearable style="width: 110px" @keyup.enter="onSearch" />
            <span class="range-sep">-</span>
            <el-input v-model="filter.maxAge" placeholder="最大" clearable style="width: 110px" @keyup.enter="onSearch" />
          </div>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="filter.questionType" placeholder="全部" style="width: 120px" clearable>
            <el-option label="单选" value="SINGLE" />
            <el-option label="多选" value="MULTI" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" style="width: 120px" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="场景">
          <el-select v-model="filter.troubleSceneId" placeholder="全部" style="width: 200px" clearable filterable>
            <el-option v-for="s in troubleScenes" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="问题" clearable style="width: 220px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="46" align="center" />
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
      <el-table-column label="烦恼场景" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ formatTroubleSceneNames(row.troubleSceneIds) }}</template>
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
      :trouble-scenes="troubleSceneOptions"
      @submit="onSubmit"
    />

    <QuestionImportDialog v-model="importVisible" @imported="reload" />

    <QuestionBatchTroubleSceneDialog
      v-model="batchTroubleSceneVisible"
      :mode="batchTroubleSceneMode"
      :selected-count="selected.length"
      :trouble-scenes="troubleSceneOptions"
      :loading="batchTroubleSceneSubmitting"
      @submit="onBatchTroubleSceneSubmit"
    />
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
  margin-bottom: 10px;
}
.age-range {
  display: flex;
  align-items: center;
  gap: 8px;
}
.range-sep {
  line-height: 32px;
  color: #909399;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
