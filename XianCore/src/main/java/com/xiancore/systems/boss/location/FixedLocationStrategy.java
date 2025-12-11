package com.xiancore.systems.boss.location;

import com.xiancore.XianCore;
import com.xiancore.systems.boss.entity.BossSpawnPoint;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定位置策略
 * 在配置的固定位置生成，支持随机偏移
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class FixedLocationStrategy extends AbstractLocationStrategy {

    public static final String NAME = "fixed";

    public FixedLocationStrategy(XianCore plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<Location> generateCandidates(BossSpawnPoint point, int maxCandidates) {
        List<Location> candidates = new ArrayList<>();

        Location baseLocation = point.getLocation();
        if (baseLocation == null) {
            logger.warning("无法获取刷新点位置: " + point.getId() + " (世界不存在？)");
            return candidates;
        }

        // 强制加载区块
        baseLocation.getChunk().load();

        int randomRadius = point.getRandomRadius();

        if (randomRadius <= 0) {
            // 无随机偏移，直接使用固定位置
            Location finalLoc = baseLocation.clone();

            if (point.isAutoFindGround()) {
                Location safeLoc = findSafeGroundLocation(finalLoc);
                if (safeLoc != null) {
                    candidates.add(safeLoc);
                } else {
                    candidates.add(finalLoc);
                }
            } else {
                candidates.add(finalLoc);
            }
        } else {
            // 有随机偏移，生成多个候选位置
            for (int i = 0; i < maxCandidates; i++) {
                int offsetX = (int) (Math.random() * randomRadius * 2) - randomRadius;
                int offsetZ = (int) (Math.random() * randomRadius * 2) - randomRadius;

                Location candidateLoc = baseLocation.clone().add(offsetX, 0, offsetZ);
                candidateLoc.getChunk().load();

                if (point.isAutoFindGround()) {
                    Location safeLoc = findSafeGroundLocation(candidateLoc);
                    if (safeLoc != null) {
                        candidates.add(safeLoc);
                    }
                } else {
                    candidates.add(candidateLoc);
                }
            }
        }

        logger.info("[固定位置策略] 生成了 " + candidates.size() + " 个候选位置");
        return candidates;
    }

    @Override
    public boolean isApplicable(BossSpawnPoint point) {
        // 固定位置是默认策略，当 spawnMode 为 null、空或 "fixed" 时使用
        String mode = point.getSpawnMode();
        return mode == null || mode.isEmpty() || NAME.equalsIgnoreCase(mode);
    }
}
