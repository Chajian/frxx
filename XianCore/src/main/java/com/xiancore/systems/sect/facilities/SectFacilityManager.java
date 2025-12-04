package com.xiancore.systems.sect.facilities;

import com.xiancore.XianCore;
import com.xiancore.systems.sect.Sect;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 宗门设施管理器
 * 负责管理所有宗门的设施升级、加成计算等
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class SectFacilityManager {

    private final XianCore plugin;

    // 设施数据缓存 (宗门ID -> 设施数据)
    private final Map<Integer, SectFacilityData> facilityDataCache;

    public SectFacilityManager(XianCore plugin) {
        this.plugin = plugin;
        this.facilityDataCache = new ConcurrentHashMap<>();
    }

    /**
     * 初始化管理器
     */
    public void initialize() {
        plugin.getLogger().info("  \u00a7a\u2713 \u5b97\u95e8\u8bbe\u65bd\u7ba1\u7406\u5668\u521d\u59cb\u5316\u5b8c\u6210");
    }

    /**
     * 获取宗门设施数据
     *
     * @param sectId 宗门ID
     * @return 设施数据
     */
    public SectFacilityData getFacilityData(int sectId) {
        return facilityDataCache.computeIfAbsent(sectId, id -> {
            // 尝试从文件加载
            SectFacilityData data = plugin.getDataManager().loadFacilityDataFromFile(id);
            
            if (data != null) {
                plugin.getLogger().fine("§7从文件加载设施数据: " + id);
                return data;
            }
            
            // 文件不存在，创建新数据
            plugin.getLogger().fine("§7创建新设施数据: " + id);
            return new SectFacilityData(id);
        });
    }

    /**
     * 升级设施
     *
     * @param sectId 宗门ID
     * @param facility 设施类型
     * @param player 执行操作的玩家
     * @return 是否成功
     */
    public boolean upgradeFacility(int sectId, SectFacility facility, Player player) {
        Sect sect = plugin.getSectSystem().getSect(sectId);
        if (sect == null) {
            player.sendMessage("\u00a7c\u5b97\u95e8\u4e0d\u5b58\u5728!");
            return false;
        }

        SectFacilityData data = getFacilityData(sectId);

        // 检查是否已满级
        if (data.isMaxLevel(facility)) {
            player.sendMessage("\u00a7c" + facility.getDisplayName() + " \u00a7c\u5df2\u8fbe\u6700\u9ad8\u7b49\u7ea7!");
            return false;
        }

        int currentLevel = data.getLevel(facility);
        int nextLevel = currentLevel + 1;

        // 计算升级消耗
        long upgradeCost = facility.getUpgradeCost(nextLevel);

        // 检查宗门灵石
        if (sect.getSectFunds() < upgradeCost) {
            player.sendMessage("\u00a7c\u5b97\u95e8\u7075\u77f3\u4e0d\u8db3!");
            player.sendMessage("\u00a77\u9700\u8981: \u00a76" + upgradeCost + " \u00a77\u5f53\u524d: \u00a7f" + sect.getSectFunds());
            return false;
        }

        // 扣除灵石
        sect.removeFunds(upgradeCost);

        // 升级设施
        data.upgradeLevel(facility);

        // 保存数据
        plugin.getSectSystem().saveSect(sect);
        saveFacilityData(data);

        // 成功消息
        player.sendMessage("\u00a7a\u00a7l========== \u8bbe\u65bd\u5347\u7ea7 ==========");
        player.sendMessage("\u00a7e\u8bbe\u65bd: " + facility.getDisplayName());
        player.sendMessage("\u00a7e\u7b49\u7ea7: \u00a77" + currentLevel + " \u00a7f\u2192 \u00a7a" + nextLevel);
        player.sendMessage("\u00a7e\u6d88\u8017: \u00a76" + upgradeCost + " \u7075\u77f3");
        player.sendMessage("\u00a7e\u6548\u679c: " + facility.getFormattedBonus(nextLevel));
        player.sendMessage("\u00a7a\u00a7l===========================");

        // 全宗门公告
        String announcement = "\u00a7e\u00a7l[\u5b97\u95e8\u516c\u544a] \u00a7r" + facility.getDisplayName() +
                              " \u00a7f\u5df2\u5347\u7ea7\u5230 \u00a7a" + nextLevel + " \u00a7f\u7ea7!";
        sect.broadcastMessage(announcement);

        return true;
    }

    /**
     * 获取修炼速度加成
     *
     * @param sectId 宗门ID
     * @return 加成百分比（例如：15.0 表示 15%）
     */
    public double getCultivationSpeedBonus(int sectId) {
        SectFacilityData data = getFacilityData(sectId);
        return data.getBonus(SectFacility.SPIRITUAL_VEIN);
    }

    /**
     * 获取炼制成功率加成
     *
     * @param sectId 宗门ID
     * @return 加成百分比（例如：9.0 表示 9%）
     */
    public double getForgeSuccessBonus(int sectId) {
        SectFacilityData data = getFacilityData(sectId);
        return data.getBonus(SectFacility.FORGE_ALTAR);
    }

    /**
     * 获取藏经阁可学习功法数量
     *
     * @param sectId 宗门ID
     * @return 可学习功法数量
     */
    public int getUnlockedSkillCount(int sectId) {
        SectFacilityData data = getFacilityData(sectId);
        return (int) data.getBonus(SectFacility.SCRIPTURE_PAVILION);
    }

    /**
     * 获取仓库容量
     *
     * @param sectId 宗门ID
     * @return 仓库格数
     */
    public int getWarehouseCapacity(int sectId) {
        SectFacilityData data = getFacilityData(sectId);
        return (int) data.getBonus(SectFacility.SECT_WAREHOUSE);
    }

    /**
     * 检查商店是否开启
     *
     * @param sectId 宗门ID
     * @return 是否开启
     */
    public boolean isShopEnabled(int sectId) {
        SectFacilityData data = getFacilityData(sectId);
        return data.isBuilt(SectFacility.SECT_SHOP);
    }

    /**
     * 保存设施数据
     *
     * @param data 设施数据
     */
    public void saveFacilityData(SectFacilityData data) {
        // 更新缓存
        facilityDataCache.put(data.getSectId(), data);
        
        // 保存到文件
        plugin.getDataManager().saveFacilityDataToFile(data);
    }

    /**
     * 移除设施数据（宗门解散时调用）
     *
     * @param sectId 宗门ID
     */
    public void removeFacilityData(int sectId) {
        facilityDataCache.remove(sectId);
        
        // 删除文件
        java.io.File file = new java.io.File(plugin.getDataFolder(), "sects/" + sectId + "_facilities.yml");
        if (file.exists()) {
            if (file.delete()) {
                plugin.getLogger().info("§7删除设施数据文件: " + sectId);
            } else {
                plugin.getLogger().warning("§e删除设施数据文件失败: " + sectId);
            }
        }
    }
    
    /**
     * 保存所有设施数据
     */
    public void saveAll() {
        int count = 0;
        for (SectFacilityData data : facilityDataCache.values()) {
            plugin.getDataManager().saveFacilityDataToFile(data);
            count++;
        }
        
        if (count > 0) {
            plugin.getLogger().info("§7保存了 " + count + " 个宗门设施数据");
        }
    }
}
