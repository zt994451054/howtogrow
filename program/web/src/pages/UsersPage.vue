<script setup lang="ts">
import { onMounted, ref } from "vue";
import { listUsers, type UserView } from "@/api/admin/users";
import { formatDateTime } from "@/utils/format";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

const loading = ref(false);
const items = ref<UserView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });

async function reload() {
  loading.value = true;
  try {
    const res = await listUsers({ page: page.value.page, pageSize: page.value.pageSize });
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
      <h3>用户</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
      </div>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%" table-layout="fixed">
      <el-table-column prop="userId" label="ID" width="90" align="center" />
      <el-table-column label="头像" width="70" align="center">
        <template #default="{ row }">
          <el-avatar :size="32" :src="row.avatarUrl || undefined">
            {{ String(row.nickname || row.userId).slice(0, 1) }}
          </el-avatar>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="昵称" width="160" show-overflow-tooltip />
      <el-table-column prop="wechatOpenid" label="openid" min-width="240" show-overflow-tooltip />
      <el-table-column prop="freeTrialUsed" label="免费体验" width="90" align="center">
        <template #default="{ row }">{{ row.freeTrialUsed ? "是" : "否" }}</template>
      </el-table-column>
      <el-table-column label="订阅到期" width="180">
        <template #default="{ row }">{{ formatDateTime(row.subscriptionEndAt) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
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
  display: flex;
  justify-content: flex-end;
}
</style>
