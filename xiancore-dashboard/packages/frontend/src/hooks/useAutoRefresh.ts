import { ref, onMounted, onUnmounted, watch } from 'vue';
import { usePreferencesStore } from '@/stores/preferences';

/**
 * 自动刷新 Hook
 * 根据偏好设置自动刷新数据
 */
export function useAutoRefresh(fetchData: () => void | Promise<void>) {
  const preferencesStore = usePreferencesStore();
  const intervalId = ref<number | null>(null);

  // 启动自动刷新
  const startAutoRefresh = () => {
    stopAutoRefresh();

    const interval = preferencesStore.autoRefreshInterval;
    if (interval > 0) {
      intervalId.value = window.setInterval(() => {
        fetchData();
      }, interval * 1000);
    }
  };

  // 停止自动刷新
  const stopAutoRefresh = () => {
    if (intervalId.value !== null) {
      clearInterval(intervalId.value);
      intervalId.value = null;
    }
  };

  // 监听刷新间隔变化
  watch(
    () => preferencesStore.autoRefreshInterval,
    () => {
      startAutoRefresh();
    }
  );

  // 组件挂载时启动
  onMounted(() => {
    startAutoRefresh();
  });

  // 组件卸载时清理
  onUnmounted(() => {
    stopAutoRefresh();
  });

  return {
    startAutoRefresh,
    stopAutoRefresh,
  };
}
