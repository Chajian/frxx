package com.xiancore.systems.skill;

import com.xiancore.XianCore;
import com.xiancore.core.data.PlayerData;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 功法快捷键绑定管理器
 * 负责管理玩家的功法槽位绑定
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class SkillBindManager {

    private final XianCore plugin;
    
    // 内存缓存: UUID -> (槽位 -> 功法ID)
    private final Map<UUID, Map<Integer, String>> bindingsCache;
    
    // 脏数据标记（需要保存的玩家）
    private final Set<UUID> dirtyPlayers;

    public SkillBindManager(XianCore plugin) {
        this.plugin = plugin;
        this.bindingsCache = new HashMap<>();
        this.dirtyPlayers = new HashSet<>();
        
        // 启动定时保存任务（每30秒保存一次脏数据）
        startAutoSaveTask();
    }

    /**
     * 绑定功法到槽位
     *
     * @param player  玩家
     * @param slot    槽位 (1-9)
     * @param skillId 功法ID
     * @return 是否成功绑定
     */
    public boolean bindSkill(Player player, int slot, String skillId) {
        // 1. 验证槽位范围
        if (slot < 1 || slot > 9) {
            player.sendMessage("§c槽位范围必须在 1-9 之间!");
            return false;
        }

        // 2. 检查功法是否存在
        Skill skill = plugin.getSkillSystem().getSkill(skillId);
        if (skill == null) {
            player.sendMessage("§c功法不存在: " + skillId);
            return false;
        }

        // 3. 检查功法是否已学习
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage("§c数据加载失败!");
            return false;
        }

        if (!data.getSkills().containsKey(skillId)) {
            player.sendMessage("§c你还没有学习这个功法!");
            player.sendMessage("§7功法名称: §f" + skill.getName());
            return false;
        }

        // 4. 绑定到缓存
        Map<Integer, String> playerBindings = bindingsCache.computeIfAbsent(
                player.getUniqueId(), 
                k -> new HashMap<>()
        );
        
        // 检查是否已有绑定
        String oldSkill = playerBindings.get(slot);
        playerBindings.put(slot, skillId);
        
        // 标记为脏数据
        dirtyPlayers.add(player.getUniqueId());

        // 5. 发送成功消息
        if (oldSkill != null) {
            Skill oldSkillObj = plugin.getSkillSystem().getSkill(oldSkill);
            String oldSkillName = oldSkillObj != null ? oldSkillObj.getName() : oldSkill;
            player.sendMessage("§a已更新槽位 §f" + slot + " §a的绑定");
            player.sendMessage("§7原功法: §f" + oldSkillName);
            player.sendMessage("§7新功法: §e" + skill.getName());
        } else {
            player.sendMessage("§a已绑定 §e" + skill.getName() + " §a到槽位 §f" + slot);
        }
        
        player.sendMessage("§7按 §eF键 §7+ 切换到槽位 §f" + slot + " §7即可快速施放");

        return true;
    }

    /**
     * 解绑槽位
     *
     * @param player 玩家
     * @param slot   槽位 (1-9)
     * @return 是否成功解绑
     */
    public boolean unbindSkill(Player player, int slot) {
        // 验证槽位范围
        if (slot < 1 || slot > 9) {
            player.sendMessage("§c槽位范围必须在 1-9 之间!");
            return false;
        }

        Map<Integer, String> playerBindings = bindingsCache.get(player.getUniqueId());
        
        if (playerBindings == null || !playerBindings.containsKey(slot)) {
            player.sendMessage("§c槽位 " + slot + " 未绑定任何功法!");
            return false;
        }

        String removedSkillId = playerBindings.remove(slot);
        dirtyPlayers.add(player.getUniqueId());

        // 获取功法名称
        Skill skill = plugin.getSkillSystem().getSkill(removedSkillId);
        String skillName = skill != null ? skill.getName() : removedSkillId;

        player.sendMessage("§7已解绑槽位 §f" + slot + " §7的功法: §f" + skillName);
        
        return true;
    }

    /**
     * 获取槽位绑定的功法ID
     *
     * @param player 玩家
     * @param slot   槽位 (1-9)
     * @return 功法ID，未绑定返回null
     */
    public String getBinding(Player player, int slot) {
        Map<Integer, String> playerBindings = bindingsCache.get(player.getUniqueId());
        return playerBindings != null ? playerBindings.get(slot) : null;
    }

    /**
     * 获取玩家的所有绑定
     *
     * @param player 玩家
     * @return 槽位 -> 功法ID 的映射
     */
    public Map<Integer, String> getAllBindings(Player player) {
        Map<Integer, String> playerBindings = bindingsCache.get(player.getUniqueId());
        return playerBindings != null ? new HashMap<>(playerBindings) : new HashMap<>();
    }

    /**
     * 加载玩家的绑定数据
     * 
     * @param player 玩家
     */
    public void loadBindings(Player player) {
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            return;
        }

        Map<Integer, String> bindings = data.getSkillBindings();
        if (bindings != null && !bindings.isEmpty()) {
            bindingsCache.put(player.getUniqueId(), new HashMap<>(bindings));
            plugin.getLogger().info("已加载玩家 " + player.getName() + " 的 " + bindings.size() + " 个功法绑定");
        }
    }

    /**
     * 保存玩家的绑定数据
     *
     * @param player 玩家
     */
    public void saveBindings(Player player) {
        Map<Integer, String> bindings = bindingsCache.get(player.getUniqueId());
        
        PlayerData data = plugin.getDataManager().loadPlayerData(player.getUniqueId());
        if (data == null) {
            plugin.getLogger().warning("无法保存绑定: 玩家数据为空 - " + player.getName());
            return;
        }

        // 更新绑定数据
        if (bindings != null) {
            data.setSkillBindings(new HashMap<>(bindings));
        } else {
            data.setSkillBindings(new HashMap<>());
        }

        // 保存到数据库/文件
        plugin.getDataManager().savePlayerData(data);
        
        // 从脏数据集中移除
        dirtyPlayers.remove(player.getUniqueId());
    }

    /**
     * 保存所有脏数据
     */
    private void saveDirtyBindings() {
        if (dirtyPlayers.isEmpty()) {
            return;
        }

        int savedCount = 0;
        Iterator<UUID> iterator = dirtyPlayers.iterator();
        
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            Player player = plugin.getServer().getPlayer(playerId);
            
            if (player != null && player.isOnline()) {
                saveBindings(player);
                savedCount++;
            } else {
                // 玩家离线，从缓存中移除
                bindingsCache.remove(playerId);
                iterator.remove();
            }
        }

        if (savedCount > 0) {
            plugin.getLogger().info("自动保存了 " + savedCount + " 个玩家的功法绑定");
        }
    }

    /**
     * 启动自动保存任务
     */
    private void startAutoSaveTask() {
        // 每30秒执行一次（600 ticks）
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::saveDirtyBindings,
                600L,
                600L
        );
    }

    /**
     * 玩家登录时调用
     *
     * @param player 玩家
     */
    public void onPlayerJoin(Player player) {
        loadBindings(player);
        
        // 首次登录教程
        if (!player.hasPlayedBefore()) {
            showTutorial(player);
        }
    }

    /**
     * 玩家退出时调用
     *
     * @param player 玩家
     */
    public void onPlayerQuit(Player player) {
        // 保存绑定
        if (dirtyPlayers.contains(player.getUniqueId())) {
            saveBindings(player);
        }
        
        // 清理缓存
        bindingsCache.remove(player.getUniqueId());
    }

    /**
     * 显示新手教程
     *
     * @param player 玩家
     */
    private void showTutorial(Player player) {
        // 检查配置是否启用教程
        boolean showTutorial = plugin.getConfigManager().getConfig("config")
                .getBoolean("skill.keybind.show-tutorial", true);
        
        if (!showTutorial) {
            return;
        }

        // 延迟3秒后显示（让玩家先看完其他消息）
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            player.sendMessage("");
            player.sendMessage("§e§l========== 功法快捷键使用教程 ==========");
            player.sendMessage("");
            player.sendMessage("§71. 使用 §e/skill bindgui §7打开绑定界面");
            player.sendMessage("§7   或使用命令 §e/skill bind <槽位> <功法ID>");
            player.sendMessage("");
            player.sendMessage("§72. 将功法绑定到槽位 §f1-9");
            player.sendMessage("§7   例如: §e/skill bind 1 fireball");
            player.sendMessage("");
            player.sendMessage("§73. 切换快捷栏到对应槽位");
            player.sendMessage("§7   然后按 §eF键 §7即可快速施放功法!");
            player.sendMessage("");
            player.sendMessage("§74. 使用 §e/skill binds §7查看当前绑定");
            player.sendMessage("");
            player.sendMessage("§e§l======================================");
            player.sendMessage("");
        }, 60L); // 3秒延迟
    }

    /**
     * 解绑指定功法的所有槽位
     * 用于遗忘功法时清理绑定
     *
     * @param player  玩家
     * @param skillId 功法ID
     * @return 解绑的槽位数量
     */
    public int unbindAllSlotsForSkill(Player player, String skillId) {
        Map<Integer, String> playerBindings = bindingsCache.get(player.getUniqueId());
        
        if (playerBindings == null || playerBindings.isEmpty()) {
            return 0;
        }

        int unboundCount = 0;
        List<Integer> slotsToRemove = new ArrayList<>();

        // 找出所有绑定了该功法的槽位
        for (Map.Entry<Integer, String> entry : playerBindings.entrySet()) {
            if (skillId.equals(entry.getValue())) {
                slotsToRemove.add(entry.getKey());
                unboundCount++;
            }
        }

        // 移除绑定
        for (Integer slot : slotsToRemove) {
            playerBindings.remove(slot);
        }

        if (unboundCount > 0) {
            dirtyPlayers.add(player.getUniqueId());
            
            // 获取功法名称
            Skill skill = plugin.getSkillSystem().getSkill(skillId);
            String skillName = skill != null ? skill.getName() : skillId;
            
            player.sendMessage("§7已自动解绑 §f" + unboundCount + " §7个槽位的功法: §f" + skillName);
        }

        return unboundCount;
    }

    /**
     * 清理所有数据（服务器关闭时）
     */
    public void shutdown() {
        // 保存所有脏数据
        saveDirtyBindings();
        
        // 清理缓存
        bindingsCache.clear();
        dirtyPlayers.clear();
        
        plugin.getLogger().info("  §a✓ 功法绑定管理器已关闭");
    }
}


