<script setup lang="ts">
import { onMounted, ref } from 'vue'
import api from '@/api'

interface HealthResponse {
  status: string
  service: string
  version: string
}

const backendStatus = ref<string>('检测后端连接中…')

onMounted(async () => {
  try {
    const { data } = await api.get<HealthResponse>('/health')
    backendStatus.value = `后端连接正常：${data.status} · ${data.service} v${data.version}`
  } catch {
    backendStatus.value = '后端未连接（请确认 Spring Boot 已在 8080 启动）'
  }
})
</script>

<template>
  <section class="home">
    <h2>欢迎使用 AI Story Co-Author</h2>
    <p class="home__desc">作者给出阶段导演指令，AI 生成连续章节，并自动维护故事状态与记忆。</p>
    <p class="home__status" :class="{ 'home__status--ok': backendStatus.includes('正常') }">
      {{ backendStatus }}
    </p>
  </section>
</template>
