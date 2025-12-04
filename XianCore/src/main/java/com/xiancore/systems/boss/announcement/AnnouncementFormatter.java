package com.xiancore.systems.boss.announcement;

import lombok.Getter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公告格式化器
 * 将公告模板转换为最终消息，处理占位符、颜色、格式等
 *
 * 支持以下功能:
 * - 占位符替换 {key}
 * - 颜色代码转换 & 和 §
 * - 自定义格式注册
 * - 模板继承
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-14
 */
@Getter
public class AnnouncementFormatter {

    // ==================== 配置常量 ====================

    /** 占位符模式 */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    /** Bukkit颜色代码 */
    private static final String COLOR_CHAR = "§";

    // ==================== 颜色代码映射 ====================

    private static final Map<String, String> COLOR_NAMES = new HashMap<>();

    static {
        // 基础颜色
        COLOR_NAMES.put("&0", "§0");  // 黑色
        COLOR_NAMES.put("&1", "§1");  // 深蓝色
        COLOR_NAMES.put("&2", "§2");  // 深绿色
        COLOR_NAMES.put("&3", "§3");  // 深青色
        COLOR_NAMES.put("&4", "§4");  // 深红色
        COLOR_NAMES.put("&5", "§5");  // 紫色
        COLOR_NAMES.put("&6", "§6");  // 金色
        COLOR_NAMES.put("&7", "§7");  // 浅灰色
        COLOR_NAMES.put("&8", "§8");  // 深灰色
        COLOR_NAMES.put("&9", "§9");  // 蓝色
        COLOR_NAMES.put("&a", "§a");  // 绿色
        COLOR_NAMES.put("&b", "§b");  // 青色
        COLOR_NAMES.put("&c", "§c");  // 红色
        COLOR_NAMES.put("&d", "§d");  // 淡紫色
        COLOR_NAMES.put("&e", "§e");  // 黄色
        COLOR_NAMES.put("&f", "§f");  // 白色

        // 格式
        COLOR_NAMES.put("&l", "§l");  // 加粗
        COLOR_NAMES.put("&m", "§m");  // 删除线
        COLOR_NAMES.put("&n", "§n");  // 下划线
        COLOR_NAMES.put("&o", "§o");  // 斜体
        COLOR_NAMES.put("&r", "§r");  // 重置
    }

    // ==================== 内部状态 ====================

    /** 预设模板库 */
    private final Map<String, String> templates;

    /** 自定义格式库 */
    private final Map<String, String> customFormats;

    /** 颜色主题库 */
    private final Map<String, String> colorThemes;

    // ==================== 构造函数 ====================

    public AnnouncementFormatter() {
        this.templates = new HashMap<>();
        this.customFormats = new HashMap<>();
        this.colorThemes = new HashMap<>();
        initializeDefaultThemes();
    }

    // ==================== 初始化 ====================

    /**
     * 初始化默认颜色主题
     */
    private void initializeDefaultThemes() {
        // 传奇主题 (红色加粗)
        colorThemes.put("legendary", "&c&l");

        // 史诗主题 (紫色)
        colorThemes.put("epic", "&d");

        // 稀有主题 (青色)
        colorThemes.put("rare", "&b");

        // 普通主题 (绿色)
        colorThemes.put("common", "&a");

        // 警告主题 (黄色)
        colorThemes.put("warning", "&e");

        // 错误主题 (红色)
        colorThemes.put("error", "&c");
    }

    // ==================== 格式化方法 ====================

    /**
     * 格式化公告消息
     *
     * @param announcement 公告对象
     * @return 格式化后的消息
     */
    public String format(BossAnnouncement announcement) {
        if (announcement == null) {
            return "";
        }

        String template = announcement.getTemplate();
        if (template == null || template.isEmpty()) {
            // 使用默认模板
            template = getDefaultTemplate(announcement.getType());
        }

        // 替换占位符
        String formatted = replacePlaceholders(template, announcement.getAllParams());

        // 应用颜色
        return applyColors(formatted);
    }

    /**
     * 格式化模板字符串
     *
     * @param template 模板文本
     * @param params 参数映射
     * @return 格式化后的消息
     */
    public String formatTemplate(String template, Map<String, String> params) {
        String replaced = replacePlaceholders(template, params);
        return applyColors(replaced);
    }

