// 布局类型
export type LayoutType =
  | 'sidebar-nav'      // 侧边栏导航
  | 'header-nav'       // 顶部导航
  | 'mixed-nav'        // 混合导航
  | 'sidebar-mixed-nav' // 侧边栏混合导航
  | 'full-content';    // 全屏内容

// 内置主题
export type BuiltinTheme =
  | 'default'
  | 'deep-blue'
  | 'deep-green'
  | 'gray'
  | 'green'
  | 'neutral'
  | 'orange'
  | 'pink'
  | 'rose'
  | 'sky-blue'
  | 'slate'
  | 'violet'
  | 'yellow'
  | 'zinc'
  | 'custom';

// 主题颜色配置
export interface ThemeConfig {
  name: string;
  color: string;
  description: string;
}

// 内置主题颜色预设
export const themePresets: Record<BuiltinTheme, ThemeConfig> = {
  default: {
    name: '默认蓝',
    color: '#409EFF',
    description: '经典的Element Plus蓝色主题',
  },
  'deep-blue': {
    name: '深蓝',
    color: '#1890ff',
    description: '深邃的蓝色，专业稳重',
  },
  'deep-green': {
    name: '深绿',
    color: '#0e9f6e',
    description: '深绿色，自然宁静',
  },
  gray: {
    name: '优雅灰',
    color: '#71717a',
    description: '低调优雅的灰色',
  },
  green: {
    name: '翠绿',
    color: '#67c23a',
    description: '清新的绿色',
  },
  neutral: {
    name: '中性',
    color: '#737373',
    description: '中性色调，百搭耐看',
  },
  orange: {
    name: '活力橙',
    color: '#f97316',
    description: '充满活力的橙色',
  },
  pink: {
    name: '粉红',
    color: '#ec4899',
    description: '温柔的粉红色',
  },
  rose: {
    name: '玫瑰',
    color: '#f43f5e',
    description: '浪漫的玫瑰色',
  },
  'sky-blue': {
    name: '天蓝',
    color: '#0ea5e9',
    description: '清澈的天蓝色',
  },
  slate: {
    name: '石板',
    color: '#64748b',
    description: '沉稳的石板色',
  },
  violet: {
    name: '紫罗兰',
    color: '#8b5cf6',
    description: '神秘的紫罗兰',
  },
  yellow: {
    name: '明黄',
    color: '#eab308',
    description: '明亮的黄色',
  },
  zinc: {
    name: '锌灰',
    color: '#71717a',
    description: '现代感的锌灰色',
  },
  custom: {
    name: '自定义',
    color: '#409EFF',
    description: '自定义颜色',
  },
};

// 布局类型配置
export interface LayoutConfig {
  type: string;
  name: string;
  description: string;
  icon: string; // SVG path or component name
}

export const layoutOptions: LayoutConfig[] = [
  {
    type: 'sidebar-nav',
    name: '侧边栏导航',
    description: '经典的侧边栏布局，左侧菜单右侧内容',
    icon: 'sidebar-nav',
  },
  {
    type: 'header-nav',
    name: '顶部导航',
    description: '顶部菜单栏布局，适合页面较少的应用',
    icon: 'header-nav',
  },
  {
    type: 'mixed-nav',
    name: '混合导航',
    description: '顶部+侧边栏混合布局',
    icon: 'mixed-nav',
  },
  {
    type: 'sidebar-mixed-nav',
    name: '侧边栏混合',
    description: '侧边栏分组混合布局',
    icon: 'sidebar-mixed-nav',
  },
  {
    type: 'full-content',
    name: '全屏内容',
    description: '无边框全屏内容布局',
    icon: 'full-content',
  },
];

// 将 hex 颜色转换为 RGB
function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const num = parseInt(hex.replace('#', ''), 16);
  return {
    r: (num >> 16) & 0xff,
    g: (num >> 8) & 0xff,
    b: num & 0xff,
  };
}

