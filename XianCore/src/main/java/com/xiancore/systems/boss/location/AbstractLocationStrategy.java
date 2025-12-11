package com.xiancore.systems.boss.location;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 位置选择策略抽象基类
 * 提供通用的辅助方法
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public abstract class AbstractLocationStrategy implements LocationSelectionStrategy {

    protected final XianCore plugin;
    protected final Logger logger;

    public AbstractLocationStrategy(XianCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    /**
     * 寻找安全的地面位置
     * 从给定位置向下搜索固体方块，并确保上方有足够空间
     *
     * @param center 中心位置
     * @return 安全位置，如果未找到则返回null
     */
    protected Location findSafeGroundLocation(Location center) {
        World world = center.getWorld();
        if (world == null) return null;

        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        int startY = Math.min(center.getBlockY(), world.getMaxHeight() - 10);

        // 从起始Y坐标向下搜索
        for (int y = startY; y > world.getMinHeight(); y--) {
            Block block = world.getBlockAt(centerX, y, centerZ);

            // 检查当前方块是否为固体（地面）
            if (block.getType().isSolid()) {
                // 检查上方是否有足够空间（至少3格空气）
                Block above1 = world.getBlockAt(centerX, y + 1, centerZ);
                Block above2 = world.getBlockAt(centerX, y + 2, centerZ);
                Block above3 = world.getBlockAt(centerX, y + 3, centerZ);

                if (above1.getType().isAir() && above2.getType().isAir() && above3.getType().isAir()) {
                    // 找到安全位置：固体地面+上方3格空气
                    return new Location(world, centerX + 0.5, y + 1, centerZ + 0.5);
                }
            }
        }

        // 未找到安全位置
        return null;
    }

    /**
     * 加载并锁定区块
     *
     * @param location 位置
     */
    protected void loadAndLockChunk(Location location) {
        if (location != null && location.getWorld() != null) {
            location.getChunk().load();
            location.getChunk().setForceLoaded(true);
        }
    }

    /**
     * 过滤并优化候选位置
     * 移除无效位置，确保区块已加载
     *
     * @param candidates 候选位置列表
     * @param point      刷新点配置
     * @return 有效的候选位置列表
     */
    protected List<Location> filterCandidates(List<Location> candidates, BossSpawnPoint point) {
        List<Location> valid = new ArrayList<>();

        for (Location loc : candidates) {
            if (loc == null || loc.getWorld() == null) continue;

            Location finalLoc = loc;

            // 如果需要自动寻找地面
            if (point.isAutoFindGround()) {
                finalLoc = findSafeGroundLocation(loc);
                if (finalLoc == null) continue;
            }

            // 确保区块已加载
            finalLoc.getChunk().load();
            valid.add(finalLoc);
        }

        return valid;
    }
}
