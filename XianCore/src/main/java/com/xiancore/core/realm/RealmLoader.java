package com.xiancore.core.realm;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 境界配置加载器
 * 从 realms.yml 加载境界配置到 RealmRegistry
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class RealmLoader {

    private final JavaPlugin plugin;
    private final Logger logger;
    private final RealmRegistry registry;

    public RealmLoader(JavaPlugin plugin, RealmRegistry registry) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.registry = registry;
    }

    /**
     * 加载境界配置
     *
     * @return 成功加载的境界数量
     */
    public int load() {
        // 确保配置文件存在
        File configFile = new File(plugin.getDataFolder(), "realms.yml");
        if (!configFile.exists()) {
            plugin.saveResource("realms.yml", false);
            logger.info("已创建默认境界配置文件: realms.yml");
        }

        // 加载配置
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // 同时加载默认配置作为后备
        InputStream defaultStream = plugin.getResource("realms.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        // 清空现有注册
        registry.clear();

        // 加载境界
        ConfigurationSection realmsSection = config.getConfigurationSection("realms");
        if (realmsSection == null) {
            logger.severe("realms.yml 中未找到 'realms' 配置节！");
            return 0;
        }

        int loadedCount = 0;
        for (String realmId : realmsSection.getKeys(false)) {
            ConfigurationSection realmConfig = realmsSection.getConfigurationSection(realmId);
            if (realmConfig == null) {
                logger.warning("跳过无效境界配置: " + realmId);
                continue;
            }

            try {
                Realm realm = loadRealm(realmId, realmConfig);
                registry.register(realm);
                loadedCount++;
            } catch (Exception e) {
                logger.severe("加载境界配置失败 [" + realmId + "]: " + e.getMessage());
            }
        }

        logger.info("成功加载 " + loadedCount + " 个境界配置");
        return loadedCount;
    }

    /**
     * 从配置节加载单个境界
     */
    private Realm loadRealm(String id, ConfigurationSection config) {
        String name = config.getString("name", id);
        int order = config.getInt("order", 1);
        long breakthroughQi = config.getLong("breakthrough-qi", 1000L);
        double difficulty = config.getDouble("difficulty", 1.0);

        // 等级增长配置
        ConfigurationSection levelGainSection = config.getConfigurationSection("level-gain");
        int levelGainSmall = 5;
        int levelGainBig = 15;
        if (levelGainSection != null) {
            levelGainSmall = levelGainSection.getInt("small", 5);
            levelGainBig = levelGainSection.getInt("big", 15);
        }

        int skillPointReward = config.getInt("skill-point-reward", 0);
        String nextRealmId = config.getString("next", null);

        // 关键词列表
        List<String> keywords = config.getStringList("keywords");
        if (keywords.isEmpty()) {
            // 默认使用名称作为关键词
            keywords = new ArrayList<>();
            keywords.add(name);
        }

        return Realm.builder()
                .id(id)
                .name(name)
                .order(order)
                .baseBreakthroughQi(breakthroughQi)
                .difficulty(difficulty)
                .levelGainSmall(levelGainSmall)
                .levelGainBig(levelGainBig)
                .skillPointReward(skillPointReward)
                .nextRealmId(nextRealmId)
                .keywords(keywords)
                .build();
    }

    /**
     * 重新加载配置
     *
     * @return 成功加载的境界数量
     */
    public int reload() {
        logger.info("重新加载境界配置...");
        return load();
    }
}