// 将 RGB 转换为 hex
function rgbToHex(r: number, g: number, b: number): string {
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`;
}

// 混合颜色与白色（产生浅色变体）
function mixWithWhite(hex: string, weight: number): string {
  const { r, g, b } = hexToRgb(hex);
  const mixR = Math.round(r + (255 - r) * weight);
  const mixG = Math.round(g + (255 - g) * weight);
  const mixB = Math.round(b + (255 - b) * weight);
  return rgbToHex(mixR, mixG, mixB);
}

// 混合颜色与黑色（产生深色变体）
export function mixWithBlack(hex: string, weight: number): string {
  const { r, g, b } = hexToRgb(hex);
  const mixR = Math.round(r * (1 - weight));
  const mixG = Math.round(g * (1 - weight));
  const mixB = Math.round(b * (1 - weight));
  return rgbToHex(mixR, mixG, mixB);
}

// 应用主题颜色到CSS变量
export function applyThemeColor(color: string) {
  const root = document.documentElement;

  // Element Plus 主色
  root.style.setProperty('--el-color-primary', color);

  // 设置 Element Plus 的颜色变体 (light-1 到 light-9 与白色混合)
  root.style.setProperty('--el-color-primary-light-1', mixWithWhite(color, 0.1));
  root.style.setProperty('--el-color-primary-light-2', mixWithWhite(color, 0.2));
  root.style.setProperty('--el-color-primary-light-3', mixWithWhite(color, 0.3));
  root.style.setProperty('--el-color-primary-light-4', mixWithWhite(color, 0.4));
  root.style.setProperty('--el-color-primary-light-5', mixWithWhite(color, 0.5));
  root.style.setProperty('--el-color-primary-light-6', mixWithWhite(color, 0.6));
  root.style.setProperty('--el-color-primary-light-7', mixWithWhite(color, 0.7));
  root.style.setProperty('--el-color-primary-light-8', mixWithWhite(color, 0.8));
  root.style.setProperty('--el-color-primary-light-9', mixWithWhite(color, 0.9));

  // 深色变体 (与黑色混合)
  root.style.setProperty('--el-color-primary-dark-1', mixWithBlack(color, 0.1));
  root.style.setProperty('--el-color-primary-dark-2', mixWithBlack(color, 0.2));

  console.log('[Theme] Applied primary color:', color);
}

// 应用侧边栏主题颜色
export function applySidebarTheme(primaryColor: string, isDark: boolean) {
  const root = document.documentElement;

  if (isDark) {
    // 暗色模式：使用深灰色系
    root.style.setProperty('--sidebar-bg-color', '#1f2937');
    root.style.setProperty('--sidebar-logo-bg-color', '#111827');
    root.style.setProperty('--sidebar-text-color', '#e5e7eb');
    root.style.setProperty('--sidebar-active-bg-color', mixWithBlack(primaryColor, 0.3));
  } else {
    // 亮色模式：基于主题色生成侧边栏颜色
    const sidebarBg = mixWithBlack(primaryColor, 0.75); // 主色深色变体
    const logoBg = mixWithBlack(primaryColor, 0.8);     // 更深的 Logo 背景
    root.style.setProperty('--sidebar-bg-color', sidebarBg);
    root.style.setProperty('--sidebar-logo-bg-color', logoBg);
    root.style.setProperty('--sidebar-text-color', '#bfcbd9');
    root.style.setProperty('--sidebar-active-bg-color', mixWithBlack(primaryColor, 0.6));
  }

  // 通用设置
  root.style.setProperty('--sidebar-active-text-color', primaryColor);

  console.log('[Theme] Applied sidebar theme:', { primaryColor, isDark });
}

// 应用暗色模式的 Element Plus CSS 变量
export function applyDarkMode(isDark: boolean) {
  const root = document.documentElement;

  if (isDark) {
    // 暗色模式背景色
    root.style.setProperty('--el-bg-color', '#141414');
    root.style.setProperty('--el-bg-color-page', '#0a0a0a');
    root.style.setProperty('--el-bg-color-overlay', '#1d1d1d');

    // 暗色模式文字颜色
    root.style.setProperty('--el-text-color-primary', '#E5EAF3');
    root.style.setProperty('--el-text-color-regular', '#CFD3DC');
    root.style.setProperty('--el-text-color-secondary', '#A3A6AD');
    root.style.setProperty('--el-text-color-placeholder', '#8D9095');

    // 暗色模式边框颜色
    root.style.setProperty('--el-border-color', '#4C4D4F');
    root.style.setProperty('--el-border-color-light', '#414243');
    root.style.setProperty('--el-border-color-lighter', '#363637');
    root.style.setProperty('--el-border-color-extra-light', '#2B2B2C');
    root.style.setProperty('--el-border-color-dark', '#58585B');

    // 暗色模式填充色
    root.style.setProperty('--el-fill-color', '#303030');
    root.style.setProperty('--el-fill-color-light', '#262727');
    root.style.setProperty('--el-fill-color-lighter', '#1D1D1D');
    root.style.setProperty('--el-fill-color-blank', '#141414');

    // 暗色模式遮罩
    root.style.setProperty('--el-mask-color', 'rgba(0, 0, 0, 0.8)');

    // 暗色模式 box-shadow
    root.style.setProperty('--el-box-shadow', '0px 12px 32px 4px rgba(0, 0, 0, 0.36), 0px 8px 20px rgba(0, 0, 0, 0.72)');
    root.style.setProperty('--el-box-shadow-light', '0px 0px 12px rgba(0, 0, 0, 0.72)');
    root.style.setProperty('--el-box-shadow-lighter', '0px 0px 6px rgba(0, 0, 0, 0.72)');
  } else {
    // 亮色模式背景色
    root.style.setProperty('--el-bg-color', '#ffffff');
    root.style.setProperty('--el-bg-color-page', '#f2f3f5');
    root.style.setProperty('--el-bg-color-overlay', '#ffffff');

    // 亮色模式文字颜色
    root.style.setProperty('--el-text-color-primary', '#303133');
    root.style.setProperty('--el-text-color-regular', '#606266');
    root.style.setProperty('--el-text-color-secondary', '#909399');
    root.style.setProperty('--el-text-color-placeholder', '#a8abb2');

    // 亮色模式边框颜色
    root.style.setProperty('--el-border-color', '#dcdfe6');
    root.style.setProperty('--el-border-color-light', '#e4e7ed');
    root.style.setProperty('--el-border-color-lighter', '#ebeef5');
    root.style.setProperty('--el-border-color-extra-light', '#f2f6fc');
    root.style.setProperty('--el-border-color-dark', '#d4d7de');

    // 亮色模式填充色
    root.style.setProperty('--el-fill-color', '#f0f2f5');
    root.style.setProperty('--el-fill-color-light', '#f5f7fa');
    root.style.setProperty('--el-fill-color-lighter', '#fafafa');
    root.style.setProperty('--el-fill-color-blank', '#ffffff');

    // 亮色模式遮罩
    root.style.setProperty('--el-mask-color', 'rgba(255, 255, 255, 0.9)');

    // 亮色模式 box-shadow
    root.style.setProperty('--el-box-shadow', '0px 12px 32px 4px rgba(0, 0, 0, 0.04), 0px 8px 20px rgba(0, 0, 0, 0.08)');
    root.style.setProperty('--el-box-shadow-light', '0px 0px 12px rgba(0, 0, 0, 0.12)');
    root.style.setProperty('--el-box-shadow-lighter', '0px 0px 6px rgba(0, 0, 0, 0.12)');
  }

  console.log('[Theme] Applied dark mode:', isDark);
}
