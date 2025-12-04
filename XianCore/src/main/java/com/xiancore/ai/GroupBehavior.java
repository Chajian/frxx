package com.xiancore.ai;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * 群体行为 - 多Boss协调系统
 * Group Behavior - Multi-Boss Coordination System
 *
 * @author XianCore
 * @version 1.0
 */
public class GroupBehavior {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Map<String, BossGroup> bossGroups = new ConcurrentHashMap<>();
    private final Map<String, BossFormation> formations = new ConcurrentHashMap<>();

    /**
     * Boss组群
     */
    public static class BossGroup {
        public String groupId;
        public List<String> bossIds;           // Boss ID列表
        public GroupType type;                 // 组织类型
        public List<String> memberOrder;       // 成员顺序 (用于协调)
        public double cohesion;                // 凝聚力 (0-1)
        public String leader;                  // 领导Boss
        public BossFormation currentFormation; // 当前阵型
        public GroupState state;               // 组群状态
        public long createdTime;

        public enum GroupType {
            PAIR,        // 双Boss组合
            TRIO,        // 三Boss组合
            SQUAD,       // 四Boss小队
            PACK         // 多Boss群体
        }

        public enum GroupState {
            FORMING, ACTIVE, FIGHTING, WEAKENED, BROKEN
        }

        public BossGroup(String groupId, GroupType type) {
            this.groupId = groupId;
            this.type = type;
            this.bossIds = new CopyOnWriteArrayList<>();
            this.memberOrder = new CopyOnWriteArrayList<>();
            this.cohesion = 1.0;
            this.state = GroupState.FORMING;
            this.createdTime = System.currentTimeMillis();
        }

        public void addMember(String bossId) {
            if (!bossIds.contains(bossId)) {
                bossIds.add(bossId);
                memberOrder.add(bossId);
            }
        }

        public void removeMember(String bossId) {
            bossIds.remove(bossId);
            if (leader != null && leader.equals(bossId)) {
                leader = bossIds.isEmpty() ? null : bossIds.get(0);
            }
        }

        public boolean isFull() {
            int expectedSize = switch (type) {
                case PAIR -> 2;
                case TRIO -> 3;
                case SQUAD -> 4;
                case PACK -> Integer.MAX_VALUE;
            };
            return bossIds.size() >= expectedSize;
        }
    }

    /**
     * Boss阵型
     */
    public static class BossFormation {
        public String formationId;
        public String name;
        public FormationType type;
        public List<String> positions;  // 位置编号
        public Map<String, String> positionMap;  // bossId -> position映射

        public enum FormationType {
            LINE,           // 直线阵型
            TRIANGLE,       // 三角阵型
            CIRCLE,         // 圆形阵型
            DIAMOND,        // 菱形阵型
            SCATTERED       // 分散阵型
        }

        public BossFormation(String formationId, String name, FormationType type) {
            this.formationId = formationId;
            this.name = name;
            this.type = type;
            this.positions = new ArrayList<>();
            this.positionMap = new HashMap<>();
            initializePositions();
        }

        private void initializePositions() {
            switch (type) {
                case LINE:
                    positions.addAll(List.of("FRONT", "MIDDLE", "BACK", "BACK-LEFT", "BACK-RIGHT"));
                    break;
                case TRIANGLE:
                    positions.addAll(List.of("APEX", "LEFT-BASE", "RIGHT-BASE", "LEFT-WING", "RIGHT-WING"));
                    break;
                case CIRCLE:
                    positions.addAll(List.of("CENTER", "NORTH", "SOUTH", "EAST", "WEST", "NE", "NW", "SE", "SW"));
                    break;
                case DIAMOND:
                    positions.addAll(List.of("TOP", "LEFT", "RIGHT", "BOTTOM", "CENTER"));
                    break;
                case SCATTERED:
                    positions.addAll(List.of("AREA-1", "AREA-2", "AREA-3", "AREA-4", "AREA-5"));
                    break;
            }
        }

