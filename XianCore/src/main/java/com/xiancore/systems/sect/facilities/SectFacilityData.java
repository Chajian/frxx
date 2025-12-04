package com.xiancore.systems.sect.facilities;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 宗门设施数据
 * 存储单个宗门的所有设施等级和状态
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Data
public class SectFacilityData {

    private int sectId;

    // 设施等级映射 (设施ID -> 等级)
    private Map<String, Integer> facilityLevels;

    // 最后更新时间
    private long lastUpdated;

    public SectFacilityData(int sectId) {
        this.sectId = sectId;
        this.facilityLevels = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();

        // 初始化所有设施等级为 0
        for (SectFacility facility : SectFacility.values()) {
            facilityLevels.put(facility.getId(), 0);
        }
    }

    /**
     * 获取设施等级
     *
     * @param facility 设施类型
     * @return 等级（0表示未建造）
     */
    public int getLevel(SectFacility facility) {
        return facilityLevels.getOrDefault(facility.getId(), 0);
    }

    /**
     * 设置设施等级
     *
     * @param facility 设施类型
     * @param level 等级
     */
    public void setLevel(SectFacility facility, int level) {
        facilityLevels.put(facility.getId(), level);
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * 升级设施
     *
     * @param facility 设施类型
     * @return 升级后的等级
     */
    public int upgradeLevel(SectFacility facility) {
        int currentLevel = getLevel(facility);
        int newLevel = Math.min(currentLevel + 1, facility.getMaxLevel());
        setLevel(facility, newLevel);
        return newLevel;
    }

    /**
     * 检查设施是否已建造
     *
     * @param facility 设施类型
     * @return 是否已建造
     */
    public boolean isBuilt(SectFacility facility) {
        return getLevel(facility) > 0;
    }

    /**
     * 检查设施是否已达最高等级
     *
     * @param facility 设施类型
     * @return 是否已满级
     */
    public boolean isMaxLevel(SectFacility facility) {
        return getLevel(facility) >= facility.getMaxLevel();
    }

    /**
     * 获取设施加成
     *
     * @param facility 设施类型
     * @return 加成值
     */
    public double getBonus(SectFacility facility) {
        int level = getLevel(facility);
        return facility.getBonus(level);
    }

    // ==================== 显式 Getter/Setter 方法 ====================

    public int getSectId() {
        return sectId;
    }

    public Map<String, Integer> getFacilityLevels() {
        return facilityLevels;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setSectId(int sectId) {
        this.sectId = sectId;
    }

    public void setFacilityLevels(Map<String, Integer> facilityLevels) {
        this.facilityLevels = facilityLevels;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
