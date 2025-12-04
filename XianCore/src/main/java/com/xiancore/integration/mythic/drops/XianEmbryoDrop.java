package com.xiancore.integration.mythic.drops;

import com.xiancore.XianCore;
import com.xiancore.systems.forge.items.Embryo;
import com.xiancore.systems.forge.items.EmbryoFactory;
import io.lumine.mythic.api.adapters.AbstractItemStack;
import io.lumine.mythic.api.config.MythicLineConfig;
import io.lumine.mythic.api.drops.DropMetadata;
import io.lumine.mythic.api.drops.IItemDrop;
import io.lumine.mythic.bukkit.adapters.BukkitItemStack;
import org.bukkit.inventory.ItemStack;

/**
 * 仙家胚胎掉落处理器
 * 用于 MythicMobs 怪物掉落仙家胚胎
 *
 * 注意: MythicMobs 5.6.1 API 实现
 * 继承自 IItemDrop 接口（需根据MythicMobs版本调整）
 * 当前实现支持概率控制和品质随机生成
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class XianEmbryoDrop implements IItemDrop {

    private final String quality;
    private final double chance;

    /**
     * 构造函数
     *
     * @param config MythicMobs 配置行
     */
    public XianEmbryoDrop(MythicLineConfig config) {
        // 从配置中读取品质和概率
        this.quality = config.getString("quality", "random");
        this.chance = config.getDouble("chance", 0.0001);
    }

    public String getItemName() {
        return "仙家胚胎";
    }

    public int getAmount() {
        return 1;
    }

    public AbstractItemStack getDrop(DropMetadata dropMetadata, double v) {
        // 检查概率
        if (Math.random() > chance) {
            return null;
        }

        try {
            // 生成仙家胚胎
            Embryo embryo = EmbryoFactory.randomGenerate(quality);
            ItemStack itemStack = embryo.toItemStack();

            return new BukkitItemStack(itemStack);

        } catch (Exception e) {
            XianCore.getInstance().getLogger().severe("生成仙家胚胎掉落时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
