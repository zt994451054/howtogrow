<script setup lang="ts">
import { computed } from "vue";
import { formatStatus } from "@/utils/format";

const props = defineProps<{ status: number }>();

const type = computed(() => (props.status === 1 ? "success" : "info"));
const label = computed(() => formatStatus(props.status));
</script>

<template>
  <el-tag :type="type" size="small">{{ label }}</el-tag>
</template>

