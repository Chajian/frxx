package com.xiancore.ecosystem;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 插件生态系统管理器 - 与第三方插件集成
 * Plugin Ecosystem Manager - Integration with Third-party Plugins
 *
 * @author XianCore
 * @version 1.0
 */
public class PluginEcosystemManager {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, PluginIntegration> integrations = new ConcurrentHashMap<>();
    private final VaultIntegration vaultIntegration;
    private final DiscordNotifier discordNotifier;
    private final PlaceholderAPIIntegration placeholderAPIIntegration;

    /**
     * 插件集成信息
     */
    public static class PluginIntegration {
        public String pluginName;
        public String pluginId;
        public String version;
        public PluginStatus status;
        public IntegrationLevel integrationLevel;
        public List<String> features;
        public long integratedTime;
        public Map<String, Object> config;

        public enum PluginStatus {
            AVAILABLE,      // 可用
            INTEGRATED,     // 已集成
            ERROR,         // 错误
            DISABLED       // 禁用
        }

        public enum IntegrationLevel {
            BASIC,         // 基础集成
            STANDARD,      // 标准集成
            ADVANCED,      // 高级集成
            FULL           // 完整集成
        }

        public PluginIntegration(String pluginName, String pluginId, String version) {
            this.pluginName = pluginName;
            this.pluginId = pluginId;
            this.version = version;
            this.status = PluginStatus.AVAILABLE;
            this.integrationLevel = IntegrationLevel.BASIC;
            this.features = new ArrayList<>();
            this.integratedTime = System.currentTimeMillis();
            this.config = new ConcurrentHashMap<>();
        }
    }

    /**
     * 构造函数
     */
    public PluginEcosystemManager() {
        this.vaultIntegration = new VaultIntegration();
        this.discordNotifier = new DiscordNotifier();
        this.placeholderAPIIntegration = new PlaceholderAPIIntegration();
        initializeDefaultIntegrations();
        logger.info("✓ PluginEcosystemManager已初始化");
    }

    /**
     * 初始化默认集成
     */
    private void initializeDefaultIntegrations() {
        // Vault经济系统
        PluginIntegration vault = new PluginIntegration("Vault", "vault", "1.7");
        vault.features.addAll(List.of(
                "玩家余额管理",
                "Boss击杀奖励",
                "经济交易记录",
                "富豪排行榜"
        ));
        vault.integrationLevel = PluginIntegration.IntegrationLevel.ADVANCED;
        integrations.put("vault", vault);

        // Discord
        PluginIntegration discord = new PluginIntegration("Discord Bot", "discord", "1.0");
        discord.features.addAll(List.of(
                "Boss事件通知",
                "玩家成就推送",
                "经济变化播报",
                "系统警报"
        ));
        discord.integrationLevel = PluginIntegration.IntegrationLevel.ADVANCED;
        integrations.put("discord", discord);

        // PlaceholderAPI
        PluginIntegration placeholder = new PluginIntegration("PlaceholderAPI", "placeholderapi", "2.11");
        placeholder.features.addAll(List.of(
                "玩家统计占位符",
                "经济占位符",
                "排名占位符",
                "服务器状态占位符"
        ));
        placeholder.integrationLevel = PluginIntegration.IntegrationLevel.STANDARD;
        integrations.put("placeholderapi", placeholder);

        // LiteBans
        PluginIntegration litebans = new PluginIntegration("LiteBans", "litebans", "2.5");
        litebans.features.addAll(List.of(
                "作弊检测",
                "违规处罚",
                "封禁管理"
        ));
        litebans.integrationLevel = PluginIntegration.IntegrationLevel.BASIC;
        integrations.put("litebans", litebans);

        // EssentialsX
        PluginIntegration essentials = new PluginIntegration("EssentialsX", "essentialsx", "2.20");
        essentials.features.addAll(List.of(
                "传送系统",
                "主城管理",
                "物品管理"
        ));
        essentials.integrationLevel = PluginIntegration.IntegrationLevel.BASIC;
        integrations.put("essentials", essentials);

        // WorldEdit
        PluginIntegration worldedit = new PluginIntegration("WorldEdit", "worldedit", "7.2");
        worldedit.features.addAll(List.of(
                "地形生成",
                "区域管理",
                "结构导出"
        ));
        worldedit.integrationLevel = PluginIntegration.IntegrationLevel.BASIC;
        integrations.put("worldedit", worldedit);

        logger.info("✓ " + integrations.size() + "个插件已注册");
    }

