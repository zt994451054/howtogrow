<script setup lang="ts">
import { computed, reactive, watch } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import type { TroubleSceneBatchUpdateMode } from "@/api/admin/questions";

type TroubleSceneOption = { id: number; name: string };

type Props = {
  modelValue: boolean;
  mode: TroubleSceneBatchUpdateMode;
  selectedCount: number;
  troubleScenes: TroubleSceneOption[];
  loading?: boolean;
};

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: "update:modelValue", v: boolean): void;
  (e: "submit", payload: { mode: TroubleSceneBatchUpdateMode; troubleSceneIds: number[] }): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit("update:modelValue", v)
});

const title = computed(() => (props.mode === "APPEND" ? "批量增加烦恼场景" : "批量修改烦恼场景"));

const form = reactive<{ troubleSceneIds: number[] }>({
  troubleSceneIds: []
});

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      form.troubleSceneIds = [];
    }
  }
);

function close() {
  if (props.loading) return;
  visible.value = false;
}

async function submit() {
  const sceneIds = Array.isArray(form.troubleSceneIds) ? form.troubleSceneIds.slice() : [];
  if (props.mode === "APPEND" && sceneIds.length === 0) {
    ElMessage.warning("请选择至少 1 个烦恼场景");
    return;
  }

  if (props.mode === "REPLACE" && sceneIds.length === 0) {
    await ElMessageBox.confirm(
      `未选择任何场景，将清空选中的 ${props.selectedCount} 道题目的烦恼场景，是否继续？`,
      "提示",
      { type: "warning" }
    );
  } else {
    const actionText = props.mode === "APPEND" ? "增加" : "修改";
    await ElMessageBox.confirm(`确认对选中的 ${props.selectedCount} 道题目${actionText}烦恼场景？`, "提示", {
      type: "warning"
    });
  }

  emit("submit", { mode: props.mode, troubleSceneIds: sceneIds });
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="560px"
    :close-on-click-modal="!loading"
    :close-on-press-escape="!loading"
    :show-close="!loading"
    @close="close"
  >
    <div class="meta">已选择 {{ selectedCount }} 道题目</div>

    <el-form label-width="90px" @submit.prevent>
      <el-form-item label="烦恼场景">
        <el-select
          v-model="form.troubleSceneIds"
          multiple
          filterable
          collapse-tags
          style="width: 100%"
          placeholder="请选择"
          :disabled="loading"
        >
          <el-option v-for="s in troubleScenes" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="mode === 'REPLACE'"
      title="不选择任何场景将清空关联"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-alert
      v-else
      title="将把所选场景追加到原有关联上（自动去重）"
      type="info"
      :closable="false"
      show-icon
    />

    <template #footer>
      <el-button :disabled="loading" @click="close">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">提交</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.meta {
  margin-bottom: 12px;
  color: #606266;
}
</style>