        public String assignPosition(String bossId) {
            if (positions.isEmpty()) return null;

            // 分配未使用的位置
            for (String position : positions) {
                if (!positionMap.containsValue(position)) {
                    positionMap.put(bossId, position);
                    return position;
                }
            }

            // 如果所有位置都被占用，分配第一个位置
            String position = positions.get(0);
            positionMap.put(bossId, position);
            return position;
        }
    }

    /**
     * 群体行为命令
     */
    public static class GroupCommand {
        public String commandId;
        public CommandType type;
        public String targetPlayer;
        public List<String> participatingBosses;
        public CommandState state;
        public long issuedTime;

        public enum CommandType {
            COORDINATE_ATTACK,    // 协调攻击
            SURROUND,            // 包围
            RUSH,                // 冲锋
            RETREAT,             // 撤退
            REGROUP              // 重组
        }

        public enum CommandState {
            ISSUED, EXECUTING, COMPLETED, CANCELLED
        }

        public GroupCommand(String commandId, CommandType type, String targetPlayer) {
            this.commandId = commandId;
            this.type = type;
            this.targetPlayer = targetPlayer;
            this.participatingBosses = new CopyOnWriteArrayList<>();
            this.state = CommandState.ISSUED;
            this.issuedTime = System.currentTimeMillis();
        }
    }

    /**
     * 构造函数
     */
    public GroupBehavior() {
        initializeFormations();
        logger.info("✓ GroupBehavior已初始化");
    }

    /**
     * 初始化预设阵型
     */
    private void initializeFormations() {
        formations.put("line", new BossFormation("line", "直线阵型", BossFormation.FormationType.LINE));
        formations.put("triangle", new BossFormation("triangle", "三角阵型", BossFormation.FormationType.TRIANGLE));
        formations.put("circle", new BossFormation("circle", "圆形阵型", BossFormation.FormationType.CIRCLE));
        formations.put("diamond", new BossFormation("diamond", "菱形阵型", BossFormation.FormationType.DIAMOND));
        formations.put("scattered", new BossFormation("scattered", "分散阵型", BossFormation.FormationType.SCATTERED));

        logger.info("✓ 5个预设阵型已加载");
    }

    /**
     * 创建Boss组群
     */
    public BossGroup createGroup(String groupId, BossGroup.GroupType type) {
        BossGroup group = new BossGroup(groupId, type);
        bossGroups.put(groupId, group);
        logger.info("✓ Boss组群已创建: " + groupId + " (" + type.name() + ")");
        return group;
    }

    /**
     * 添加Boss到组群
     */
    public void addBossToGroup(String groupId, String bossId) {
        BossGroup group = bossGroups.get(groupId);
        if (group != null) {
            group.addMember(bossId);

            if (group.leader == null) {
                group.leader = bossId;
            }

            if (group.currentFormation == null) {
                group.currentFormation = formations.get("line");
            }

            logger.info("✓ Boss已加入组群: " + bossId + " -> " + groupId);
        }
    }

    /**
     * 移除Boss从组群
     */
    public void removeBossFromGroup(String groupId, String bossId) {
        BossGroup group = bossGroups.get(groupId);
        if (group != null) {
            group.removeMember(bossId);

            if (group.bossIds.isEmpty()) {
                bossGroups.remove(groupId);
                logger.info("✓ Boss组群已解散: " + groupId);
            }
        }
    }

    /**
     * 设置组群阵型
     */
    public void setGroupFormation(String groupId, String formationId) {
        BossGroup group = bossGroups.get(groupId);
        BossFormation formation = formations.get(formationId);

        if (group != null && formation != null) {
            group.currentFormation = formation;

            // 分配位置
            for (String bossId : group.bossIds) {
                formation.assignPosition(bossId);
            }

            logger.info("✓ 组群阵型已设置: " + groupId + " -> " + formation.name);
        }
    }