    /**
     * 初始化插件集成
     */
    public boolean initializePlugin(String pluginId, Map<String, Object> config) {
        PluginIntegration integration = integrations.get(pluginId);
        if (integration == null) {
            logger.warning("⚠ 插件不存在: " + pluginId);
            return false;
        }

        try {
            integration.config.putAll(config);

            // 根据插件类型初始化
            switch (pluginId) {
                case "vault":
                    vaultIntegration.initializeVault((boolean) config.getOrDefault("enabled", true));
                    break;
                case "discord":
                    String botToken = (String) config.getOrDefault("bot-token", "");
                    String webhookUrl = (String) config.getOrDefault("webhook-url", "");
                    discordNotifier.initializeDiscord(botToken, webhookUrl,
                            (boolean) config.getOrDefault("enabled", true));
                    break;
                case "placeholderapi":
                    placeholderAPIIntegration.initializePlaceholderAPI(
                            (boolean) config.getOrDefault("enabled", true));
                    break;
            }

            integration.status = PluginIntegration.PluginStatus.INTEGRATED;
            logger.info("✓ 插件已集成: " + integration.pluginName);
            return true;

        } catch (Exception e) {
            integration.status = PluginIntegration.PluginStatus.ERROR;
            logger.warning("⚠ 插件集成失败: " + pluginId + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * 禁用插件集成
     */
    public void disablePlugin(String pluginId) {
        PluginIntegration integration = integrations.get(pluginId);
        if (integration != null) {
            integration.status = PluginIntegration.PluginStatus.DISABLED;
            logger.info("✓ 插件已禁用: " + integration.pluginName);
        }
    }

    /**
     * 获取Vault集成
     */
    public VaultIntegration getVaultIntegration() {
        return vaultIntegration;
    }

    /**
     * 获取Discord通知器
     */
    public DiscordNotifier getDiscordNotifier() {
        return discordNotifier;
    }

    /**
     * 获取PlaceholderAPI集成
     */
    public PlaceholderAPIIntegration getPlaceholderAPIIntegration() {
        return placeholderAPIIntegration;
    }

    /**
     * 获取插件集成信息
     */
    public PluginIntegration getPluginIntegration(String pluginId) {
        return integrations.get(pluginId);
    }

    /**
     * 获取所有插件集成
     */
    public Collection<PluginIntegration> getAllIntegrations() {
        return integrations.values();
    }

    /**
     * 获取已集成的插件
     */
    public List<PluginIntegration> getIntegratedPlugins() {
        return integrations.values().stream()
                .filter(p -> p.status == PluginIntegration.PluginStatus.INTEGRATED)
                .toList();
    }

    /**
     * 获取可用的插件
     */
    public List<PluginIntegration> getAvailablePlugins() {
        return integrations.values().stream()
                .filter(p -> p.status == PluginIntegration.PluginStatus.AVAILABLE)
                .toList();
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_plugins", integrations.size());
        stats.put("integrated_plugins", getIntegratedPlugins().size());
        stats.put("available_plugins", getAvailablePlugins().size());

        // 按状态统计
        Map<String, Integer> statusCount = new HashMap<>();
        for (PluginIntegration integration : integrations.values()) {
            statusCount.merge(integration.status.name(), 1, Integer::sum);
        }
        stats.put("plugins_by_status", statusCount);

        // 按集成级别统计
        Map<String, Integer> levelCount = new HashMap<>();
        for (PluginIntegration integration : integrations.values()) {
            levelCount.merge(integration.integrationLevel.name(), 1, Integer::sum);
        }
        stats.put("plugins_by_level", levelCount);

        // 包含子系统统计
        stats.put("vault_stats", vaultIntegration.getStatistics());
        stats.put("discord_stats", discordNotifier.getStatistics());
        stats.put("placeholder_stats", placeholderAPIIntegration.getStatistics());

        return stats;
    }

    /**
     * 获取插件详细信息
     */
    public Map<String, Object> getPluginDetails(String pluginId) {
        PluginIntegration integration = integrations.get(pluginId);
        if (integration == null) return null;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("name", integration.pluginName);
        details.put("id", integration.pluginId);
        details.put("version", integration.version);
        details.put("status", integration.status.name());
        details.put("integration_level", integration.integrationLevel.name());
        details.put("features", integration.features);
        details.put("config", integration.config);

        return details;
    }

    /**
     * 重置所有集成
     */
    public void reset() {
        integrations.values().forEach(p -> p.status = PluginIntegration.PluginStatus.AVAILABLE);
        vaultIntegration.reset();
        discordNotifier.reset();
        placeholderAPIIntegration.reset();
        logger.info("✓ 所有插件集成已重置");
    }
}
