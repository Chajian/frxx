<template>
  <el-config-provider :locale="locale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import en from 'element-plus/es/locale/lang/en';
import { usePreferencesStore } from '@/stores/preferences';

// 初始化偏好设置 store，确保主题在启动时应用
const preferencesStore = usePreferencesStore();

const locale = computed(() => {
  return preferencesStore.locale === 'zh-CN' ? zhCn : en;
});

// 监听暗色模式变化，在 html 上设置 dark 类
watch(
  () => preferencesStore.theme,
  (theme) => {
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  },
  { immediate: true }
);

// 监听布局变化，在 html 上设置 data-layout 属性
watch(
  () => preferencesStore.layout,
  (layout) => {
    document.documentElement.dataset.layout = layout;
  },
  { immediate: true }
);
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  width: 100%;
  height: 100vh;
}

/* 全局暗色模式样式 */
html.dark {
  color-scheme: dark;
}

/* 暗色模式下的 Element Plus 组件样式增强 */
html.dark .el-table {
  --el-table-bg-color: var(--el-bg-color);
  --el-table-tr-bg-color: var(--el-bg-color);
  --el-table-header-bg-color: var(--el-fill-color-light);
  --el-table-row-hover-bg-color: var(--el-fill-color);
  --el-table-border-color: var(--el-border-color);
}

html.dark .el-dialog {
  --el-dialog-bg-color: var(--el-bg-color);
}

html.dark .el-drawer {
  --el-drawer-bg-color: var(--el-bg-color);
}

html.dark .el-message-box {
  --el-messagebox-bg-color: var(--el-bg-color);
}

html.dark .el-popover {
  --el-popover-bg-color: var(--el-bg-color);
}

html.dark .el-dropdown-menu {
  --el-dropdown-menu-bg-color: var(--el-bg-color);
}

html.dark .el-select-dropdown {
  --el-select-dropdown-bg-color: var(--el-bg-color);
}

html.dark .el-pagination {
  --el-pagination-bg-color: var(--el-bg-color);
}

/* 暗色模式过渡动画 */
html,
html.dark {
  transition: background-color 0.3s ease, color 0.3s ease;
}
</style>
