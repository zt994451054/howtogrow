<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import StatusTag from "@/components/StatusTag.vue";
import { createQuote, deleteQuote, listQuotes, updateQuote, type QuoteUpsertRequest, type QuoteView } from "@/api/admin/quotes";

const loading = ref(false);
const items = ref<QuoteView[]>([]);

const dialogVisible = ref(false);
const editing = ref<QuoteView | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<QuoteUpsertRequest>({
  content: "",
  status: 1
});

const rules: FormRules = {
  content: [{ required: true, message: "请输入内容", trigger: "blur" }]
};

async function reload() {
  loading.value = true;
  try {
    items.value = await listQuotes();
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editing.value = null;
  form.content = "";
  form.status = 1;
  dialogVisible.value = true;
}

function openEdit(row: QuoteView) {
  editing.value = row;
  form.content = row.content;
  form.status = row.status;
  dialogVisible.value = true;
}

async function save() {
  const ok = await formRef.value?.validate();
  if (!ok) return;

  const request: QuoteUpsertRequest = {
    content: form.content.trim(),
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

    <el-table :data="items" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="90" />
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑鸡汤语' : '新增鸡汤语'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" />
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
</style>

