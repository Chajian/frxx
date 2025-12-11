package com.xiancore.core.realm;

import java.util.*;
import java.util.logging.Logger;

/**
 * 境界注册器
 * 管理所有境界的注册、查询和比较
 *
 * @author XianCore Team
 * @version 1.0.0
 */
public class RealmRegistry {

    private final Logger logger;

    /**
     * ID -> 境界 映射
     */
    private final Map<String, Realm> realmsById = new HashMap<>();

    /**
     * 名称 -> 境界 映射
     */
    private final Map<String, Realm> realmsByName = new HashMap<>();

    /**
     * 按顺序排列的境界列表
     */
    private final List<Realm> orderedRealms = new ArrayList<>();

    /**
     * 关键词 -> 境界 映射 (用于从怪物名称解析境界)
     */
    private final Map<String, Realm> realmsByKeyword = new HashMap<>();

    /**
     * 默认境界 (通常是最低境界)
     */
    private Realm defaultRealm;

    public RealmRegistry(Logger logger) {
        this.logger = logger;
    }

    /**
     * 注册一个境界
     *
     * @param realm 境界对象
     */
    public void register(Realm realm) {
        realmsById.put(realm.getId().toLowerCase(), realm);
        realmsByName.put(realm.getName(), realm);

        // 注册关键词
        for (String keyword : realm.getKeywords()) {
            realmsByKeyword.put(keyword.toLowerCase(), realm);
        }

        // 重新排序
        orderedRealms.add(realm);
        orderedRealms.sort(Comparator.comparingInt(Realm::getOrder));

        // 设置默认境界为order最小的
        if (defaultRealm == null || realm.getOrder() < defaultRealm.getOrder()) {
            defaultRealm = realm;
        }

        logger.fine("已注册境界: " + realm.getName() + " (ID: " + realm.getId() + ")");
    }

    /**
     * 通过ID获取境界
     *
     * @param id 境界ID
     * @return 境界对象，未找到返回null
     */
    public Realm getById(String id) {
        if (id == null) return null;
        return realmsById.get(id.toLowerCase());
    }

    /**
     * 通过名称获取境界
     *
     * @param name 境界名称 (如 "炼气期")
     * @return 境界对象，未找到返回null
     */
    public Realm getByName(String name) {
        if (name == null) return null;
        return realmsByName.get(name);
    }

    /**
     * 通过关键词获取境界 (用于解析怪物名称)
     *
     * @param keyword 关键词
     * @return 境界对象，未找到返回null
     */
    public Realm getByKeyword(String keyword) {
        if (keyword == null) return null;
        return realmsByKeyword.get(keyword.toLowerCase());
    }

    /**
     * 获取下一个境界
     *
     * @param current 当前境界
     * @return 下一境界，如果已是最高境界则返回null
     */
    public Realm getNext(Realm current) {
        if (current == null || current.isMaxRealm()) {
            return null;
        }
        return getById(current.getNextRealmId());
    }

    /**
     * 获取下一个境界 (通过名称)
     *
     * @param currentName 当前境界名称
     * @return 下一境界，如果已是最高境界则返回null
     */
    public Realm getNextByName(String currentName) {
        Realm current = getByName(currentName);
        return getNext(current);
    }

    /**
     * 比较两个境界的高低
     *
     * @param name1 境界1名称
     * @param name2 境界2名称
     * @return 负数表示name1 < name2，0表示相等，正数表示name1 > name2
     */
    public int compare(String name1, String name2) {
        Realm r1 = getByName(name1);
        Realm r2 = getByName(name2);

        if (r1 == null && r2 == null) return 0;
        if (r1 == null) return -1;
        if (r2 == null) return 1;

        return Integer.compare(r1.getOrder(), r2.getOrder());
    }

    /**
     * 判断境界1是否高于境界2
     *
     * @param name1 境界1名称
     * @param name2 境界2名称
     * @return true 如果境界1高于境界2
     */
    public boolean isHigher(String name1, String name2) {
        return compare(name1, name2) > 0;
    }

    /**
     * 判断境界1是否低于境界2
     *
     * @param name1 境界1名称
     * @param name2 境界2名称
     * @return true 如果境界1低于境界2
     */
    public boolean isLower(String name1, String name2) {
        return compare(name1, name2) < 0;
    }

    /**
     * 获取所有境界 (按顺序)
     *
     * @return 境界列表
     */
    public List<Realm> getAll() {
        return Collections.unmodifiableList(orderedRealms);
    }

    /**
     * 获取所有境界名称 (按顺序)
     *
     * @return 境界名称列表
     */
    public List<String> getAllNames() {
        List<String> names = new ArrayList<>();
        for (Realm realm : orderedRealms) {
            names.add(realm.getName());
        }
        return names;
    }

    /**
     * 获取默认境界 (最低境界)
     *
     * @return 默认境界
     */
    public Realm getDefault() {
        return defaultRealm;
    }

    /**
     * 获取最高境界
     *
     * @return 最高境界
     */
    public Realm getHighest() {
        if (orderedRealms.isEmpty()) {
            return null;
        }
        return orderedRealms.get(orderedRealms.size() - 1);
    }

    /**
     * 检查境界名称是否有效
     *
     * @param name 境界名称
     * @return true 如果有效
     */
    public boolean isValidRealm(String name) {
        return getByName(name) != null;
    }

    /**
     * 获取注册的境界数量
     *
     * @return 境界数量
     */
    public int size() {
        return orderedRealms.size();
    }

    /**
     * 清空所有注册的境界
     */
    public void clear() {
        realmsById.clear();
        realmsByName.clear();
        realmsByKeyword.clear();
        orderedRealms.clear();
        defaultRealm = null;
    }

    /**
     * 从文本中解析境界 (用于怪物名称等)
     *
     * @param text 文本
     * @return 解析出的境界，未找到返回默认境界
     */
    public Realm parseFromText(String text) {
        if (text == null || text.isEmpty()) {
            return defaultRealm;
        }

        String lowerText = text.toLowerCase();

        // 先尝试直接匹配境界名称
        for (Realm realm : orderedRealms) {
            if (text.contains(realm.getName())) {
                return realm;
            }
        }

        // 再尝试关键词匹配
        for (Map.Entry<String, Realm> entry : realmsByKeyword.entrySet()) {
            if (lowerText.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return defaultRealm;
    }
}
