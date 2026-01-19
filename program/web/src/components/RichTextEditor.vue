<script setup lang="ts">
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import { Quill, QuillEditor } from "@vueup/vue-quill";
import type QuillInstance from "quill";
import "@vueup/vue-quill/dist/vue-quill.snow.css";

type Upload = (file: File) => Promise<string>;

let html5VideoRegistered = false;
function ensureHtml5VideoRegistered(): void {
  if (html5VideoRegistered) return;
  html5VideoRegistered = true;

  try {
    const BlockEmbed = Quill.import("blots/block/embed");
    class Html5VideoBlot extends BlockEmbed {
      static blotName = "html5Video";
      static tagName = "video";
      static className = "ql-html5Video";

      static create(value: unknown) {
        const node = super.create() as HTMLVideoElement;
        const src = typeof value === "string" ? value : "";
        node.setAttribute("src", src);
        node.setAttribute("controls", "");
        node.setAttribute("preload", "metadata");
        node.setAttribute("playsinline", "");
        node.setAttribute("webkit-playsinline", "");
        return node;
      }

      static value(node: Element) {
        return node.getAttribute("src") ?? "";
      }
    }

    Quill.register(Html5VideoBlot, true);
  } catch {
    // ignore: if Quill import/register fails, editor still works (without video embed).
  }
}

ensureHtml5VideoRegistered();

const props = defineProps<{
  modelValue: string;
  upload: Upload;
  placeholder?: string;
  minHeight?: number;
}>();

const emit = defineEmits<{
  (e: "update:modelValue", value: string): void;
}>();

const editorRef = ref<InstanceType<typeof QuillEditor> | null>(null);
const internal = computed({
  get: () => props.modelValue,
  set: (value: string) => emit("update:modelValue", value ?? "")
});

async function pickFile(accept: string): Promise<File | null> {
  return await new Promise((resolve) => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = accept;
    input.onchange = () => resolve(input.files?.[0] ?? null);
    input.click();
  });
}

function getQuillOrThrow(): QuillInstance {
  const q = editorRef.value?.getQuill?.();
  if (!q) throw new Error("Editor not ready");
  return q as QuillInstance;
}

async function insertImage(): Promise<void> {
  try {
    const file = await pickFile("image/*");
    if (!file) return;
    const url = await props.upload(file);
    const quill = getQuillOrThrow();
    const range = quill.getSelection(true) ?? { index: quill.getLength(), length: 0 };
    quill.insertEmbed(range.index, "image", url, "user");
    quill.setSelection(range.index + 1, 0, "user");
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "图片上传失败");
  }
}

async function insertVideo(): Promise<void> {
  try {
    const file = await pickFile("video/*");
    if (!file) return;
    const url = await props.upload(file);
    const quill = getQuillOrThrow();
    const range = quill.getSelection(true) ?? { index: quill.getLength(), length: 0 };
    quill.insertEmbed(range.index, "html5Video", url, "user");
    quill.insertText(range.index + 1, "\n", "user");
    quill.setSelection(range.index + 2, 0, "user");
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "视频上传失败");
  }
}

const modules = computed(() => ({
  toolbar: {
    container: [
      [{ header: [1, 2, 3, false] }],
      ["bold", "italic", "underline", "strike"],
      [{ list: "ordered" }, { list: "bullet" }],
      [{ align: [] }],
      [{ color: [] }, { background: [] }],
      ["link"],
      ["image", "video"],
      ["clean"]
    ],
    handlers: {
      image: () => void insertImage(),
      video: () => void insertVideo()
    }
  }
}));

const options = computed(() => ({
  modules: modules.value
}));

const minHeightPx = computed(() => {
  const minHeight = typeof props.minHeight === "number" && props.minHeight > 0 ? props.minHeight : 360;
  return `${minHeight}px`;
});
</script>

<template>
  <div class="rte-root" :style="{ '--rte-min-height': minHeightPx }">
    <QuillEditor
      ref="editorRef"
      class="rte-editor"
      v-model:content="internal"
      contentType="html"
      theme="snow"
      :options="options"
      :placeholder="placeholder ?? '请输入内容...'"
    />
  </div>
</template>

<style scoped>
.rte-root {
  width: 100%;
  flex: 1;
}

.rte-editor {
  width: 100%;
}

.rte-root :deep(.ql-toolbar) {
  width: 100%;
}

.rte-root :deep(.ql-container) {
  width: 100%;
  min-height: var(--rte-min-height);
}

.rte-root :deep(.ql-editor) {
  min-height: var(--rte-min-height);
}

.rte-root :deep(.ql-editor video),
.rte-root :deep(.ql-editor .ql-html5Video) {
  max-width: 100%;
  width: 100%;
  height: auto;
  display: block;
}
</style>