    /**
     * 替换占位符 {key}
     *
     * @param template 模板文本
     * @param params 参数映射
     * @return 替换后的文本
     */
    public String replacePlaceholders(String template, Map<String, String> params) {
        if (template == null || template.isEmpty() || params == null) {
            return template;
        }

        StringBuffer sb = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = params.getOrDefault(key, "{" + key + "}");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 应用颜色代码
     * 支持 &X 格式 (Minecraft风格)
     *
     * @param message 消息文本
     * @return 应用颜色后的消息
     */
    public String applyColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        String result = message;

        // 替换所有颜色代码
        for (Map.Entry<String, String> entry : COLOR_NAMES.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        return result;
    }

    // ==================== 模板管理 ====================

    /**
     * 注册自定义模板
     *
     * @param name 模板名称
     * @param template 模板文本
     */
    public void registerTemplate(String name, String template) {
        templates.put(name, template);
    }

    /**
     * 获取已注册的模板
     *
     * @param name 模板名称
     * @return 模板文本
     */
    public String getTemplate(String name) {
        return templates.get(name);
    }

    /**
     * 获取默认模板
     *
     * @param type 公告类型
     * @return 模板文本
     */
    public String getDefaultTemplate(BossAnnouncement.AnnouncementType type) {
        if (type == null) {
            return "";
        }

        return switch (type) {
            case SPAWN -> "&8[&c⚔&8] &6{boss_name}&8(&e{boss_tier}&8)在&a{world}&8 位置&a({x}, {y}, {z})&8出现！";
            case KILLED -> "&8[&c✔&8] &6{boss_name}&8被&a{killer}&8击杀！品质: &e{quality}&8 | 经验: &b{exp_reward}";
            case RARE_DROP -> "&8[&e✨&8] &e稀有掉落！&a{lucky_player}&8获得了&d{drop_name}&8！";
            case LEVEL_UP -> "&8[&d⭐&8] &a{boss_type}&8已被击杀&c{kill_count}&8次！";
            case PERIODIC -> "&8--- &7存活Boss列表 &8---";
        };
    }

    // ==================== 格式库管理 ====================

    /**
     * 注册自定义格式
     *
     * @param name 格式名称
     * @param format 格式文本
     */
    public void registerCustomFormat(String name, String format) {
        customFormats.put(name, format);
    }

    /**
     * 获取自定义格式
     *
     * @param name 格式名称
     * @return 格式文本
     */
    public String getCustomFormat(String name) {
        return customFormats.get(name);
    }

    // ==================== 主题管理 ====================

    /**
     * 注册颜色主题
     *
     * @param name 主题名称
     * @param colorCode 颜色代码
     */
    public void registerColorTheme(String name, String colorCode) {
        colorThemes.put(name, colorCode);
    }

    /**
     * 获取颜色主题
     *
     * @param name 主题名称
     * @return 颜色代码
     */
    public String getColorTheme(String name) {
        return colorThemes.getOrDefault(name, "");
    }

    /**
     * 应用主题颜色到文本
     *
     * @param text 文本
     * @param theme 主题名称
     * @return 应用颜色后的文本
     */
    public String applyTheme(String text, String theme) {
        String themeColor = getColorTheme(theme);
        if (themeColor == null || themeColor.isEmpty()) {
            return text;
        }
        return applyColors(themeColor + text + "&r");
    }

    // ==================== 信息样式方法 ====================

    /**
     * 获取优先级1样式 (传奇Boss - 红色加粗)
     */
    public String getPriority1Style() {
        return applyColors("&c&l");
    }

    /**
     * 获取优先级2样式 (其他重要事件 - 黄色)
     */
    public String getPriority2Style() {
        return applyColors("&e");
    }

    /**
     * 获取优先级3样式 (普通事件 - 绿色)
     */
    public String getPriority3Style() {
        return applyColors("&a");
    }

    /**
     * 创建标题样式文本
     *
     * @param title 标题
     * @return 样式化的标题
     */
    public String createTitle(String title) {
        return applyColors("&8[&c⚔&8] &6" + title);
    }

    /**
     * 创建成功提示
     *
     * @param message 消息
     * @return 样式化的成功消息
     */
    public String createSuccessMessage(String message) {
        return applyColors("&a✔ &f" + message);
    }

    /**
     * 创建警告提示
     *
     * @param message 消息
     * @return 样式化的警告消息
     */
    public String createWarningMessage(String message) {
        return applyColors("&e⚠ &f" + message);
    }

    /**
     * 创建错误提示
     *
     * @param message 消息
     * @return 样式化的错误消息
     */
    public String createErrorMessage(String message) {
        return applyColors("&c✘ &f" + message);
    }

    // ==================== 工具方法 ====================

    /**
     * 移除所有颜色代码
     *
     * @param text 文本
     * @return 无颜色的文本
     */
    public String stripColors(String text) {
        if (text == null) {
            return "";
        }

        // 移除 Minecraft颜色代码
        return text.replaceAll("§[0-9a-fklmnor]", "")
                   .replaceAll("&[0-9a-fklmnor]", "");
    }

    /**
     * 获取文本的显示长度 (不计颜色代码)
     *
     * @param text 文本
     * @return 长度
     */
    public int getDisplayLength(String text) {
        return stripColors(text).length();
    }

    /**
     * 截断文本到指定长度 (不计颜色代码)
     *
     * @param text 文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public String truncateText(String text, int maxLength) {
        String clean = stripColors(text);
        if (clean.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + applyColors("&f...");
    }

    /**
     * 创建分隔符
     *
     * @param length 长度
     * @param char_ 字符
     * @return 分隔符
     */
    public String createSeparator(int length, String char_) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(char_);
        }
        return applyColors("&8" + sb);
    }
}
