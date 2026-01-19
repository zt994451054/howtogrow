<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import StatusTag from "@/components/StatusTag.vue";
import { createBanner, deleteBanner, listBanners, updateBanner, type BannerUpsertRequest, type BannerView } from "@/api/admin/banners";
import { uploadPublic } from "@/api/admin/uploads";
import { sanitizeHtmlForDisplay } from "@/utils/sanitizeHtml";
import RichTextEditor from "@/components/RichTextEditor.vue";

type PageState = {
  page: number;
  pageSize: number;
  total: number;
};

const loading = ref(false);
const items = ref<BannerView[]>([]);
const page = ref<PageState>({ page: 1, pageSize: 20, total: 0 });

type BannerFilter = {
  status: number | null;
  keyword: string;
};

const filter = reactive<BannerFilter>({
  status: null,
  keyword: ""
});

const dialogVisible = ref(false);
const editing = ref<BannerView | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<BannerUpsertRequest>({
  title: "",
  imageUrl: "",
  htmlContent: "",
  status: 0,
  sortNo: 0
});

const previewHtml = computed(() => sanitizeHtmlForDisplay(form.htmlContent || ""));

const rules: FormRules = {
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  imageUrl: [{ required: true, message: "请上传封面图", trigger: "change" }],
  htmlContent: [{ required: true, message: "请输入内容", trigger: "change" }]
};

async function reload() {
  loading.value = true;
  try {
    const res = await listBanners({
      page: page.value.page,
      pageSize: page.value.pageSize,
      status: filter.status ?? undefined,
      keyword: filter.keyword.trim() || undefined
    });
    items.value = res.items;
    page.value.total = res.total;
    if (page.value.page > 1 && page.value.total > 0 && items.value.length === 0) {
      page.value.page -= 1;
      await reload();
    }
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
  filter.status = null;
  filter.keyword = "";
  page.value.page = 1;
  void reload();
}

function openCreate() {
  editing.value = null;
  form.title = "";
  form.imageUrl = "";
  form.htmlContent = "";
  form.status = 1;
  form.sortNo = 0;
  dialogVisible.value = true;
}

function openEdit(row: BannerView) {
  editing.value = row;
  form.title = row.title;
  form.imageUrl = row.imageUrl;
  form.htmlContent = row.htmlContent;
  form.status = row.status;
  form.sortNo = row.sortNo;
  dialogVisible.value = true;
}

async function save() {
  const ok = await formRef.value?.validate();
  if (!ok) return;

  const request: BannerUpsertRequest = {
    title: form.title.trim(),
    imageUrl: form.imageUrl.trim(),
    htmlContent: form.htmlContent.trim(),
    status: form.status,
    sortNo: Number(form.sortNo)
  };

  if (editing.value) {
    await updateBanner(editing.value.id, request);
    ElMessage.success("已保存");
  } else {
    await createBanner(request);
    ElMessage.success("已创建");
  }

  dialogVisible.value = false;
  await reload();
}

async function remove(row: BannerView) {
  await ElMessageBox.confirm("确认删除该 Banner？", "提示", { type: "warning" });
  await deleteBanner(row.id);
  ElMessage.success("已删除");
  await reload();
}

async function onUploadCover(options: { file: File; onSuccess: (res: unknown) => void; onError: (err: unknown) => void }) {
  try {
    const url = await uploadPublic(options.file);
    form.imageUrl = url;
    options.onSuccess({ url });
  } catch (e) {
    options.onError(e);
  }
}

function getH5PreviewUrl(id: number): string {
  return new URL(`/h5/banners/${id}`, window.location.origin).toString();
}

function openH5Preview(row: BannerView): void {
  window.open(getH5PreviewUrl(row.id), "_blank", "noopener,noreferrer");
}

async function copyH5PreviewUrl(row: BannerView): Promise<void> {
  const url = getH5PreviewUrl(row.id);
  try {
    await navigator.clipboard.writeText(url);
    ElMessage.success("已复制链接");
  } catch {
    window.prompt("复制链接：", url);
  }
}

onMounted(() => {
  void reload();
});
</script>

<template>
  <div class="page">
    <div class="header">
      <h3>Banner</h3>
      <div class="actions">
        <el-button @click="reload">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增</el-button>
      </div>
    </div>

    <div class="filters">
      <el-form :inline="true" label-width="60px" @submit.prevent>
        <el-form-item label="状态">
          <el-select v-model="filter.status" placeholder="全部" style="width: 120px" clearable>
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filter.keyword" placeholder="标题" clearable style="width: 220px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="items" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column label="顺序" width="90">
        <template #default="{ row }">{{ row.sortNo }}</template>
      </el-table-column>
      <el-table-column prop="title" label="标题" width="220" />
      <el-table-column label="封面" width="140">
        <template #default="{ row }">
          <el-image :src="row.imageUrl" style="width: 64px; height: 36px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <StatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="H5" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openH5Preview(row)">预览</el-button>
          <el-button link @click="copyH5PreviewUrl(row)">复制链接</el-button>
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

    <el-drawer v-model="dialogVisible" :title="editing ? '编辑 Banner' : '新增 Banner'" size="92%" destroy-on-close>
      <div class="drawer-body">
        <div class="drawer-left">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
            <el-form-item label="标题" prop="title">
              <el-input v-model="form.title" />
            </el-form-item>
            <el-form-item label="顺序" prop="sortNo">
              <el-input-number v-model="form.sortNo" :step="1" />
            </el-form-item>
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">上架</el-radio>
                <el-radio :label="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="封面图" prop="imageUrl">
              <div style="display: flex; gap: 12px; align-items: center">
                <el-upload :show-file-list="false" :http-request="onUploadCover" accept="image/*">
                  <el-button>上传</el-button>
                </el-upload>
                <el-image v-if="form.imageUrl" :src="form.imageUrl" style="width: 96px; height: 54px" fit="cover" />
              </div>
            </el-form-item>
            <el-form-item label="富文本" prop="htmlContent">
              <RichTextEditor v-model="form.htmlContent" :upload="uploadPublic" placeholder="请输入内容（支持文字/图片/视频）" :min-height="520" />
            </el-form-item>
          </el-form>
        </div>

        <div class="drawer-right">
          <div class="preview-header">
            <div class="preview-title">预览</div>
            <el-text type="info">H5/WebView 展示效果预览</el-text>
          </div>
          <div class="preview-box">
            <div class="preview-content" v-html="previewHtml"></div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </div>
      </template>
    </el-drawer>
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

.drawer-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.drawer-left {
  flex: 1;
  min-width: 360px;
}

.drawer-right {
  flex: 1;
  min-width: 320px;
}

.preview-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8px;
}

.preview-title {
  font-weight: 600;
}

.preview-box {
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 12px;
  min-height: 320px;
  background: #fff;
}

.preview-content :deep(img),
.preview-content :deep(video),
.preview-content :deep(audio) {
  max-width: 100%;
}

.preview-content :deep(video),
.preview-content :deep(audio) {
  width: 100%;
}

.preview-content :deep(p) {
  line-height: 1.7;
  margin: 10px 0;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

@media (max-width: 900px) {
  .drawer-body {
    flex-direction: column;
  }
  .drawer-left,
  .drawer-right {
    min-width: auto;
    width: 100%;
  }
}
</style>
