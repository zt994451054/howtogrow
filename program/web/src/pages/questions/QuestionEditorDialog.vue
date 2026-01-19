<script setup lang="ts">
import { computed, reactive, ref, watch } from "vue";
import { ElMessage, type FormInstance } from "element-plus";
import type { DimensionView } from "@/api/admin/dimensions";
import type { QuestionDetailView, QuestionType, QuestionUpsertRequest } from "@/api/admin/questions";

type TroubleSceneOption = { id: number; name: string };

type Props = {
  modelValue: boolean;
  title: string;
  dimensions: DimensionView[];
  troubleScenes: TroubleSceneOption[];
  initial?: QuestionDetailView | null;
};

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: "update:modelValue", v: boolean): void;
  (e: "submit", v: { request: QuestionUpsertRequest; questionId?: number }): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit("update:modelValue", v)
});

const formRef = ref<FormInstance>();
const form = reactive<QuestionUpsertRequest>({
  minAge: 3,
  maxAge: 3,
  questionType: "MULTI",
  content: "",
  troubleSceneIds: [],
  status: 1,
  options: []
});

const editingId = ref<number | undefined>(undefined);

function resetFromInitial() {
  const q = props.initial;
  if (!q) {
    editingId.value = undefined;
    form.minAge = 3;
    form.maxAge = 3;
    form.questionType = "MULTI";
    form.content = "";
    form.troubleSceneIds = [];
    form.status = 1;
    form.options = [
      {
        content: "",
        suggestFlag: 1,
        improvementTip: null,
        dimensionScores: [{ dimensionCode: "", score: 1 }]
      },
      {
        content: "",
        suggestFlag: 0,
        improvementTip: null,
        dimensionScores: [{ dimensionCode: "", score: 1 }]
      }
    ];
    return;
  }

  editingId.value = q.questionId;
  form.minAge = q.minAge;
  form.maxAge = q.maxAge;
  form.questionType = q.questionType;
  form.content = q.content;
  form.troubleSceneIds = Array.isArray(q.troubleSceneIds) ? q.troubleSceneIds.slice() : [];
  form.status = 1;
  form.options = q.options.map((o) => ({
    content: o.content,
    suggestFlag: o.suggestFlag,
    improvementTip: o.improvementTip,
    dimensionScores: o.dimensionScores.map((d) => ({ dimensionCode: d.dimensionCode, score: d.score }))
  }));
}

watch(
  () => [props.modelValue, props.initial],
  ([open]) => {
    if (open) resetFromInitial();
  }
);

function close() {
  visible.value = false;
}

function addOption() {
  form.options.push({
    content: "",
    suggestFlag: 1,
    improvementTip: null,
    dimensionScores: [{ dimensionCode: "", score: 1 }]
  });
}

function removeOption(index: number) {
  form.options.splice(index, 1);
}

function addDimensionScore(optionIndex: number) {
  form.options[optionIndex].dimensionScores.push({ dimensionCode: "", score: 1 });
}

function removeDimensionScore(optionIndex: number, scoreIndex: number) {
  form.options[optionIndex].dimensionScores.splice(scoreIndex, 1);
}

function validateRequest(): string | null {
  if (!Number.isInteger(form.minAge) || form.minAge < 0 || form.minAge > 18) return "请输入合法最小年龄（0-18岁）";
  if (!Number.isInteger(form.maxAge) || form.maxAge < 0 || form.maxAge > 18) return "请输入合法最大年龄（0-18岁）";
  if (form.minAge > form.maxAge) return "最小年龄不能大于最大年龄";
  if (!form.content.trim()) return "请输入问题内容";
  if (!form.options.length) return "请至少配置 1 个选项";
  if (form.options.length < 2) return "请至少配置 2 个选项";

  for (const [i, opt] of form.options.entries()) {
    if (!opt.content.trim()) return `第 ${i + 1} 个选项内容不能为空`;
    if (opt.dimensionScores.length === 0) return `第 ${i + 1} 个选项请至少配置 1 个维度分值`;

    const codes = opt.dimensionScores.map((d) => d.dimensionCode).filter(Boolean);
    const unique = new Set(codes);
    if (codes.length !== unique.size) return `第 ${i + 1} 个选项存在重复维度`;

    for (const [j, ds] of opt.dimensionScores.entries()) {
      if (!ds.dimensionCode) return `第 ${i + 1} 个选项第 ${j + 1} 行：请选择维度`;
      if (!Number.isInteger(ds.score) || ds.score < 1) return `第 ${i + 1} 个选项第 ${j + 1} 行：分值必须为 >= 1 的整数`;
    }
  }

  return null;
}

