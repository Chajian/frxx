package com.xiancore.ai;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 路径寻找AI - A*寻路算法实现
 * Pathfinding AI - A* Algorithm Implementation
 *
 * @author XianCore
 * @version 1.0
 */
public class PathfindingAI {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private final int gridSize;
    private final Map<String, Node> nodeCache = new ConcurrentHashMap<>();

    /**
     * 网格节点
     */
    public static class Node {
        public int x, y, z;
        public double g;              // 从起点到当前节点的成本
        public double h;              // 从当前节点到目标的启发式距离
        public double f;              // g + h
        public Node parent;
        public NodeState state;       // OPEN/CLOSED

        public enum NodeState {
            OPEN, CLOSED
        }

        public Node(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.g = 0;
            this.h = 0;
            this.f = 0;
            this.parent = null;
            this.state = NodeState.OPEN;
        }

        public String getKey() {
            return x + "," + y + "," + z;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Node node)) return false;
            return x == node.x && y == node.y && z == node.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    /**
     * 路径结果
     */
    public static class PathResult {
        public List<Node> path;
        public double distance;
        public int nodesExpanded;
        public long computeTime;
        public boolean success;

        public PathResult() {
            this.path = new ArrayList<>();
            this.distance = 0;
            this.nodesExpanded = 0;
            this.computeTime = 0;
            this.success = false;
        }
    }

    /**
     * 构造函数
     */
    public PathfindingAI(int gridSize) {
        this.gridSize = gridSize;
        logger.info("✓ PathfindingAI已初始化 (网格大小: " + gridSize + ")");
    }

    /**
     * A*寻路算法
     */
    public PathResult findPath(int startX, int startY, int startZ,
                               int endX, int endY, int endZ) {
        long startTime = System.currentTimeMillis();
        PathResult result = new PathResult();

        Node startNode = new Node(startX, startY, startZ);
        Node endNode = new Node(endX, endY, endZ);

        PriorityQueue<Node> openSet = new PriorityQueue<>(
                Comparator.comparingDouble((Node n) -> n.f)
        );
        Set<String> closedSet = new HashSet<>();

        startNode.h = heuristic(startNode, endNode);
        startNode.f = startNode.h;
        openSet.add(startNode);

        int nodesExpanded = 0;

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            nodesExpanded++;

            if (current.equals(endNode)) {
                result.path = reconstructPath(current);
                result.success = true;
                result.nodesExpanded = nodesExpanded;
                result.computeTime = System.currentTimeMillis() - startTime;
                result.distance = calculateDistance(result.path);
                logger.info("✓ 路径寻找成功: " + result.path.size() + "个节点");
                return result;
            }

            closedSet.add(current.getKey());

            // 检查所有邻居 (26方向立体网格)
            for (Node neighbor : getNeighbors(current, endNode)) {
                if (closedSet.contains(neighbor.getKey())) {
                    continue;
                }

                double tentativeG = current.g + distance(current, neighbor);

                Node openNode = findNodeInQueue(openSet, neighbor);
                if (openNode != null && tentativeG >= openNode.g) {
                    continue;
                }

                neighbor.g = tentativeG;
                neighbor.h = heuristic(neighbor, endNode);
                neighbor.f = neighbor.g + neighbor.h;
                neighbor.parent = current;

                if (openNode == null) {
                    openSet.add(neighbor);
                }
            }
        }

        result.success = false;
        result.nodesExpanded = nodesExpanded;
        result.computeTime = System.currentTimeMillis() - startTime;
        logger.warning("⚠ 路径寻找失败: 无法到达目标");
        return result;
    }

    /**
     * 启发式函数 (欧几里得距离)
     */
    private double heuristic(Node a, Node b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * 计算两个节点间距离
     */
    private double distance(Node a, Node b) {
        return heuristic(a, b);
    }

    /**
     * 获取邻近节点 (26方向)
     */
    private List<Node> getNeighbors(Node current, Node target) {
        List<Node> neighbors = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    int nx = current.x + dx;
                    int ny = current.y + dy;
                    int nz = current.z + dz;

                    // 边界检查
                    if (isWalkable(nx, ny, nz)) {
                        neighbors.add(new Node(nx, ny, nz));
                    }
                }
            }
        }

        return neighbors;
    }

    /**
     * 检查节点是否可行走
     */
    private boolean isWalkable(int x, int y, int z) {
        // 简单的网格边界检查
        return x >= 0 && x < gridSize &&
               y >= 0 && y < gridSize &&
               z >= 0 && z < gridSize;
    }

    /**
     * 在队列中查找节点
     */
    private Node findNodeInQueue(PriorityQueue<Node> queue, Node target) {
        for (Node node : queue) {
            if (node.equals(target)) {
                return node;
            }
        }
        return null;
    }

    /**
     * 重建路径
     */
    private List<Node> reconstructPath(Node node) {
        List<Node> path = new ArrayList<>();
        while (node != null) {
            path.add(0, node);
            node = node.parent;
        }
        return path;
    }

    /**
     * 计算路径距离
     */
    private double calculateDistance(List<Node> path) {
        if (path.isEmpty()) return 0;

        double distance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            distance += distance(path.get(i), path.get(i + 1));
        }
        return distance;
    }

    /**
     * 平滑路径 (移除不必要的转向)
     */
    public List<Node> smoothPath(List<Node> path) {
        if (path.size() <= 2) return path;

        List<Node> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));

        for (int i = 1; i < path.size() - 1; i++) {
            Node prev = smoothed.get(smoothed.size() - 1);
            Node current = path.get(i);
            Node next = path.get(i + 1);

            // 如果三点共线，跳过中点
            if (!isCollinear(prev, current, next)) {
                smoothed.add(current);
            }
        }

        smoothed.add(path.get(path.size() - 1));
        return smoothed;
    }

    /**
     * 检查三点是否共线
     */
    private boolean isCollinear(Node a, Node b, Node c) {
        // 使用叉积检查共线性
        double v1x = b.x - a.x;
        double v1y = b.y - a.y;
        double v1z = b.z - a.z;

        double v2x = c.x - a.x;
        double v2y = c.y - a.y;
        double v2z = c.z - a.z;

        double crossX = v1y * v2z - v1z * v2y;
        double crossY = v1z * v2x - v1x * v2z;
        double crossZ = v1x * v2y - v1y * v2x;

        double crossMagnitude = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
        return crossMagnitude < 0.001;  // 接近于0
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        nodeCache.clear();
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("grid_size", gridSize);
        stats.put("cached_nodes", nodeCache.size());
        return stats;
    }
}
