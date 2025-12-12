package com.xiancore.systems.forge;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import com.xiancore.systems.forge.items.EmbryoParser;
import com.xiancore.systems.forge.items.EquipmentParser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 炼器显示服务
 * 负责炼器GUI相关的数据统计和检测逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ForgeDisplayService {

    private final XianCore plugin;

    public ForgeDisplayService(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取玩家灵石数量
     */
    public long getPlayerSpiritStones(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        return data != null ? data.getSpiritStones() : 0;
    }

    /**
     * 统计背包中的胚胎数量
     */
    public int countEmbryos(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && EmbryoParser.isEmbryo(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 统计背包中的装备数量
     */
    public int countEquipments(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isEquipment(item)) {
                count += item.getAmount();
            }
        }
        return count;
    }

    /**
     * 检查是否是装备
     */
    public boolean isEquipment(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        // 检查是否是胚胎（胚胎不算装备）
        if (EmbryoParser.isEmbryo(item)) {
            return false;
        }

        // 使用 EquipmentParser 检查
        if (EquipmentParser.isEquipment(item)) {
            return true;
        }

        // 备用方案：检查Lore
        var meta = item.getItemMeta();
        if (meta != null && meta.hasLore()) {
            var lore = meta.getLore();
            if (lore != null) {
                for (String line : lore) {
                    if (line.contains("装备类型:") || line.contains("强化等级:") || line.contains("五行属性:")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查玩家是否可以融合（主副手都有胚胎）
     */
    public boolean canFuse(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return EmbryoParser.isEmbryo(mainHand) && EmbryoParser.isEmbryo(offHand);
    }

    /**
     * 检查主手是否有胚胎
     */
    public boolean hasMainHandEmbryo(Player player) {
        return EmbryoParser.isEmbryo(player.getInventory().getItemInMainHand());
    }

    /**
     * 检查副手是否有胚胎
     */
    public boolean hasOffHandEmbryo(Player player) {
        return EmbryoParser.isEmbryo(player.getInventory().getItemInOffHand());
    }

    /**
     * 获取炼器状态信息
     */
    public ForgeStatusInfo getForgeStatus(Player player) {
        long spiritStones = getPlayerSpiritStones(player);
        int embryoCount = countEmbryos(player);
        int equipmentCount = countEquipments(player);
        boolean canFuse = canFuse(player);

        return new ForgeStatusInfo(spiritStones, embryoCount, equipmentCount, canFuse);
    }

    /**
     * 炼器状态信息
     */
    public static class ForgeStatusInfo {
        private final long spiritStones;
        private final int embryoCount;
        private final int equipmentCount;
        private final boolean canFuse;

        public ForgeStatusInfo(long spiritStones, int embryoCount, int equipmentCount, boolean canFuse) {
            this.spiritStones = spiritStones;
            this.embryoCount = embryoCount;
            this.equipmentCount = equipmentCount;
            this.canFuse = canFuse;
        }

        public long getSpiritStones() { return spiritStones; }
        public int getEmbryoCount() { return embryoCount; }
        public int getEquipmentCount() { return equipmentCount; }
        public boolean canFuse() { return canFuse; }
        public boolean hasEmbryos() { return embryoCount > 0; }
        public boolean hasEquipments() { return equipmentCount > 0; }
    }
}
