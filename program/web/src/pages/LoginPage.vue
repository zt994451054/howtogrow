<script setup lang="ts">
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import { adminLogin } from "@/api/admin/auth";
import { useAuthStore } from "@/stores/auth";
import { usePermissionStore } from "@/stores/permission";

const router = useRouter();
const auth = useAuthStore();
const permission = usePermissionStore();

const loading = ref(false);
const form = reactive({
  username: "",
  password: ""
});

async function onSubmit() {
  if (!form.username || !form.password) {
    ElMessage.warning("请输入用户名和密码");
    return;
  }

  loading.value = true;
  try {
    const res = await adminLogin({ username: form.username, password: form.password });
    auth.setToken(res.token);
    await permission.bootstrap();
    await router.replace({ path: "/" });
    ElMessage.success("登录成功");
  } catch {
    // Error message is already handled globally in the HTTP interceptor.
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="wrap">
    <div class="card">
      <h2 class="title">运营后台登录</h2>
      <el-form label-position="top" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">登录</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.wrap {
  height: 100%;
  display: grid;
  place-items: center;
}

.card {
  width: 360px;
  padding: 18px 18px 22px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.title {
  margin: 0 0 12px;
  font-size: 18px;
  color: #303133;
}
</style>
