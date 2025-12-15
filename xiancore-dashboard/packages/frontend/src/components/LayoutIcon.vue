<template>
  <svg
    viewBox="0 0 64 64"
    xmlns="http://www.w3.org/2000/svg"
    style="width: 64px; height: 48px"
  >
    <!-- 侧边栏导航 -->
    <g v-if="type === 'sidebar-nav'">
      <rect x="0" y="0" width="16" height="64" fill="currentColor" opacity="0.8" />
      <rect x="20" y="4" width="40" height="8" fill="currentColor" opacity="0.3" />
      <rect x="20" y="16" width="40" height="44" fill="currentColor" opacity="0.15" />
    </g>

    <!-- 顶部导航 -->
    <g v-else-if="type === 'header-nav'">
      <rect x="0" y="0" width="64" height="12" fill="currentColor" opacity="0.8" />
      <rect x="4" y="16" width="56" height="44" fill="currentColor" opacity="0.15" />
    </g>

    <!-- 混合导航 -->
    <g v-else-if="type === 'mixed-nav'">
      <rect x="0" y="0" width="64" height="12" fill="currentColor" opacity="0.8" />
      <rect x="0" y="16" width="16" height="48" fill="currentColor" opacity="0.6" />
      <rect x="20" y="16" width="40" height="48" fill="currentColor" opacity="0.15" />
    </g>

    <!-- 侧边栏混合导航 -->
    <g v-else-if="type === 'sidebar-mixed-nav'">
      <rect x="0" y="0" width="12" height="64" fill="currentColor" opacity="0.8" />
      <rect x="16" y="0" width="16" height="64" fill="currentColor" opacity="0.5" />
      <rect x="36" y="4" width="24" height="8" fill="currentColor" opacity="0.3" />
      <rect x="36" y="16" width="24" height="44" fill="currentColor" opacity="0.15" />
    </g>

    <!-- 全屏内容 -->
    <g v-else-if="type === 'full-content'">
      <rect x="0" y="0" width="64" height="64" fill="currentColor" opacity="0.15" />
      <rect x="8" y="8" width="48" height="48" fill="currentColor" opacity="0.3" />
    </g>
  </svg>
</template>

<script setup lang="ts">
defineProps<{
  type: string;
}>();
</script>

<style scoped>
svg {
  color: var(--el-color-primary);
}
</style>
