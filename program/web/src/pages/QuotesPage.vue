<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import StatusTag from "@/components/StatusTag.vue";
import { createQuote, deleteQuote, listQuotes, updateQuote, type QuoteUpsertRequest, type QuoteView } from "@/api/admin/quotes";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

const loading = ref(false);
const items = ref<QuoteView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });

type QuoteFilter = {
  scene: string;
  status: number | null;
  keyword: string;
};

const scenes = [
  { label: "每日觉察", value: "每日觉察" },
  { label: "育儿状态", value: "育儿状态" },
  { label: "烦恼档案", value: "烦恼档案" },
  { label: "育儿日记", value: "育儿日记" }
];

const filter = reactive<QuoteFilter>({
  scene: "",
  status: null,
  keyword: ""
});

const dialogVisible = ref(false);
const editing = ref<QuoteView | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<QuoteUpsertRequest>({
  content: "",
  scene: "每日觉察",
  minAge: 0,
  maxAge: 18,
  status: 1
});

const rules: FormRules = {
  content: [{ required: true, message: "请输入内容", trigger: "blur" }],
  scene: [{ required: true, message: "请选择场景", trigger: "change" }],
  minAge: [{ required: true, message: "请输入最小年龄", trigger: "change" }],
  maxAge: [
    { required: true, message: "请输入最大年龄", trigger: "change" },
    {
      validator: (_rule, _value, callback) => {
        if (Number(form.minAge) > Number(form.maxAge)) callback(new Error("最小年龄不能大于最大年龄"));
        else callback();
      },
      trigger: "change"
    }
  ]
};

async function reload() {
  loading.value = true;
  try {
    const res = await listQuotes({
      page: page.value.page,
      pageSize: page.value.pageSize,
      scene: filter.scene || undefined,
      status: filter.status ?? undefined,
      keyword: filter.keyword.trim() || undefined
    });
    items.value = res.items;
    page.value.total = res.total;
    if (page.value.page > 1 && page.value.total > 0 && items.value.length === 0) {
      page.value.page -= 1;
      await reload();
    }
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

function onSearch() {
  page.value.page = 1;
  void reload();
}

function onReset() {
  filter.scene = "";
  filter.status = null;
  filter.keyword = "";
  page.value.page = 1;
  void reload();
}

function openCreate() {
  editing.value = null;
  form.content = "";
  form.scene = "每日觉察";
  form.minAge = 0;
  form.maxAge = 18;
  form.status = 1;
  dialogVisible.value = true;
}

function openEdit(row: QuoteView) {
  editing.value = row;
  form.content = row.content;
  form.scene = row.scene;
  form.minAge = row.minAge;
  form.maxAge = row.maxAge;
  form.status = row.status;
  dialogVisible.value = true;
}

async function save() {
  const ok = await formRef.value?.validate();
  if (!ok) return;

  const request: QuoteUpsertRequest = {
    content: form.content.trim(),
    scene: form.scene,
    minAge: Number(form.minAge),
    maxAge: Number(form.maxAge),
    status: form.status
  };

  if (editing.value) {
    await updateQuote(editing.value.id, request);
    ElMessage.success("已保存");
  } else {
    await createQuote(request);
    ElMessage.success("已创建");
  }

  dialogVisible.value = false;
  if (!editing.value) {
    page.value.page = 1;
  }
  await reload();
}

async function remove(row: QuoteView) {
  await ElMessageBox.confirm("确认删除该条鸡汤语？", "提示", { type: "warning" });
  await deleteQuote(row.id);
  ElMessage.success("已删除");
  await reload();
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>鸡汤语</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <div class="filters">
      <el-form :inline="true" label-width="60px" @submit.prevent>
        <el-form-item label="场景">
          <el-select v-model="filter.scene" placeholder="全部" style="width: 140px" clearable>
            <el-option v-for="s in scenes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" style="width: 120px" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="内容" clearable style="width: 220px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="scene" label="场景" width="120" />
      <el-table-column label="年龄(岁)" width="140">
        <template #default="{ row }">{{ row.minAge }}-{{ row.maxAge }}</template>
      </el-table-column>
      <el-table-column prop="content" label="内容" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑鸡汤语' : '新增鸡汤语'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="场景" prop="scene">
          <el-select v-model="form.scene" style="width: 100%">
            <el-option v-for="s in scenes" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄(岁)">
          <div style="display: flex; gap: 10px; width: 100%">
            <el-input-number v-model="form.minAge" :min="0" :max="18" :step="1" />
            <span style="line-height: 32px">-</span>
            <el-input-number v-model="form.maxAge" :min="0" :max="18" :step="1" />
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
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
