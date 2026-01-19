<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { getMiniprogramBannerDetail } from "@/api/miniprogram/banners";
import { sanitizeHtmlForDisplay } from "@/utils/sanitizeHtml";

const route = useRoute();

const loading = ref(false);
const errorMessage = ref<string>("");
const title = ref<string>("");
const rawHtml = ref<string>("");

function parsePositiveInt(value: unknown): number | null {
  if (typeof value === "number" && Number.isInteger(value) && value > 0) return value;
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  if (!/^[1-9]\d*$/.test(trimmed)) return null;
  return Number(trimmed);
}

function parseBannerIdFromPath(pathname: string): number | null {
  const m = pathname.match(/\/h5\/banners\/(\d+)(?:\/)?$/);
  if (!m) return null;
  return parsePositiveInt(m[1]);
}

const bannerId = computed<number | null>(() => {
  return (
    parsePositiveInt(route.params.id) ??
    parsePositiveInt(route.query.id) ??
    parseBannerIdFromPath(window.location.pathname)
  );
});

const displayHtml = computed(() => sanitizeHtmlForDisplay(rawHtml.value));

async function load(): Promise<void> {
  if (!bannerId.value) {
    errorMessage.value = "无效的 Banner ID";
    title.value = "";
    rawHtml.value = "";
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await getMiniprogramBannerDetail(bannerId.value);
    title.value = data.title || "Banner";
    rawHtml.value = data.htmlContent || "";
    document.title = title.value;
  } catch (e) {
    title.value = "";
    rawHtml.value = "";
    errorMessage.value = e instanceof Error ? e.message : "加载失败";
    document.title = "Banner";
  } finally {
    loading.value = false;
  }
}

watch(bannerId, () => void load(), { immediate: true });
</script>

<template>
  <div class="h5-page">
    <div class="h5-container">
      <div v-if="title" class="h5-title">{{ title }}</div>
      <div v-if="errorMessage" class="h5-error">{{ errorMessage }}</div>
      <div v-else-if="loading" class="h5-loading">加载中...</div>
      <div v-else class="h5-content" v-html="displayHtml"></div>
    </div>
  </div>
</template>

<style scoped>
.h5-page {
  min-height: 100vh;
  background: #fff;
  color: #111;
}

.h5-container {
  padding: 16px;
  max-width: 720px;
  margin: 0 auto;
}

.h5-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.h5-error {
  color: #d93025;
  font-size: 14px;
  line-height: 1.6;
}

.h5-loading {
  color: #666;
  font-size: 14px;
}

.h5-content :deep(img),
.h5-content :deep(video),
.h5-content :deep(audio) {
  max-width: 100%;
}

.h5-content :deep(video),
.h5-content :deep(audio) {
  width: 100%;
}

.h5-content :deep(p) {
  line-height: 1.7;
  margin: 10px 0;
}

.h5-content :deep(h1),
.h5-content :deep(h2),
.h5-content :deep(h3),
.h5-content :deep(h4) {
  margin: 14px 0 8px;
  line-height: 1.35;
}
</style>
