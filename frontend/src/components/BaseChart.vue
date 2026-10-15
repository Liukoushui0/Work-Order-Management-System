<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '320px' },
  loading: { type: Boolean, default: false }
})

const container = ref(null)
let chart = null

function render() {
  if (!container.value) return
  if (!chart) {
    chart = echarts.init(container.value)
  }
  chart.setOption(props.option, true)
}

function resize() {
  if (chart) chart.resize()
}

onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

watch(() => props.option, render, { deep: true })
</script>

<template>
  <div v-loading="loading" class="base-chart" :style="{ height }">
    <div ref="container" class="base-chart__canvas" />
  </div>
</template>

<style scoped>
.base-chart {
  width: 100%;
}

.base-chart__canvas {
  width: 100%;
  height: 100%;
}
</style>