function submit() {
  const err = validateRequest();
  if (err) {
    ElMessage.warning(err);
    return;
  }

  emit("submit", {
    questionId: editingId.value,
    request: {
      minAge: form.minAge,
      maxAge: form.maxAge,
      questionType: form.questionType as QuestionType,
      content: form.content.trim(),
      troubleSceneIds: Array.isArray(form.troubleSceneIds) ? form.troubleSceneIds.slice() : [],
      status: form.status,
      options: form.options.map((o) => ({
        content: o.content.trim(),
        suggestFlag: o.suggestFlag,
        improvementTip: o.improvementTip ? o.improvementTip.trim() : null,
        dimensionScores: o.dimensionScores.map((d) => ({ dimensionCode: d.dimensionCode, score: d.score }))
      }))
    }
  });
}
</script>

<template>
  <el-dialog v-model="visible" :title="title" width="900px" top="5vh" @close="close">
    <el-form ref="formRef" :model="form" label-width="110px">
      <el-form-item label="适用年龄（岁）">
        <div style="display: flex; align-items: center; gap: 8px">
          <el-input-number v-model="form.minAge" :min="0" :max="18" :step="1" />
          <span style="color: #909399">至</span>
          <el-input-number v-model="form.maxAge" :min="0" :max="18" :step="1" />
        </div>
      </el-form-item>
      <el-form-item label="题型">
        <el-radio-group v-model="form.questionType">
          <el-radio label="MULTI">多选</el-radio>
          <el-radio label="SINGLE">单选</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="烦恼场景">
        <el-select v-model="form.troubleSceneIds" multiple filterable collapse-tags style="width: 100%" placeholder="可不选">
          <el-option v-for="s in troubleScenes" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="问题">
        <el-input v-model="form.content" type="textarea" :rows="3" placeholder="请输入问题内容" />
      </el-form-item>

      <el-form-item label="选项">
        <div class="options">
          <el-button type="primary" plain @click="addOption">新增选项</el-button>
          <div v-for="(opt, idx) in form.options" :key="idx" class="option-card">
            <div class="option-header">
              <div class="option-title">选项 {{ idx + 1 }}</div>
              <div class="option-actions">
                <el-button link type="danger" :disabled="form.options.length <= 2" @click="removeOption(idx)"
                  >删除选项</el-button
                >
              </div>
            </div>

            <div class="option-grid">
              <el-form-item label="内容" label-width="60px">
                <el-input v-model="opt.content" placeholder="选项内容" />
              </el-form-item>
              <el-form-item label="建议" label-width="60px">
                <el-radio-group v-model="opt.suggestFlag">
                  <el-radio :label="1">建议</el-radio>
                  <el-radio :label="0">不建议</el-radio>
                </el-radio-group>
              </el-form-item>
            </div>

            <el-form-item label="改进建议" label-width="90px">
              <el-input v-model="opt.improvementTip" type="textarea" :rows="2" placeholder="可为空" />
            </el-form-item>

            <div class="score-header">
              <div class="score-title">维度分值</div>
              <el-button size="small" @click="addDimensionScore(idx)">新增维度</el-button>
            </div>
            <el-table :data="opt.dimensionScores" size="small" style="width: 100%">
              <el-table-column label="维度" width="240">
                <template #default="{ row }">
                  <el-select v-model="row.dimensionCode" filterable style="width: 220px">
                    <el-option v-for="d in dimensions" :key="d.code" :label="`${d.name}（${d.code}）`" :value="d.code" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="分值" width="160">
                <template #default="{ row }">
                  <el-input-number v-model="row.score" :min="1" :step="1" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90">
                <template #default="{ $index }">
                  <el-button link type="danger" @click="removeDimensionScore(idx, $index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.options {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 12px;
}

.option-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.option-title {
  font-weight: 600;
}

.option-grid {
  display: grid;
  grid-template-columns: 1fr 240px 220px;
  gap: 10px;
}

.score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0;
}
</style>
