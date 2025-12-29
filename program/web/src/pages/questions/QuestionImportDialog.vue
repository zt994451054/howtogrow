<script setup lang="ts">
import { ref } from "vue";
import { ElMessage } from "element-plus";
import { importQuestionsExcel, type QuestionImportResponse } from "@/api/admin/questions";

const visible = defineModel<boolean>({ required: true });
const emit = defineEmits<{ imported: [] }>();

const uploading = ref(false);
const result = ref<QuestionImportResponse | null>(null);

const MAX_SIZE_BYTES = 5 * 1024 * 1024;

function reset() {
  result.value = null;
}

async function onSelectFile(e: Event) {
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
    result.value = await importQuestionsExcel(file);
    ElMessage.success("导入完成");
    emit("imported");
  } finally {
    uploading.value = false;
    input.value = "";
  }
}
</script>

<template>
  <el-dialog v-model="visible" title="题库导入" width="720px" @closed="reset">
    <div class="uploader">
      <input type="file" accept=".xlsx" :disabled="uploading" @change="onSelectFile" />
      <el-text type="info">
        限制：.xlsx，≤5MB；一行一个选项；同一问题多个选项请复制多行（问题/题型/年龄可在后续行留空沿用上一行）。
      </el-text>
    </div>

    <div v-if="result" class="result">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="总行数">{{ result.total }}</el-descriptions-item>
        <el-descriptions-item label="成功">{{ result.success }}</el-descriptions-item>
        <el-descriptions-item label="失败">{{ result.failed }}</el-descriptions-item>
      </el-descriptions>

      <el-table v-if="result.failures.length" :data="result.failures" style="width: 100%; margin-top: 12px">
        <el-table-column prop="row" label="行号" width="100" />
        <el-table-column prop="reason" label="原因" />
      </el-table>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.uploader {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 8px;
}
.result {
  margin-top: 16px;
}
</style>

