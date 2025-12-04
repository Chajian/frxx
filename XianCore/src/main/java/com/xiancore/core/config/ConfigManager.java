package com.xiancore.core.config;

import com.xiancore.XianCore;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理器
 * 负责加载和管理所有配置文件
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class ConfigManager {

    private final XianCore plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    // 配置文件名称常量
    public static final String MAIN_CONFIG = "config.yml";
    public static final String CULTIVATION_CONFIG = "cultivation.yml";
    public static final String FORGE_CONFIG = "forge.yml";
    public static final String FATE_CONFIG = "fate.yml";
    public static final String SECT_CONFIG = "sect.yml";
    public static final String SECT_TASK_CONFIG = "sect_task.yml";  // 宗门任务配置
    public static final String SKILL_CONFIG = "skill.yml";
    public static final String SKILL_SHOP_CONFIG = "skill_shop.yml";  // 功法商店配置
    public static final String MESSAGES_CONFIG = "messages.yml";

    public ConfigManager(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载所有配置文件
     */
    public void loadConfigs() {
        plugin.getLogger().info("正在加载配置文件...");

        // 创建数据文件夹
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        // 加载主配置
        loadConfig(MAIN_CONFIG);
        loadConfig(CULTIVATION_CONFIG);
        loadConfig(FORGE_CONFIG);
        loadConfig(FATE_CONFIG);
        loadConfig(SECT_CONFIG);
        loadConfig(SECT_TASK_CONFIG);  // 加载宗门任务配置
        loadConfig(SKILL_CONFIG);
        loadConfig(SKILL_SHOP_CONFIG);  // 加载功法商店配置
        loadConfig(MESSAGES_CONFIG);

        plugin.getLogger().info(String.format("§a已加载 %d 个配置文件", configs.size()));
    }

    /**
     * 加载单个配置文件
     *
     * @param fileName 文件名
     */
    private void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        configFiles.put(fileName, file);

        // 如果文件不存在，从资源中复制
        if (!file.exists()) {
            saveDefaultConfig(fileName);
        }

        // 加载配置
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(fileName, config);

        plugin.getLogger().info(String.format("  §a✓ 加载配置: %s", fileName));
    }

    /**
     * 保存默认配置文件
     *
     * @param fileName 文件名
     */
    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);

        // 尝试从资源中复制
        try (InputStream in = plugin.getResource(fileName)) {
            if (in != null) {
                Files.copy(in, file.toPath());
                plugin.getLogger().info(String.format("  §a✓ 创建默认配置: %s", fileName));
            } else {
                // 如果资源不存在，创建空配置文件
                file.createNewFile();
                plugin.getLogger().warning(String.format("  §e! 资源文件不存在，创建空配置: %s", fileName));
            }
        } catch (IOException e) {
            plugin.getLogger().severe(String.format("  §c✗ 保存配置文件失败: %s", fileName));
            e.printStackTrace();
        }
    }

    /**
     * 获取配置文件
     *
     * @param fileName 文件名（支持带或不带 .yml 后缀）
     * @return 配置对象
     */
    public FileConfiguration getConfig(String fileName) {
        // 先尝试直接获取
        FileConfiguration config = configs.get(fileName);
        if (config != null) {
            return config;
        }

        // 如果不带 .yml 后缀，尝试添加后缀再查找
        if (!fileName.endsWith(".yml")) {
            config = configs.get(fileName + ".yml");
            if (config != null) {
                return config;
            }
        }

        // 如果带 .yml 后缀，尝试去掉后缀再查找
        if (fileName.endsWith(".yml")) {
            String nameWithoutExt = fileName.substring(0, fileName.length() - 4);
            config = configs.get(nameWithoutExt);
            if (config != null) {
                return config;
            }
        }

        // 都找不到，返回默认配置
        return plugin.getConfig();
    }

    /**
     * 保存配置文件
     *
     * @param fileName 文件名
     */
    public void saveConfig(String fileName) {
        FileConfiguration config = configs.get(fileName);
        File file = configFiles.get(fileName);

        if (config != null && file != null) {
            try {
                config.save(file);
                plugin.getLogger().info(String.format("§a保存配置: %s", fileName));
            } catch (IOException e) {
                plugin.getLogger().severe(String.format("§c保存配置文件失败: %s", fileName));
                e.printStackTrace();
            }
        }
    }

    /**
     * 重载所有配置文件
     */
    public void reloadConfigs() {
        configs.clear();
        configFiles.clear();
        loadConfigs();
    }

    /**
     * 重载单个配置文件
     *
     * @param fileName 文件名
     */
    public void reloadConfig(String fileName) {
        File file = configFiles.get(fileName);
        if (file != null && file.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            configs.put(fileName, config);
            plugin.getLogger().info(String.format("§a重载配置: %s", fileName));
        }
    }
}
