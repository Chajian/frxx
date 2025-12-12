package com.xiancore.systems.forge;

import com.xiancore.systems.forge.items.Embryo;
import com.xiancore.systems.forge.items.EmbryoParser;
import com.xiancore.systems.forge.items.Equipment;
import com.xiancore.systems.forge.items.EquipmentParser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品选择服务
 * 负责胚胎和装备选择GUI的背包扫描逻辑
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class ItemSelectionService {

    // 最大强化等级
    public static final int MAX_ENHANCE_LEVEL = 20;

    /**
     * 扫描玩家背包中的胚胎
     */
    public List<EmbryoSlotInfo> scanEmbryos(Player player) {
        List<EmbryoSlotInfo> embryos = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir() && EmbryoParser.isEmbryo(item)) {
                Embryo embryo = EmbryoParser.parseFromItemStack(item);
                if (embryo != null) {
                    embryos.add(new EmbryoSlotInfo(embryo, i, item));
                }
            }
        }

        return embryos;
    }

    /**
     * 扫描玩家背包中的装备
     */
    public List<EquipmentSlotInfo> scanEquipments(Player player) {
        List<EquipmentSlotInfo> equipments = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && !item.getType().isAir() && EquipmentParser.isEquipment(item)) {
                Equipment equipment = EquipmentParser.parseFromItemStack(item);
                if (equipment != null) {
                    int enhanceLevel = EquipmentParser.getEnhanceLevel(item);
                    equipments.add(new EquipmentSlotInfo(equipment, i, item, enhanceLevel));
                }
            }
        }

        return equipments;
    }

    /**
     * 获取槽位位置描述
     */
    public String getSlotDescription(int slot) {
        if (slot < 9) {
            return "快捷栏 #" + (slot + 1);
        } else {
            return "背包 #" + ((slot - 9) % 9 + 1);
        }
    }

    /**
     * 检查装备是否可以强化
     */
    public boolean canEnhance(int enhanceLevel) {
        return enhanceLevel < MAX_ENHANCE_LEVEL;
    }

    /**
     * 胚胎槽位信息
     */
    public static class EmbryoSlotInfo {
        private final Embryo embryo;
        private final int slot;
        private final ItemStack itemStack;

        public EmbryoSlotInfo(Embryo embryo, int slot, ItemStack itemStack) {
            this.embryo = embryo;
            this.slot = slot;
            this.itemStack = itemStack;
        }

        public Embryo getEmbryo() { return embryo; }
        public int getSlot() { return slot; }
        public ItemStack getItemStack() { return itemStack; }
    }

    /**
     * 装备槽位信息
     */
    public static class EquipmentSlotInfo {
        private final Equipment equipment;
        private final int slot;
        private final ItemStack itemStack;
        private final int enhanceLevel;

        public EquipmentSlotInfo(Equipment equipment, int slot, ItemStack itemStack, int enhanceLevel) {
            this.equipment = equipment;
            this.slot = slot;
            this.itemStack = itemStack;
            this.enhanceLevel = enhanceLevel;
        }

        public Equipment getEquipment() { return equipment; }
        public int getSlot() { return slot; }
        public ItemStack getItemStack() { return itemStack; }
        public int getEnhanceLevel() { return enhanceLevel; }
        public boolean isMaxEnhanced() { return enhanceLevel >= MAX_ENHANCE_LEVEL; }
    }
}
