package com.xiancore.systems.fate.rewards;

import com.xiancore.XianCore;
import com.xiancore.systems.skill.Skill;
import com.xiancore.systems.skill.items.SkillBookFactory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 功法书奖励
 * 发放功法秘籍给玩家
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class SkillBookReward extends FateReward {

    private String skillId;              // 固定功法ID
    private List<String> skillPool;      // 功法池（随机选择）
    private int skillLevel;              // 功法等级
    private final Random random = new Random();

    public SkillBookReward(XianCore plugin) {
        super(plugin, "skill-book");
        this.skillLevel = 1;
        this.skillPool = new ArrayList<>();
    }

    @Override
    public String give(Player player) {
        // 概率判定
        if (!shouldGive()) {
            return null;
        }

        try {
            // 确定要给的功法ID
            String finalSkillId = determineSkillId();
            if (finalSkillId == null) {
                plugin.getLogger().warning("[奇遇系统] 无法确定功法ID（skillId和skillPool都为空）");
                return null;
            }

            // 获取功法对象
            Skill skill = plugin.getSkillSystem().getSkill(finalSkillId);
            if (skill == null) {
                plugin.getLogger().warning("[奇遇系统] 功法不存在: " + finalSkillId + "，跳过该奖励");
                return null;
            }

            // 创建功法书
            ItemStack skillBook = SkillBookFactory.createSkillBook(skill, skillLevel);
            if (skillBook == null) {
                plugin.getLogger().warning("[奇遇系统] 创建功法书失败: " + skill.getName());
                return null;
            }

            // 检查背包空间
            if (player.getInventory().firstEmpty() == -1) {
                // 背包满，掉落到地面
                player.sendMessage("§c背包已满！功法书掉落在地上");
                player.getWorld().dropItem(player.getLocation(), skillBook);
                
                if (plugin.isDebugMode()) {
                    plugin.getLogger().fine("[奇遇系统] 玩家 " + player.getName() + " 背包满，功法书已掉落");
                }
            } else {
                // 背包有空间，直接添加
                player.getInventory().addItem(skillBook);
            }

            // 返回奖励消息
            return "§5功法书: §e" + skill.getName() + " §7(Lv." + skillLevel + ")";

        } catch (Exception e) {
            plugin.getLogger().warning("[奇遇系统] 发放功法书奖励失败: " + e.getMessage());
            if (plugin.isDebugMode()) {
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * 确定要给的功法ID
     * 优先使用固定ID，否则从功法池随机选择
     */
    private String determineSkillId() {
        // 1. 如果指定了固定功法ID，使用固定ID
        if (skillId != null && !skillId.isEmpty()) {
            return skillId;
        }

        // 2. 如果提供了功法池，随机选择
        if (skillPool != null && !skillPool.isEmpty()) {
            return skillPool.get(random.nextInt(skillPool.size()));
        }

        // 3. 都没有，返回null
        return null;
    }

    /**
     * 从配置Map创建功法书奖励
     *
     * @param map    配置Map
     * @param plugin 插件实例
     * @return 功法书奖励对象
     */
    public static SkillBookReward fromMap(Map<String, Object> map, XianCore plugin) {
        SkillBookReward reward = new SkillBookReward(plugin);

        // 读取固定功法ID（可选）
        if (map.containsKey("skill-id")) {
            Object skillIdObj = map.get("skill-id");
            if (skillIdObj != null) {
                reward.skillId = skillIdObj.toString();
            }
        }

        // 读取功法池（可选）
        if (map.containsKey("skill-pool")) {
            Object poolObj = map.get("skill-pool");
            if (poolObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> poolList = (List<String>) poolObj;
                reward.skillPool = new ArrayList<>(poolList);
            } else if (poolObj != null) {
                // 如果是单个字符串，也添加到池中
                reward.skillPool = new ArrayList<>();
                reward.skillPool.add(poolObj.toString());
            }
        }

        // skill-id 和 skill-pool 必须至少有一个
        if ((reward.skillId == null || reward.skillId.isEmpty()) && 
            (reward.skillPool == null || reward.skillPool.isEmpty())) {
            throw new IllegalArgumentException("功法书奖励必须指定 skill-id 或 skill-pool");
        }

        // 读取功法等级（可选，默认1）
        if (map.containsKey("level")) {
            Object levelObj = map.get("level");
            if (levelObj instanceof Number) {
                reward.skillLevel = ((Number) levelObj).intValue();
            } else if (levelObj instanceof String) {
                try {
                    reward.skillLevel = Integer.parseInt((String) levelObj);
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[奇遇系统] 功法等级格式错误: " + levelObj + "，使用默认值1");
                }
            }
        }

        // 验证等级范围
        if (reward.skillLevel <= 0) {
            plugin.getLogger().warning("[奇遇系统] 功法等级无效: " + reward.skillLevel + "，使用默认值1");
            reward.skillLevel = 1;
        } else if (reward.skillLevel > 10) {
            plugin.getLogger().warning("[奇遇系统] 功法等级过高: " + reward.skillLevel + "，已修正为10");
            reward.skillLevel = 10;
        }

        // 读取品质（可选，用于从品质池选择功法）
        if (map.containsKey("quality")) {
            String quality = (String) map.get("quality");
            // TODO: 未来可以根据品质从对应的功法池选择
            // 例如: rare -> 稀有功法池, legendary -> 传说功法池
            if (plugin.isDebugMode()) {
                plugin.getLogger().fine("[奇遇系统] 功法品质配置: " + quality + " (暂未实现品质过滤)");
            }
        }

        return reward;
    }

    /**
     * 获取功法ID（用于调试和验证）
     */
    public String getSkillId() {
        return skillId;
    }

    /**
     * 获取功法池（用于调试和验证）
     */
    public List<String> getSkillPool() {
        return skillPool;
    }

    /**
     * 获取功法等级（用于调试和验证）
     */
    public int getSkillLevel() {
        return skillLevel;
    }

    /**
     * 设置功法ID
     */
    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    /**
     * 设置功法池
     */
    public void setSkillPool(List<String> skillPool) {
        this.skillPool = skillPool;
    }

    /**
     * 设置功法等级
     */
    public void setSkillLevel(int skillLevel) {
        if (skillLevel > 0 && skillLevel <= 10) {
            this.skillLevel = skillLevel;
        }
    }
}


