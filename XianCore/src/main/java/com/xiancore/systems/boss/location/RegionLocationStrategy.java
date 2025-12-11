package com.xiancore.systems.boss.location;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 区域随机位置策略
 * 从配置的区域列表中随机选择一个区域，然后在区域内随机生成位置
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class RegionLocationStrategy extends AbstractLocationStrategy {

    public static final String NAME = "region";

    private final Random random = new Random();

    public RegionLocationStrategy(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<Location> generateCandidates(BossSpawnPoint point, int maxCandidates) {
        List<Location> candidates = new ArrayList<>();

        List<String> regions = point.getRegions();
        if (regions == null || regions.isEmpty()) {
            logger.warning("[区域策略] 刷新点 " + point.getId() + " 未配置 regions");
            return candidates;
        }

        logger.info("========================================");
        logger.info("▶ [区域策略] 开始位置选择");
        logger.info("  刷新点: " + point.getId());
        logger.info("  可选区域: " + regions.size() + " 个");

        // 随机选择一个区域
        String selectedRegion = regions.get(random.nextInt(regions.size()));
        logger.info("  选中区域: " + selectedRegion);

        // 解析区域定义：格式 "world,x1,z1,x2,z2"
        RegionBounds bounds = parseRegion(selectedRegion);
        if (bounds == null) {
            return candidates;
        }

        logger.info("  区域范围: X(" + bounds.minX + " - " + bounds.maxX +
                   "), Z(" + bounds.minZ + " - " + bounds.maxZ + ")");

        // 在区域内生成候选位置
        for (int i = 0; i < maxCandidates; i++) {
            int randomX = bounds.minX + random.nextInt(bounds.maxX - bounds.minX + 1);
            int randomZ = bounds.minZ + random.nextInt(bounds.maxZ - bounds.minZ + 1);

            // 从较高处开始搜索
            Location candidateLoc = new Location(bounds.world, randomX, 128, randomZ);

            if (point.isAutoFindGround()) {
                Location safeLoc = findSafeGroundLocation(candidateLoc);
                if (safeLoc != null) {
                    candidates.add(safeLoc);
                }
            } else {
                candidates.add(candidateLoc);
            }
        }

        logger.info("[区域策略] 生成了 " + candidates.size() + " 个候选位置");
        return candidates;
    }

    /**
     * 解析区域字符串
     *
     * @param regionStr 区域字符串，格式 "world,x1,z1,x2,z2"
     * @return 区域边界，解析失败返回 null
     */
    private RegionBounds parseRegion(String regionStr) {
        String[] parts = regionStr.split(",");
        if (parts.length != 5) {
            logger.warning("✗ 区域格式错误: " + regionStr + "，应为 'world,x1,z1,x2,z2'");
            return null;
        }

        String worldName = parts[0].trim();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.warning("✗ 世界不存在: " + worldName);
            return null;
        }

        try {
            int x1 = Integer.parseInt(parts[1].trim());
            int z1 = Integer.parseInt(parts[2].trim());
            int x2 = Integer.parseInt(parts[3].trim());
            int z2 = Integer.parseInt(parts[4].trim());

            return new RegionBounds(
                    world,
                    Math.min(x1, x2),
                    Math.max(x1, x2),
                    Math.min(z1, z2),
                    Math.max(z1, z2)
            );
        } catch (NumberFormatException e) {
            logger.warning("✗ 区域坐标解析失败: " + regionStr);
            return null;
        }
    }

    /**
     * 区域边界数据类
     */
    private static class RegionBounds {
        final World world;
        final int minX, maxX, minZ, maxZ;

        RegionBounds(World world, int minX, int maxX, int minZ, int maxZ) {
            this.world = world;
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
        }
    }
}
