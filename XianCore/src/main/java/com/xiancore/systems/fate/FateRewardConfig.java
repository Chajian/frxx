package com.xiancore.systems.fate;

import com.xiancore.XianCore;
import com.xiancore.systems.fate.FateSystem.FateType;
import com.xiancore.systems.fate.rewards.FateReward;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 奇遇奖励配置管理器
 * 负责从 fate.yml 加载和管理奖励配置
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class FateRewardConfig {

    private final XianCore plugin;
    private final Map<FateType, FateRewardPool> rewardPools;
    private boolean loaded = false;

    public FateRewardConfig(XianCore plugin) {
        this.plugin = plugin;
        this.rewardPools = new HashMap<>();
    }

    /**
     * 从配置文件加载奖励配置
     */
    public void load() {
        try {
            FileConfiguration config = plugin.getConfigManager().getConfig("fate");
            
            if (config == null) {
                plugin.getLogger().warning("[奇遇系统] 无法加载 fate.yml，使用默认奖励");
                loadDefaultRewards();
                loaded = true;
                return;
            }

            rewardPools.clear();

            // 加载各类型奇遇的奖励池
            loadRewardPool(config, FateType.SMALL, "fate-types.small");
            loadRewardPool(config, FateType.MEDIUM, "fate-types.medium");
            loadRewardPool(config, FateType.LARGE, "fate-types.large");
            loadRewardPool(config, FateType.DESTINY, "fate-types.destiny");

            loaded = true;
            plugin.getLogger().info("  §a✓ 奇遇奖励配置加载完成 (共 " + getTotalRewardCount() + " 个奖励)");

        } catch (Exception e) {
            plugin.getLogger().severe("[奇遇系统] 加载奖励配置失败: " + e.getMessage());
            e.printStackTrace();
            plugin.getLogger().warning("[奇遇系统] 降级到默认奖励配置");
            loadDefaultRewards();
            loaded = true;
        }
    }

    /**
     * 加载单个奇遇类型的奖励池
     */
    private void loadRewardPool(FileConfiguration config, FateType type, String path) {
        try {
            ConfigurationSection section = config.getConfigurationSection(path);
            if (section == null) {
                plugin.getLogger().warning("[奇遇系统] 未找到奖励配置: " + path + "，使用默认配置");
                loadDefaultRewardPool(type);
                return;
            }

            FateRewardPool pool = new FateRewardPool();

            // 读取权重
            pool.setWeight(section.getDouble("weight", getDefaultWeight(type)));

            // 读取是否广播
            pool.setBroadcast(section.getBoolean("broadcast", type == FateType.DESTINY));

            // 读取奖励列表
            if (section.contains("rewards")) {
                List<?> rewardsList = section.getList("rewards");
                if (rewardsList != null && !rewardsList.isEmpty()) {
                    for (Object rewardObj : rewardsList) {
                        if (rewardObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> rewardMap = (Map<String, Object>) rewardObj;

                            try {
                                FateReward reward = FateReward.fromMap(rewardMap, plugin);
                                if (reward != null) {
                                    pool.addReward(reward);
                                }
                            } catch (Exception e) {
                                plugin.getLogger().warning("[奇遇系统] 解析奖励配置失败 (" + type + "): " + e.getMessage());
                                if (plugin.isDebugMode()) {
                                    plugin.getLogger().warning("  配置内容: " + rewardMap);
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    plugin.getLogger().warning("[奇遇系统] " + type + " 的奖励列表为空，使用默认奖励");
                    loadDefaultRewardPool(type);
                    return;
                }
            } else {
                plugin.getLogger().warning("[奇遇系统] " + type + " 缺少 rewards 配置，使用默认奖励");
                loadDefaultRewardPool(type);
                return;
            }

            // 保存奖励池
            rewardPools.put(type, pool);

            if (plugin.isDebugMode()) {
                plugin.getLogger().fine("[奇遇系统] 加载 " + type + " 奖励池: " + pool.getRewards().size() + " 个奖励");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("[奇遇系统] 加载 " + type + " 配置失败: " + e.getMessage());
            e.printStackTrace();
            loadDefaultRewardPool(type);
        }
    }

    /**
     * 发放奖励
     *
     * @param player 玩家
     * @param type   奇遇类型
     */
    public void giveRewards(Player player, FateType type) {
        if (!loaded) {
            plugin.getLogger().warning("[奇遇系统] 配置未加载，无法发放奖励");
            return;
        }

        FateRewardPool pool = rewardPools.get(type);
        if (pool == null) {
            plugin.getLogger().warning("[奇遇系统] 未找到奇遇类型的奖励池: " + type);
            return;
        }

        try {
            // 发放奖励池中的所有奖励
            List<String> rewardMessages = new ArrayList<>();
            
            for (FateReward reward : pool.getRewards()) {
                try {
                    String message = reward.give(player);
                    if (message != null && !message.isEmpty()) {
                        rewardMessages.add(message);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[奇遇系统] 发放奖励失败: " + e.getMessage());
                    if (plugin.isDebugMode()) {
                        e.printStackTrace();
                    }
                }
            }

            // 显示奖励消息
            if (!rewardMessages.isEmpty()) {
                player.sendMessage("§7获得奖励:");
                for (String msg : rewardMessages) {
                    player.sendMessage("  " + msg);
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("[奇遇系统] 发放 " + type + " 奖励时出现严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查奖励池是否应该广播
     */
    public boolean shouldBroadcast(FateType type) {
        FateRewardPool pool = rewardPools.get(type);
        return pool != null && pool.isBroadcast();
    }

    /**
     * 加载默认奖励配置（当配置文件不可用时）
     */
    private void loadDefaultRewards() {
        plugin.getLogger().info("[奇遇系统] 加载默认奖励配置...");
        
        loadDefaultRewardPool(FateType.SMALL);
        loadDefaultRewardPool(FateType.MEDIUM);
        loadDefaultRewardPool(FateType.LARGE);
        loadDefaultRewardPool(FateType.DESTINY);
    }

    /**
     * 加载单个类型的默认奖励池
     */
    private void loadDefaultRewardPool(FateType type) {
        FateRewardPool pool = new FateRewardPool();
        pool.setWeight(getDefaultWeight(type));
        pool.setBroadcast(type == FateType.DESTINY);

        // 根据类型添加默认的数值奖励
        switch (type) {
            case SMALL:
                // 小奇遇默认奖励
                pool.addReward(createDefaultNumericReward("qi", 5000, 10000));
                pool.addReward(createDefaultNumericReward("spirit-stones", 50, 100));
                break;

            case MEDIUM:
                // 中奇遇默认奖励
                pool.addReward(createDefaultNumericReward("qi", 15000, 30000));
                pool.addReward(createDefaultNumericReward("spirit-stones", 150, 300));
                pool.addReward(createDefaultNumericReward("skill-points", 5, 10));
                break;

            case LARGE:
                // 大奇遇默认奖励
                pool.addReward(createDefaultNumericReward("qi", 50000, 100000));
                pool.addReward(createDefaultNumericReward("spirit-stones", 500, 1000));
                pool.addReward(createDefaultNumericReward("skill-points", 20, 30));
                pool.addReward(createDefaultNumericReward("contribution", 100, 200));
                break;

            case DESTINY:
                // 命运奇遇默认奖励
                pool.addReward(createDefaultNumericReward("qi", 200000, 400000));
                pool.addReward(createDefaultNumericReward("spirit-stones", 2000, 4000));
                pool.addReward(createDefaultNumericReward("skill-points", 50, 100));
                pool.addReward(createDefaultNumericReward("contribution", 500, 1000));
                pool.addReward(createDefaultNumericReward("level", 5, 10));
                pool.addReward(createDefaultNumericReward("active-qi", 100, 100));
                break;
        }

        rewardPools.put(type, pool);
    }

    /**
     * 创建默认的数值奖励（临时方法，等NumericReward创建后替换）
     */
    private FateReward createDefaultNumericReward(String type, long min, long max) {
        // 这是临时实现，等 NumericReward 创建后会被替换
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("min-amount", min);
        map.put("max-amount", max);
        map.put("chance", 1.0);
        
        try {
            return FateReward.fromMap(map, plugin);
        } catch (Exception e) {
            plugin.getLogger().warning("[奇遇系统] 创建默认奖励失败: " + type);
            return null;
        }
    }

    /**
     * 获取默认权重
     */
    private double getDefaultWeight(FateType type) {
        return switch (type) {
            case SMALL -> 70.0;
            case MEDIUM -> 25.0;
            case LARGE -> 4.5;
            case DESTINY -> 0.5;
        };
    }

    /**
     * 获取总奖励数量
     */
    private int getTotalRewardCount() {
        int count = 0;
        for (FateRewardPool pool : rewardPools.values()) {
            if (pool != null) {
                count += pool.getRewards().size();
            }
        }
        return count;
    }

    /**
     * 重载配置
     */
    public void reload() {
        plugin.getLogger().info("[奇遇系统] 重载奖励配置...");
        plugin.getConfigManager().reloadConfig("fate");
        load();
    }

    /**
     * 检查是否已加载
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * 获取奖励池
     */
    public FateRewardPool getRewardPool(FateType type) {
        return rewardPools.get(type);
    }
}

