<script setup>
import { computed } from 'vue'

import { priorityLabel, priorityTagType, statusLabel, statusTagType } from '@/utils/format'

const props = defineProps({
  /** status | priority */
  kind: { type: String, default: 'status' },
  value: { type: String, default: '' }
})

const label = computed(() =>
  props.kind === 'priority' ? priorityLabel(props.value) : statusLabel(props.value)
)
const type = computed(() =>
  props.kind === 'priority' ? priorityTagType(props.value) : statusTagType(props.value)
)
</script>

<template>
  <el-tag :type="type" effect="light" size="small">{{ label }}</el-tag>
</template>
