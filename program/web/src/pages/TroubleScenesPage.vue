<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { batchDeleteTroubleScenes, createTroubleScene, deleteTroubleScene, importTroubleScenesExcel, listTroubleScenes, updateTroubleScene, type TroubleSceneUpsertRequest, type TroubleSceneView } from "@/api/admin/trouble-scenes";
import { uploadPublic } from "@/api/admin/uploads";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

const loading = ref(false);
const items = ref<TroubleSceneView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });
const selected = ref<TroubleSceneView[]>([]);
const batchDeleting = ref(false);

type TroubleSceneFilter = {
  keyword: string;
  ageYear: string;
};

const filter = reactive<TroubleSceneFilter>({
  keyword: "",
  ageYear: ""
});

const dialogVisible = ref(false);
const editing = ref<TroubleSceneView | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<TroubleSceneUpsertRequest>({
  name: "",
  logoUrl: null,
  minAge: 0,
  maxAge: 18
});

const rules: FormRules = {
  name: [{ required: true, message: "请输入名称", trigger: "blur" }],
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

const importVisible = ref(false);
const uploading = ref(false);
const MAX_SIZE_BYTES = 5 * 1024 * 1024;

async function reload() {
  const ageText = filter.ageYear.trim();
  const ageYear = ageText ? Number(ageText) : undefined;
  if (ageYear !== undefined && (!Number.isInteger(ageYear) || ageYear < 0 || ageYear > 18)) {
    ElMessage.warning("年龄请输入 0-18 的整数");
    return;
  }

  loading.value = true;
  try {
    const res = await listTroubleScenes({
      page: page.value.page,
      pageSize: page.value.pageSize,
      keyword: filter.keyword.trim() || undefined,
      ageYear
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
  filter.keyword = "";
  filter.ageYear = "";
  page.value.page = 1;
  void reload();
}

function openCreate() {
  editing.value = null;
  form.name = "";
  form.logoUrl = null;
  form.minAge = 0;
  form.maxAge = 18;
  dialogVisible.value = true;
}

function openEdit(row: TroubleSceneView) {
  editing.value = row;
  form.name = row.name;
  form.logoUrl = row.logoUrl ?? null;
  form.minAge = row.minAge;
  form.maxAge = row.maxAge;
  dialogVisible.value = true;
}

async function save() {
  const ok = await formRef.value?.validate();
  if (!ok) return;

  const request: TroubleSceneUpsertRequest = {
    name: form.name.trim(),
    logoUrl: form.logoUrl ?? null,
    minAge: Number(form.minAge),
    maxAge: Number(form.maxAge)
  };
  if (editing.value) {
    await updateTroubleScene(editing.value.id, request);
    ElMessage.success("已保存");
  } else {
    await createTroubleScene(request);
    ElMessage.success("已创建");
  }

  dialogVisible.value = false;
  if (!editing.value) {
    page.value.page = 1;
  }
  await reload();
}

async function remove(row: TroubleSceneView) {
  await ElMessageBox.confirm("确认删除该烦恼场景？（将影响题目关联）", "提示", { type: "warning" });
  await deleteTroubleScene(row.id);
  ElMessage.success("已删除");
  await reload();
}

function onSelectionChange(rows: TroubleSceneView[]) {
  selected.value = Array.isArray(rows) ? rows : [];
}

async function removeSelected() {
  if (!selected.value.length) return;
  const ids = Array.from(new Set(selected.value.map((x) => x.id)));
  await ElMessageBox.confirm(`确认删除选中的 ${ids.length} 个烦恼场景？（将影响题目关联）`, "提示", {
    type: "warning"
  });

  batchDeleting.value = true;
  try {
    await batchDeleteTroubleScenes(ids);
    ElMessage.success("已批量删除");
    await reload();
  } finally {
    batchDeleting.value = false;
  }
}

async function onUploadLogo(options: { file: File; onSuccess: (res: unknown) => void; onError: (err: unknown) => void }) {
  try {
    const url = await uploadPublic(options.file);
    form.logoUrl = url;
    options.onSuccess({ url });
  } catch (e) {
    options.onError(e);
  }
}

function openImport() {
  importVisible.value = true;
}

async function onSelectImportFile(e: Event) {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;

  if (!file.name.toLowerCase().endsWith(".xlsx")) {
    ElMessage.warning("仅支持 .xlsx 文件");
    input.value = "";
    return;
  }
  if (file.size > MAX_SIZE_BYTES) {
    ElMessage.warning("文件不能超过 5MB");
    input.value = "";
    return;
  }

  uploading.value = true;
  try {
    const res = await importTroubleScenesExcel(file);
    ElMessage.success(`导入完成（${res.imported}条）`);
    importVisible.value = false;
    page.value.page = 1;
    await reload();
  } finally {
    uploading.value = false;
    input.value = "";
  }
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>烦恼场景</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
        <el-button type="danger" :disabled="!selected.length" :loading="batchDeleting" @click="removeSelected">
          批量删除<span v-if="selected.length">({{ selected.length }})</span>
        </el-button>
        <el-button @click="openImport">导入</el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <div class="filters">
      <el-form :inline="true" label-width="60px" @submit.prevent>
        <el-form-item label="年龄">
          <el-input v-model="filter.ageYear" placeholder="0-18" clearable style="width: 110px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="名称" clearable style="width: 220px" @keyup.enter="onSearch" />
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
      <el-table-column prop="name" label="名称" />
      <el-table-column label="年龄(岁)" width="140">
        <template #default="{ row }">{{ row.minAge }}-{{ row.maxAge }}</template>
      </el-table-column>
      <el-table-column label="Logo" width="140">
        <template #default="{ row }">
          <el-image v-if="row.logoUrl" :src="row.logoUrl" style="width: 48px; height: 48px" fit="cover" />
          <span v-else style="color: #909399">—</span>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑烦恼场景' : '新增烦恼场景'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="年龄(岁)">
          <div style="display: flex; gap: 10px; width: 100%">
            <el-input-number v-model="form.minAge" :min="0" :max="18" :step="1" />
            <span style="line-height: 32px">-</span>
            <el-input-number v-model="form.maxAge" :min="0" :max="18" :step="1" />
          </div>
        </el-form-item>
        <el-form-item label="Logo">
          <div style="display: flex; gap: 12px; align-items: center">
            <el-upload :show-file-list="false" :http-request="onUploadLogo" accept="image/*">
              <el-button>上传</el-button>
            </el-upload>
            <el-image v-if="form.logoUrl" :src="form.logoUrl" style="width: 48px; height: 48px" fit="cover" />
            <el-button v-if="form.logoUrl" link type="danger" @click="form.logoUrl = null">移除</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入烦恼场景" width="720px">
      <div class="uploader">
        <input type="file" accept=".xlsx" :disabled="uploading" @change="onSelectImportFile" />
        <el-text type="info">
          限制：.xlsx，≤5MB；列名支持：名称/Logo/最小年龄/最大年龄（Logo 可为空）。任一错误将整体失败。
        </el-text>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
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
.uploader {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
}
</style>
