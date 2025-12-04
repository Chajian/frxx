package com.xiancore.systems.sect.util;

import com.xiancore.systems.sect.Sect;
import com.xiancore.systems.sect.SectMember;
import com.xiancore.systems.sect.SectRank;

import java.util.UUID;

/**
 * 晋升诊断工具类 - 提供晋升相关的诊断和分析功能
 *
 * 用途：
 * - 诊断晋升失败的原因
 * - 提供详细的诊断信息和建议
 * - 支持管理员调试和数据检查
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
public class PromotionDiagnostic {

    /**
     * 晋升失败原因枚举
     */
    public enum FailureReason {
        MEMBER_NOT_FOUND("成员不存在", "目标成员在宗门的成员列表中不存在"),
        ALREADY_LEADER("已是最高职位", "目标成员已经是宗主（最高职位），无法再晋升"),
        INVALID_RANK("职位无效", "成员的职位值不在系统定义的职位列表中"),
        INSUFFICIENT_EXECUTOR_RANK("权限不足", "执行者的职位权限不足以晋升目标成员"),
        TARGET_RANK_TOO_HIGH("职位太高", "目标成员的职位已经很高，执行者权限不足以晋升"),
        EXECUTOR_NOT_IN_SECT("未加入宗门", "执行者还没有加入宗门"),
        EXECUTOR_DATA_MISSING("数据缺失", "执行者的数据不完整或损坏");

        private final String shortMessage;
        private final String detailedMessage;

        FailureReason(String shortMessage, String detailedMessage) {
            this.shortMessage = shortMessage;
            this.detailedMessage = detailedMessage;
        }

        public String getShortMessage() {
            return shortMessage;
        }

        public String getDetailedMessage() {
            return detailedMessage;
        }
    }

    /**
     * 晋升诊断结果类
     */
    public static class PromotionDiagnosisResult {
        private final boolean canPromote;                  // 是否可以晋升
        private final FailureReason failureReason;         // 失败原因（如果不能晋升）
        private final String additionalInfo;               // 额外信息
        private final long diagnosticTime;                 // 诊断时间

        public PromotionDiagnosisResult(boolean canPromote, FailureReason failureReason,
                                       String additionalInfo) {
            this.canPromote = canPromote;
            this.failureReason = failureReason;
            this.additionalInfo = additionalInfo;
            this.diagnosticTime = System.currentTimeMillis();
        }

        public boolean canPromote() {
            return canPromote;
        }

        public FailureReason getFailureReason() {
            return failureReason;
        }

        public String getAdditionalInfo() {
            return additionalInfo;
        }

        public long getDiagnosticTime() {
            return diagnosticTime;
        }

        @Override
        public String toString() {
            if (canPromote) {
                return "PromotionDiagnosisResult{canPromote=true}";
            } else {
                return "PromotionDiagnosisResult{" +
                        "canPromote=false, " +
                        "failureReason=" + failureReason.name() + ", " +
                        "message='" + failureReason.getShortMessage() + "', " +
                        "additionalInfo='" + additionalInfo + "'}";
            }
        }
    }

    /**
     * 诊断指定玩家是否可以被晋升
     *
     * @param sect 宗门对象
     * @param targetId 目标玩家UUID
     * @return 诊断结果
     */
    public static PromotionDiagnosisResult diagnosePromotion(Sect sect, UUID targetId) {
        // 检查成员是否存在
        SectMember member = sect.getMember(targetId);
        if (member == null) {
            return new PromotionDiagnosisResult(false,
                    FailureReason.MEMBER_NOT_FOUND,
                    "玩家ID: " + targetId);
        }

        // 检查是否已是最高职位（宗主）
        if (member.getRank() == SectRank.LEADER) {
            return new PromotionDiagnosisResult(false,
                    FailureReason.ALREADY_LEADER,
                    "当前职位: " + member.getRank().getDisplayName());
        }

        // 检查职位是否有效
        SectRank currentRank = member.getRank();
        SectRank[] ranks = SectRank.values();
        boolean rankFound = false;
        for (SectRank rank : ranks) {
            if (rank == currentRank) {
                rankFound = true;
                break;
            }
        }

        if (!rankFound) {
            return new PromotionDiagnosisResult(false,
                    FailureReason.INVALID_RANK,
                    "职位: " + currentRank + ", 系统职位总数: " + ranks.length);
        }

        // 可以晋升
        return new PromotionDiagnosisResult(true,
                null,
                "玩家: " + member.getPlayerName() + ", 当前职位: " + currentRank.getDisplayName());
    }

    /**
     * 诊断执行者是否有权限晋升目标成员
     *
     * @param executorRank 执行者职位
     * @param targetRank 目标职位
     * @return 诊断结果
     */
    public static PromotionDiagnosisResult diagnosePermission(SectRank executorRank,
                                                              SectRank targetRank) {
        // 执行者必须有管理权限
        if (!executorRank.hasManagePermission()) {
            return new PromotionDiagnosisResult(false,
                    FailureReason.INSUFFICIENT_EXECUTOR_RANK,
                    "执行者职位: " + executorRank.getDisplayName() +
                            " (无管理权限)");
        }

        // 执行者职位必须高于目标职位
        if (executorRank.getLevel() <= targetRank.getLevel()) {
            return new PromotionDiagnosisResult(false,
                    FailureReason.TARGET_RANK_TOO_HIGH,
                    "执行者职位: " + executorRank.getDisplayName() +
                            " (等级 " + executorRank.getLevel() + ")" +
                            ", 目标职位: " + targetRank.getDisplayName() +
                            " (等级 " + targetRank.getLevel() + ")");
        }

        // 有权限
        return new PromotionDiagnosisResult(true,
                null,
                "执行者职位: " + executorRank.getDisplayName() +
                        " > 目标职位: " + targetRank.getDisplayName());
    }

    /**
     * 生成诊断报告（用于管理员调试）
     *
     * @param sect 宗门对象
     * @param targetId 目标玩家UUID
     * @param executorRank 执行者职位
     * @return 诊断报告字符串
     */
    public static String generateDiagnosticReport(Sect sect, UUID targetId, SectRank executorRank) {
        StringBuilder report = new StringBuilder();
        report.append("========== 晋升诊断报告 ==========\n");
        report.append("宗门: ").append(sect.getName()).append("\n");
        report.append("宗门ID: ").append(sect.getId()).append("\n");
        report.append("执行者职位: ").append(executorRank.getDisplayName()).append("\n");

        // 诊断目标成员
        PromotionDiagnosisResult memberDiagnosis = diagnosePromotion(sect, targetId);
        report.append("\n[目标成员诊断]\n");
        report.append("可晋升: ").append(memberDiagnosis.canPromote() ? "是" : "否").append("\n");
        if (!memberDiagnosis.canPromote()) {
            report.append("失败原因: ").append(memberDiagnosis.getFailureReason().getShortMessage()).append("\n");
            report.append("详细信息: ").append(memberDiagnosis.getAdditionalInfo()).append("\n");
        } else {
            report.append("详细信息: ").append(memberDiagnosis.getAdditionalInfo()).append("\n");
        }

        // 诊断权限
        if (memberDiagnosis.canPromote()) {
            SectMember member = sect.getMember(targetId);
            if (member != null) {
                PromotionDiagnosisResult permissionDiagnosis = diagnosePermission(
                        executorRank, member.getRank());
                report.append("\n[权限诊断]\n");
                report.append("有权限: ").append(permissionDiagnosis.canPromote() ? "是" : "否").append("\n");
                if (!permissionDiagnosis.canPromote()) {
                    report.append("失败原因: ").append(permissionDiagnosis.getFailureReason().getShortMessage()).append("\n");
                    report.append("详细信息: ").append(permissionDiagnosis.getAdditionalInfo()).append("\n");
                } else {
                    report.append("详细信息: ").append(permissionDiagnosis.getAdditionalInfo()).append("\n");
                }
            }
        }

        report.append("\n========== 诊断完成 ==========");
        return report.toString();
    }
}
