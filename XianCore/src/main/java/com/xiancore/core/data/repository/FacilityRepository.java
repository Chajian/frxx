package com.xiancore.core.data.repository;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.facilities.SectFacilityData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * 设施数据仓储
 * 负责设施数据的加载、保存（仅支持文件存储）
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class FacilityRepository {

    private final XianCore plugin;

    // 重试配置
    private static final int RETRY_COUNT = 3;
    private static final long RETRY_DELAY_MS = 100;

    public FacilityRepository(XianCore plugin) {
        this.plugin = plugin;
    }

    // ==================== 公开 API ====================

    /**
     * 保存设施数据
     *
     * @param data 设施数据
     */
    public void save(SectFacilityData data) {
        File sectDir = new File(plugin.getDataFolder(), "sects");
        if (!sectDir.exists()) {
            sectDir.mkdirs();
        }

        File file = new File(sectDir, data.getSectId() + "_facilities.yml");

        // 重试机制
        Exception lastException = null;

        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                YamlConfiguration config = new YamlConfiguration();

                config.set("sect_id", data.getSectId());
                config.set("last_updated", data.getLastUpdated());

                // 保存设施等级
                for (var entry : data.getFacilityLevels().entrySet()) {
                    config.set("facility_levels." + entry.getKey(), entry.getValue());
                }

                config.save(file);
                return; // 成功，退出

            } catch (Exception e) {
                lastException = e;
                plugin.getLogger().warning("§e保存设施数据失败，重试 " + (i + 1) + "/" + RETRY_COUNT +
                        ": " + data.getSectId());

                // 等待后重试
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ignored) {
                }
            }
        }

        // 所有重试都失败
        plugin.getLogger().severe("§c保存设施数据彻底失败: " + data.getSectId());
        if (lastException != null) {
            lastException.printStackTrace();
        }
    }

    /**
     * 加载设施数据
     *
     * @param sectId 宗门 ID
     * @return 设施数据，如果不存在返回 null
     */
    public SectFacilityData load(int sectId) {
        File file = new File(plugin.getDataFolder(), "sects/" + sectId + "_facilities.yml");

        if (!file.exists()) {
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            SectFacilityData data = new SectFacilityData(sectId);
            data.setLastUpdated(config.getLong("last_updated", System.currentTimeMillis()));

            // 加载设施等级
            if (config.contains("facility_levels")) {
                ConfigurationSection levelsSection = config.getConfigurationSection("facility_levels");

                if (levelsSection != null) {
                    for (String facilityId : levelsSection.getKeys(false)) {
                        int level = config.getInt("facility_levels." + facilityId, 0);
                        data.getFacilityLevels().put(facilityId, level);
                    }
                }
            }

            return data;

        } catch (Exception e) {
            plugin.getLogger().warning("§e加载设施数据失败: " + sectId);
            e.printStackTrace();
            return null;
        }
    }
}
