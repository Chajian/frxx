package com.xiancore.systems.sect.task;

import com.xiancore.systems.sect.Sect;
import lombok.Getter;

/**
 * 领地相关任务类型
 * 定义与宗门领地系统相关的各类任务
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class LandTaskType {

    public enum TaskType {
        // 领地任务
        LAND_CLAIM("领地圈地", "圈地一块宗门领地", 100, 500),
        LAND_EXPAND("领地扩展", "扩展宗门领地", 50, 300),
        LAND_SHRINK("领地缩小", "缩小宗门领地", 30, 200),
        LAND_PAY_MAINTENANCE("维护费缴纳", "缴纳宗门领地维护费", 20, 150),
        LAND_BUILD_SLOT("建筑位建设", "在领地中建设建筑位", 60, 400),

        // 权限任务
        PERMISSION_SETUP("权限设置", "为成员设置领地权限", 40, 250),
        PERMISSION_MANAGE("权限管理", "管理宗门成员权限", 50, 350),

        // 维护任务
        MAINTENANCE_KEEP("维护守护", "保持30天不逾期维护费", 80, 600),
        MAINTENANCE_RECOVER("维护恢复", "从逾期状态恢复", 100, 800);

        private final String displayName;
        private final String description;
        private final int targetCount;
        private final int rewardPoints;

        TaskType(String displayName, String description, int targetCount, int rewardPoints) {
            this.displayName = displayName;
            this.description = description;
            this.targetCount = targetCount;
            this.rewardPoints = rewardPoints;
        }
    }

    /**
     * 检查宗门是否完成了特定任务
     */
    public static boolean isTaskCompleted(Sect sect, TaskType taskType) {
        return switch (taskType) {
            case LAND_CLAIM -> sect.hasLand();
            case LAND_EXPAND -> sect.getLandCenter() != null;
            case LAND_PAY_MAINTENANCE -> sect.getLastMaintenanceTime() > 0;
            case LAND_BUILD_SLOT -> !sect.getBuildingSlots().isEmpty();
            case PERMISSION_SETUP -> sect.getMemberList().size() > 0;
            default -> false;
        };
    }

    /**
     * 获取任务进度
     */
    public static int getProgress(Sect sect, TaskType taskType) {
        return switch (taskType) {
            case LAND_CLAIM -> sect.hasLand() ? 1 : 0;
            case LAND_EXPAND -> sect.getResidenceLandId() != null ? 1 : 0;
            case LAND_BUILD_SLOT -> sect.getBuildingSlots().size();
            case PERMISSION_SETUP -> sect.getMemberList().size();
            default -> 0;
        };
    }
}