    /**
     * 发出群体命令
     */
    public GroupCommand issueGroupCommand(String groupId, GroupCommand.CommandType type, String targetPlayer) {
        BossGroup group = bossGroups.get(groupId);
        if (group == null) return null;

        String commandId = UUID.randomUUID().toString();
        GroupCommand command = new GroupCommand(commandId, type, targetPlayer);
        command.participatingBosses.addAll(group.bossIds);
        command.state = GroupCommand.CommandState.EXECUTING;

        // 更新组群状态
        group.state = BossGroup.GroupState.FIGHTING;

        logger.info("✓ 群体命令已发出: " + type.name() + " (" + group.bossIds.size() + "个Boss参与)");

        return command;
    }

    /**
     * 计算组群凝聚力
     */
    public double calculateCohesion(String groupId, Map<String, Double> bossHealth, Map<String, Double> bossMana) {
        BossGroup group = bossGroups.get(groupId);
        if (group == null) return 0;

        // 凝聚力 = 1 - (标准差 / 平均值)
        if (group.bossIds.isEmpty()) return 1.0;

        double avgHealth = group.bossIds.stream()
                .mapToDouble(id -> bossHealth.getOrDefault(id, 100.0))
                .average()
                .orElse(100.0);

        double variance = group.bossIds.stream()
                .mapToDouble(id -> {
                    double health = bossHealth.getOrDefault(id, 100.0);
                    return Math.pow(health - avgHealth, 2);
                })
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(variance);
        double cohesion = Math.max(0, 1.0 - (stdDev / Math.max(1.0, avgHealth)));

        group.cohesion = cohesion;
        return cohesion;
    }

    /**
     * 检查组群完整性
     */
    public void checkGroupIntegrity(String groupId) {
        BossGroup group = bossGroups.get(groupId);
        if (group == null) return;

        int livingBosses = group.bossIds.size();

        if (livingBosses == 0) {
            group.state = BossGroup.GroupState.BROKEN;
        } else if (livingBosses < group.type.ordinal() + 1) {
            group.state = BossGroup.GroupState.WEAKENED;
        } else {
            group.state = BossGroup.GroupState.ACTIVE;
        }
    }

    /**
     * 选择新的领导者
     */
    public void selectNewLeader(String groupId) {
        BossGroup group = bossGroups.get(groupId);
        if (group == null || group.bossIds.isEmpty()) return;

        group.leader = group.bossIds.get(0);
        logger.info("↻ 新的领导者已选出: " + group.leader + " (组: " + groupId + ")");
    }

    /**
     * 获取组群
     */
    public BossGroup getGroup(String groupId) {
        return bossGroups.get(groupId);
    }

    /**
     * 获取所有组群
     */
    public Collection<BossGroup> getAllGroups() {
        return bossGroups.values();
    }

    /**
     * 获取阵型
     */
    public BossFormation getFormation(String formationId) {
        return formations.get(formationId);
    }

    /**
     * 获取所有阵型
     */
    public Collection<BossFormation> getAllFormations() {
        return formations.values();
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total_groups", bossGroups.size());
        stats.put("total_bosses_in_groups", bossGroups.values().stream()
                .mapToInt(g -> g.bossIds.size())
                .sum());
        stats.put("available_formations", formations.size());

        // 按类型统计
        Map<String, Integer> groupTypes = new HashMap<>();
        for (BossGroup group : bossGroups.values()) {
            groupTypes.merge(group.type.name(), 1, Integer::sum);
        }
        stats.put("group_type_distribution", groupTypes);

        // 按状态统计
        Map<String, Integer> groupStates = new HashMap<>();
        for (BossGroup group : bossGroups.values()) {
            groupStates.merge(group.state.name(), 1, Integer::sum);
        }
        stats.put("group_state_distribution", groupStates);

        return stats;
    }

    /**
     * 重置所有组群
     */
    public void reset() {
        bossGroups.clear();
        logger.info("✓ 所有Boss组群已重置");
    }
}
