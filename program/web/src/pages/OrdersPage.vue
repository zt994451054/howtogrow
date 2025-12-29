<script setup lang="ts">
import { onMounted, ref } from "vue";
import { listOrders, type OrderView } from "@/api/admin/orders";
import { formatMoneyCent } from "@/utils/format";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

const loading = ref(false);
const items = ref<OrderView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });

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

    <el-table :data="items" v-loading="loading" style="width: 100%">
      <el-table-column prop="orderId" label="ID" width="90" />
      <el-table-column prop="orderNo" label="订单号" width="220" />
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="planName" label="套餐" width="160" />
      <el-table-column prop="amountCent" label="金额(元)" width="120">
        <template #default="{ row }">¥ {{ formatMoneyCent(row.amountCent) }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="createdAt" label="创建时间" width="220" />
      <el-table-column prop="paidAt" label="支付时间" width="220" />
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
  display: flex;
  justify-content: flex-end;
}
</style>

