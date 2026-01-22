<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import StatusTag from "@/components/StatusTag.vue";
import { batchDeleteAiQuickQuestions, createAiQuickQuestion, deleteAiQuickQuestion, listAiQuickQuestions, updateAiQuickQuestion, type AiQuickQuestionUpsertRequest, type AiQuickQuestionView } from "@/api/admin/ai-quick-questions";
import { formatDateTime } from "@/utils/format";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

type FilterState = {
  status: number | null;
  keyword: string;
};

const loading = ref(false);
const items = ref<AiQuickQuestionView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });
const filter = reactive<FilterState>({ status: null, keyword: "" });
const selected = ref<AiQuickQuestionView[]>([]);
const batchDeleting = ref(false);

const dialogVisible = ref(false);
const editing = ref<AiQuickQuestionView | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<AiQuickQuestionUpsertRequest>({
  prompt: "",
  status: 1,
  sortNo: 0
});

const rules: FormRules = {
  prompt: [{ required: true, message: "请输入快捷问题", trigger: "blur" }],
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
  sortNo: [{ required: true, message: "请输入排序号", trigger: "change" }]
};

async function reload() {
  loading.value = true;
  try {
    const res = await listAiQuickQuestions({
      page: page.value.page,
      pageSize: page.value.pageSize,
      status: filter.status ?? undefined,
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

function onSearch() {
  page.value.page = 1;
  void reload();
}

function onReset() {
  filter.status = null;
  filter.keyword = "";
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

function openCreate() {
  editing.value = null;
  form.prompt = "";
  form.status = 1;
  form.sortNo = 0;
  dialogVisible.value = true;
}

function openEdit(row: AiQuickQuestionView) {
  editing.value = row;
  form.prompt = row.prompt;
  form.status = row.status;
  form.sortNo = row.sortNo;
  dialogVisible.value = true;
}

async function save() {
  const ok = await formRef.value?.validate();
  if (!ok) return;

  const request: AiQuickQuestionUpsertRequest = {
    prompt: form.prompt.trim(),
    status: form.status,
    sortNo: Number(form.sortNo)
  };

  if (editing.value) {
    await updateAiQuickQuestion(editing.value.id, request);
    ElMessage.success("已保存");
  } else {
    await createAiQuickQuestion(request);
    ElMessage.success("已创建");
    page.value.page = 1;
  }

  dialogVisible.value = false;
  await reload();
}

async function remove(row: AiQuickQuestionView) {
  await ElMessageBox.confirm("确认删除该快捷问题？", "提示", { type: "warning" });
  await deleteAiQuickQuestion(row.id);
  ElMessage.success("已删除");
  await reload();
}

function onSelectionChange(rows: AiQuickQuestionView[]) {
  selected.value = Array.isArray(rows) ? rows : [];
}

async function removeSelected() {
  if (!selected.value.length) return;
  const ids = Array.from(new Set(selected.value.map((x) => x.id)));
  await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 条快捷问题？`, "提示", { type: "warning" });

  batchDeleting.value = true;
  try {
    await batchDeleteAiQuickQuestions(ids);
    ElMessage.success("已批量删除");
    await reload();
  } finally {
    batchDeleting.value = false;
  }
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>快捷问题</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
        <el-button type="danger" :disabled="!selected.length" :loading="batchDeleting" @click="removeSelected">
          批量删除<span v-if="selected.length">({{ selected.length }})</span>
        </el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <div class="filters">
      <el-form :inline="true" label-width="60px" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" style="width: 120px" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="快捷问题" clearable style="width: 260px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="46" align="center" />
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column label="顺序" width="90" align="center">
        <template #default="{ row }">{{ row.sortNo }}</template>
      </el-table-column>
      <el-table-column prop="prompt" label="快捷问题" show-overflow-tooltip />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑快捷问题' : '新增快捷问题'" width="720px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="快捷问题" prop="prompt">
          <el-input v-model="form.prompt" type="textarea" :rows="4" maxlength="512" show-word-limit />
        </el-form-item>
        <el-form-item label="顺序" prop="sortNo">
          <el-input-number v-model="form.sortNo" :step="1" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
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
  margin-bottom: 10px;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
