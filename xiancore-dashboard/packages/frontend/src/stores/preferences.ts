import { defineStore } from 'pinia';
import { ref, watch, computed } from 'vue';
import { themePresets, applyThemeColor, applyDarkMode, applySidebarTheme, type BuiltinTheme, type LayoutType } from '@/config/theme';

// 重新导出类型以便其他地方使用
export type { BuiltinTheme, LayoutType } from '@/config/theme';

export interface PreferencesState {
  // 主题设置
  theme: 'light' | 'dark';
  // 内置主题
  builtinTheme: BuiltinTheme;
  // 自定义主题颜色
  primaryColor: string;
  // 布局类型
  layout: LayoutType;
  // 语言设置
  locale: 'zh-CN' | 'en-US';
  // 侧边栏折叠
  sidebarCollapsed: boolean;
  // 数据自动刷新间隔（秒，0 表示禁用）
  autoRefreshInterval: number;
  // 显示设置
  showBreadcrumb: boolean;
  // 动画效果
  enableAnimation: boolean;
}

const STORAGE_KEY = 'xiancore-preferences';

// 默认配置
const defaultPreferences: PreferencesState = {
  theme: 'light',
  builtinTheme: 'default',
  primaryColor: '#409EFF',
  layout: 'sidebar-nav',
  locale: 'zh-CN',
  sidebarCollapsed: false,
  autoRefreshInterval: 0,
  showBreadcrumb: true,
  enableAnimation: true,
};

// 从 localStorage 加载配置
function loadPreferences(): PreferencesState {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored) {
      return { ...defaultPreferences, ...JSON.parse(stored) };
    }
  } catch (error) {
    console.error('Failed to load preferences:', error);
  }
  return { ...defaultPreferences };
}

// 保存配置到 localStorage
function savePreferences(preferences: PreferencesState) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(preferences));
  } catch (error) {
    console.error('Failed to save preferences:', error);
  }
}

export const usePreferencesStore = defineStore('preferences', () => {
  // 加载初始配置
  const initialPreferences = loadPreferences();

  const theme = ref<'light' | 'dark'>(initialPreferences.theme);
  const builtinTheme = ref<BuiltinTheme>(initialPreferences.builtinTheme);
  const primaryColor = ref(initialPreferences.primaryColor);
  const layout = ref<LayoutType>(initialPreferences.layout);
  const locale = ref<'zh-CN' | 'en-US'>(initialPreferences.locale);
  const sidebarCollapsed = ref(initialPreferences.sidebarCollapsed);
  const autoRefreshInterval = ref(initialPreferences.autoRefreshInterval);
  const showBreadcrumb = ref(initialPreferences.showBreadcrumb);
  const enableAnimation = ref(initialPreferences.enableAnimation);

  // 监听配置变化，自动保存
  watch(
    [theme, builtinTheme, primaryColor, layout, locale, sidebarCollapsed, autoRefreshInterval, showBreadcrumb, enableAnimation],
    () => {
      savePreferences({
        theme: theme.value,
        builtinTheme: builtinTheme.value,
        primaryColor: primaryColor.value,
        layout: layout.value,
        locale: locale.value,
        sidebarCollapsed: sidebarCollapsed.value,
        autoRefreshInterval: autoRefreshInterval.value,
        showBreadcrumb: showBreadcrumb.value,
        enableAnimation: enableAnimation.value,
      });
    },
    { deep: true }
  );

  // 监听主题变化，应用主题颜色
  watch(
    [builtinTheme, primaryColor],
    ([newTheme, newColor]) => {
      console.log('[Preferences] Theme watch triggered:', { newTheme, newColor });
      const color = newTheme === 'custom'
        ? newColor
        : themePresets[newTheme].color;
      console.log('[Preferences] Applying color:', color);
      applyThemeColor(color);
    },
    { immediate: true } // 立即执行，应用初始主题
  );

  // 监听暗色模式变化，应用暗色模式 CSS 变量
  watch(
    theme,
    (newTheme) => {
      console.log('[Preferences] Dark mode watch triggered:', newTheme);
      applyDarkMode(newTheme === 'dark');
    },
    { immediate: true }
  );

  // 监听主题变化，应用侧边栏颜色
  watch(
    [builtinTheme, primaryColor, theme],
    ([newBuiltinTheme, newColor, newTheme]) => {
      console.log('[Preferences] Sidebar theme watch triggered:', { newBuiltinTheme, newColor, newTheme });
      const color = newBuiltinTheme === 'custom'
        ? newColor
        : themePresets[newBuiltinTheme].color;
      applySidebarTheme(color, newTheme === 'dark');
    },
    { immediate: true }
  );

  // 重置为默认配置
  function resetPreferences() {
    theme.value = defaultPreferences.theme;
    builtinTheme.value = defaultPreferences.builtinTheme;
    primaryColor.value = defaultPreferences.primaryColor;
    layout.value = defaultPreferences.layout;
    locale.value = defaultPreferences.locale;
    sidebarCollapsed.value = defaultPreferences.sidebarCollapsed;
    autoRefreshInterval.value = defaultPreferences.autoRefreshInterval;
    showBreadcrumb.value = defaultPreferences.showBreadcrumb;
    enableAnimation.value = defaultPreferences.enableAnimation;
  }

  // 切换主题
  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light';
  }

  // 切换语言
  function toggleLocale() {
    locale.value = locale.value === 'zh-CN' ? 'en-US' : 'zh-CN';
  }

  // 切换侧边栏
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  // 设置内置主题
  function setBuiltinTheme(newTheme: BuiltinTheme) {
    console.log('[Preferences] setBuiltinTheme called:', newTheme);
    builtinTheme.value = newTheme;
  }

  // 设置布局
  function setLayout(newLayout: LayoutType) {
    console.log('[Preferences] setLayout called:', newLayout);
    layout.value = newLayout;
  }

  // 设置自定义颜色
  function setPrimaryColor(color: string) {
    console.log('[Preferences] setPrimaryColor called:', color);
    primaryColor.value = color;
  }

  // 获取当前实际的主题颜色值
  const currentPrimaryColor = computed(() => {
    return builtinTheme.value === 'custom'
      ? primaryColor.value
      : themePresets[builtinTheme.value].color;
  });

  return {
    // 状态
    theme,
    builtinTheme,
    primaryColor,
    currentPrimaryColor,
    layout,
    locale,
    sidebarCollapsed,
    autoRefreshInterval,
    showBreadcrumb,
    enableAnimation,

    // 方法
    resetPreferences,
    toggleTheme,
    toggleLocale,
    toggleSidebar,
    setBuiltinTheme,
    setLayout,
    setPrimaryColor,
  };
});
