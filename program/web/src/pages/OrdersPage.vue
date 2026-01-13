<script setup lang="ts">
import { onMounted, ref } from "vue";
import { listOrders, type OrderView } from "@/api/admin/orders";
import { formatDateTime, formatMoneyCent } from "@/utils/format";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

const loading = ref(false);
const items = ref<OrderView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });

const ORDER_STATUS_LABEL: Record<string, string> = {
  CREATED: "待支付",
  PAID: "已支付"
};

function formatOrderStatus(status: string) {
  const key = String(status ?? "");
  return ORDER_STATUS_LABEL[key] ?? key;
}

function userDisplayName(row: OrderView) {
  const name = typeof row.userNickname === "string" ? row.userNickname.trim() : "";
  return name || `用户#${row.userId}`;
}

async function reload() {
  loading.value = true;
  try {
    const res = await listOrders({ page: page.value.page, pageSize: page.value.pageSize });
    items.value = res.items;
    page.value.total = res.total;
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

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>订单</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
      </div>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%" table-layout="fixed">
      <el-table-column prop="orderId" label="ID" width="80" align="center" />
      <el-table-column prop="orderNo" label="订单号" width="200" show-overflow-tooltip />
      <el-table-column label="用户" width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="user">
            <el-avatar :size="28" :src="row.userAvatarUrl || undefined">
              {{ userDisplayName(row).slice(0, 1) }}
            </el-avatar>
            <div class="user__meta">
              <div class="user__name">{{ userDisplayName(row) }}</div>
              <div class="user__id">ID: {{ row.userId }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="planName" label="套餐" width="140" show-overflow-tooltip />
      <el-table-column prop="amountCent" label="金额(元)" width="120">
        <template #default="{ row }">¥ {{ formatMoneyCent(row.amountCent) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">{{ formatOrderStatus(row.status) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="支付时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.paidAt) }}</template>
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
  width: 100%;
}

.pager :deep(.el-pagination) {
  width: 100%;
  justify-content: space-between;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.user__meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.user__name {
  font-weight: 700;
  color: #111827;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user__id {
  font-size: 12px;
  color: #6b7280;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
