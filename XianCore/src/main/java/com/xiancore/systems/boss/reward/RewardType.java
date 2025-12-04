package com.xiancore.systems.boss.reward;

/**
 * 奖励类型枚举
 * 
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-30
 */
public enum RewardType {
    
    /**
     * 经验奖励
     */
    EXPERIENCE("experience", "经验"),
    
    /**
     * 金钱奖励 (需要Vault)
     */
    MONEY("money", "金钱"),
    
    /**
     * 物品奖励
     */
    ITEM("item", "物品"),
    
    /**
     * 执行命令
     */
    COMMAND("command", "命令"),
    
    /**
     * MythicMobs物品
     */
    MYTHIC_ITEM("mythic_item", "神话物品");
    
    private final String configKey;
    private final String displayName;
    
    RewardType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }
    
    public String getConfigKey() {
        return configKey;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 根据配置键获取奖励类型
     */
    public static RewardType fromConfigKey(String key) {
        for (RewardType type : values()) {
            if (type.configKey.equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }
}
