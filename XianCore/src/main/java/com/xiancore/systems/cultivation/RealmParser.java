package com.xiancore.systems.cultivation;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 境界解析器
 * 从怪物名称中提取境界信息，支持多种格式
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class RealmParser {

    // 境界关键词映射表
    private static final Map<String, String> REALM_KEYWORDS = new HashMap<>();
    
    // 原版怪物默认境界映射
    private static final Map<EntityType, String> DEFAULT_REALM_MAPPING = new HashMap<>();
    
    static {
        // 初始化境界关键词映射
        REALM_KEYWORDS.put("炼气期", "炼气期");
        REALM_KEYWORDS.put("炼气", "炼气期");
        REALM_KEYWORDS.put("筑基期", "筑基期");
        REALM_KEYWORDS.put("筑基", "筑基期");
        REALM_KEYWORDS.put("结丹期", "结丹期");
        REALM_KEYWORDS.put("结丹", "结丹期");
        REALM_KEYWORDS.put("元婴期", "元婴期");
        REALM_KEYWORDS.put("元婴", "元婴期");
        REALM_KEYWORDS.put("化神期", "化神期");
        REALM_KEYWORDS.put("化神", "化神期");
        REALM_KEYWORDS.put("炼虚期", "炼虚期");
        REALM_KEYWORDS.put("炼虚", "炼虚期");
        REALM_KEYWORDS.put("合体期", "合体期");
        REALM_KEYWORDS.put("合体", "合体期");
        REALM_KEYWORDS.put("大乘期", "大乘期");
        REALM_KEYWORDS.put("大乘", "大乘期");
        
        // 初始化原版怪物默认境界映射
        // 炼气期 - 基础怪物
        DEFAULT_REALM_MAPPING.put(EntityType.ZOMBIE, "炼气期");
        DEFAULT_REALM_MAPPING.put(EntityType.SKELETON, "炼气期");
        DEFAULT_REALM_MAPPING.put(EntityType.SPIDER, "炼气期");
        DEFAULT_REALM_MAPPING.put(EntityType.CREEPER, "炼气期");
        DEFAULT_REALM_MAPPING.put(EntityType.SLIME, "炼气期");
        DEFAULT_REALM_MAPPING.put(EntityType.SILVERFISH, "炼气期");
        
        // 筑基期 - 中级怪物  
        DEFAULT_REALM_MAPPING.put(EntityType.ENDERMAN, "筑基期");
        DEFAULT_REALM_MAPPING.put(EntityType.BLAZE, "筑基期");
        DEFAULT_REALM_MAPPING.put(EntityType.WITCH, "筑基期");
        DEFAULT_REALM_MAPPING.put(EntityType.PHANTOM, "筑基期");
        DEFAULT_REALM_MAPPING.put(EntityType.GUARDIAN, "筑基期");
        
        // 结丹期 - 高级怪物
        DEFAULT_REALM_MAPPING.put(EntityType.WITHER_SKELETON, "结丹期");
        DEFAULT_REALM_MAPPING.put(EntityType.PIGLIN_BRUTE, "结丹期");
        DEFAULT_REALM_MAPPING.put(EntityType.RAVAGER, "结丹期");
        DEFAULT_REALM_MAPPING.put(EntityType.EVOKER, "结丹期");
        
        // 元婴期 - Boss级怪物
        DEFAULT_REALM_MAPPING.put(EntityType.ELDER_GUARDIAN, "元婴期");
        DEFAULT_REALM_MAPPING.put(EntityType.SHULKER, "元婴期");
        
        // 化神期 - 终极Boss
        DEFAULT_REALM_MAPPING.put(EntityType.WITHER, "化神期");
        
        // 炼虚期 - 最强Boss
        DEFAULT_REALM_MAPPING.put(EntityType.ENDER_DRAGON, "炼虚期");
    }

    /**
     * 解析怪物名称中的境界信息
     * 支持格式: "炼气期·苦力怕", "筑基修士", "结丹期Boss", "§e筑基期§r·骷髅将军" 等
     *
     * @param mobName 怪物名称（可能包含颜色代码）
     * @return 境界名称，如果未找到则返回null
     */
    public static String parseRealmFromName(String mobName) {
        if (mobName == null || mobName.isEmpty()) {
            return null;
        }
        
        // 移除所有颜色代码
        String cleanName = ChatColor.stripColor(mobName);
        
        // 匹配境界关键词（优先匹配完整境界名）
        // 先匹配完整境界名（如"炼气期"），再匹配简化名（如"炼气"）
        for (Map.Entry<String, String> entry : REALM_KEYWORDS.entrySet()) {
            String keyword = entry.getKey();
            String realm = entry.getValue();
            
            if (cleanName.contains(keyword)) {
                // 优先匹配更长的关键词（避免"炼气"匹配到"炼气期"）
                return realm;
            }
        }
        
        return null; // 未找到境界信息
    }

    /**
     * 获取原版怪物的默认境界
     *
     * @param entityType 实体类型
     * @return 默认境界，如果未配置则返回"炼气期"
     */
    public static String getDefaultRealmForEntityType(EntityType entityType) {
        return DEFAULT_REALM_MAPPING.getOrDefault(entityType, "炼气期");
    }

    /**
     * 解析实体的境界（综合多种来源）
     * 优先级: 自定义名称 > 默认映射
     *
     * @param entity 生物实体
     * @return 境界名称，永远不会返回null
     */
    public static String parseEntityRealm(LivingEntity entity) {
        // 优先级1: 检查自定义名称
        String customName = entity.getCustomName();
        if (customName != null && !customName.isEmpty()) {
            String realmFromName = parseRealmFromName(customName);
            if (realmFromName != null) {
                return realmFromName;
            }
        }
        
        // 优先级2: 使用默认境界映射
        return getDefaultRealmForEntityType(entity.getType());
    }

    /**
     * 检查名称中是否包含Boss标识
     *
     * @param mobName 怪物名称
     * @return 是否为Boss
     */
    public static boolean isBossFromName(String mobName) {
        if (mobName == null || mobName.isEmpty()) {
            return false;
        }
        
        String cleanName = ChatColor.stripColor(mobName).toLowerCase();
        return cleanName.contains("boss") || 
               cleanName.contains("王") || 
               cleanName.contains("龙") ||
               cleanName.contains("帝") ||
               cleanName.contains("尊") ||
               cleanName.contains("圣");
    }

    /**
     * 检查名称中是否包含精英标识
     *
     * @param mobName 怪物名称
     * @return 是否为精英
     */
    public static boolean isEliteFromName(String mobName) {
        if (mobName == null || mobName.isEmpty()) {
            return false;
        }
        
        String cleanName = ChatColor.stripColor(mobName).toLowerCase();
        return cleanName.contains("精英") || 
               cleanName.contains("将军") || 
               cleanName.contains("队长") ||
               cleanName.contains("长老") ||
               cleanName.contains("护法");
    }

    /**
     * 获取所有支持的境界列表
     *
     * @return 境界数组
     */
    public static String[] getAllRealms() {
        return new String[]{
            "炼气期", "筑基期", "结丹期", "元婴期", 
            "化神期", "炼虚期", "合体期", "大乘期"
        };
    }

    /**
     * 获取境界在数组中的索引（用于比较境界高低）
     *
     * @param realm 境界名称
     * @return 索引，未找到返回-1
     */
    public static int getRealmIndex(String realm) {
        String[] allRealms = getAllRealms();
        for (int i = 0; i < allRealms.length; i++) {
            if (allRealms[i].equals(realm)) {
                return i;
            }
        }
        return -1; // 未找到
    }

    /**
     * 计算境界差距（目标境界相对于玩家境界的差距）
     *
     * @param playerRealm 玩家境界
     * @param mobRealm    怪物境界
     * @return 差距值（正数表示怪物境界更高，负数表示玩家境界更高，0表示相同）
     */
    public static int calculateRealmGap(String playerRealm, String mobRealm) {
        int playerIndex = getRealmIndex(playerRealm);
        int mobIndex = getRealmIndex(mobRealm);
        
        if (playerIndex == -1 || mobIndex == -1) {
            return 0; // 无法比较时认为相同
        }
        
        return mobIndex - playerIndex;
    }
}










