<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import StatusTag from "@/components/StatusTag.vue";
import { formatMoneyCent } from "@/utils/format";
import { createPlan, deletePlan, listPlans, updatePlan, type PlanUpsertRequest, type PlanView } from "@/api/admin/plans";

const loading = ref(false);
const items = ref<PlanView[]>([]);

const dialogVisible = ref(false);
const editing = ref<PlanView | null>(null);
const formRef = ref<FormInstance>();

type PlanFormState = {
  name: string;
  days: number;
  originalPriceYuan: number;
  priceYuan: number;
  status: number;
};

const form = reactive<PlanFormState>({
  name: "",
  days: 30,
  originalPriceYuan: 0,
  priceYuan: 0,
  status: 1
});

const rules: FormRules = {
  name: [{ required: true, message: "请输入套餐名称", trigger: "blur" }],
  days: [{ required: true, message: "请输入套餐天数", trigger: "blur" }],
  originalPriceYuan: [{ required: true, message: "请输入原价（元）", trigger: "blur" }],
  priceYuan: [{ required: true, message: "请输入现价（元）", trigger: "blur" }]
};

async function reload() {
  loading.value = true;
  try {
    items.value = await listPlans();
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editing.value = null;
  form.name = "";
  form.days = 30;
  form.originalPriceYuan = 0;
  form.priceYuan = 0;
  form.status = 1;
  dialogVisible.value = true;
}

function openEdit(row: PlanView) {
  editing.value = row;
  form.name = row.name;
  form.days = row.days;
  form.originalPriceYuan = row.originalPriceCent / 100;
  form.priceYuan = row.priceCent / 100;
  form.status = row.status;
  dialogVisible.value = true;
}

function moneyYuanToCent(amountYuan: number): number {
  if (!Number.isFinite(amountYuan) || amountYuan < 0) return 0;
  return Math.round(amountYuan * 100);
}

async function save() {
  const ok = await formRef.value?.validate();
  if (!ok) return;

  if (form.originalPriceYuan < form.priceYuan) {
    ElMessage.error("原价不能小于现价");
    return;
  }

  const request: PlanUpsertRequest = {
    name: form.name.trim(),
    days: form.days,
    originalPriceCent: moneyYuanToCent(form.originalPriceYuan),
    priceCent: moneyYuanToCent(form.priceYuan),
    status: form.status
  };

  if (editing.value) {
    await updatePlan(editing.value.planId, request);
    ElMessage.success("已保存");
  } else {
    await createPlan(request);
    ElMessage.success("已创建");
  }

  dialogVisible.value = false;
  await reload();
}

async function remove(row: PlanView) {
  await ElMessageBox.confirm(`确认删除套餐「${row.name}」？`, "提示", { type: "warning" });
  await deletePlan(row.planId);
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
      <h3>套餐</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%">
      <el-table-column prop="planId" label="ID" width="90" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="days" label="天数" width="90" />
      <el-table-column label="原价(元)" width="120">
        <template #default="{ row }">¥ {{ formatMoneyCent(row.originalPriceCent) }}</template>
      </el-table-column>
      <el-table-column label="现价(元)" width="120">
        <template #default="{ row }">¥ {{ formatMoneyCent(row.priceCent) }}</template>
      </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑套餐' : '新增套餐'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="套餐名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="套餐天数" prop="days">
          <el-input-number v-model="form.days" :min="1" :step="1" />
        </el-form-item>
        <el-form-item label="原价(元)" prop="originalPriceYuan">
          <el-input-number v-model="form.originalPriceYuan" :min="0" :step="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="现价(元)" prop="priceYuan">
          <el-input-number v-model="form.priceYuan" :min="0" :step="0.01" :precision="2" />
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
