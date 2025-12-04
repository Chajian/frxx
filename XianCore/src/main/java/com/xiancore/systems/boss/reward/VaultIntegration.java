package com.xiancore.systems.boss.reward;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Logger;

/**
 * Vault经济系统集成
 * 
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-30
 */
public class VaultIntegration {
    
    private static Economy economy = null;
    private static boolean enabled = false;
    private static final Logger logger = Logger.getLogger("XianCore");
    
    /**
     * 初始化Vault集成
     */
    public static boolean setup() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            logger.warning("未检测到Vault插件，金钱奖励将不可用");
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer()
            .getServicesManager()
            .getRegistration(Economy.class);
            
        if (rsp == null) {
            logger.warning("未找到Economy服务提供者，金钱奖励将不可用");
            return false;
        }
        
        economy = rsp.getProvider();
        enabled = economy != null;
        
        if (enabled) {
            logger.info("§a✓ Vault经济集成已启用 (" + economy.getName() + ")");
        }
        
        return enabled;
    }
    
    /**
     * 检查是否已启用
     */
    public static boolean isEnabled() {
        return enabled && economy != null;
    }
    
    /**
     * 给予玩家金钱
     * 
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public static boolean giveMoney(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            economy.depositPlayer(player, amount);
            return true;
        } catch (Exception e) {
            logger.warning("给予玩家 " + player.getName() + " 金钱失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 扣除玩家金钱
     * 
     * @param player 玩家
     * @param amount 金额
     * @return 是否成功
     */
    public static boolean takeMoney(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            if (!economy.has(player, amount)) {
                return false;
            }
            economy.withdrawPlayer(player, amount);
            return true;
        } catch (Exception e) {
            logger.warning("扣除玩家 " + player.getName() + " 金钱失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取玩家余额
     * 
     * @param player 玩家
     * @return 余额
     */
    public static double getBalance(Player player) {
        if (!isEnabled()) {
            return 0.0;
        }
        
        try {
            return economy.getBalance(player);
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    /**
     * 检查玩家是否有足够的钱
     * 
     * @param player 玩家
     * @param amount 金额
     * @return 是否有足够的钱
     */
    public static boolean has(Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        
        try {
            return economy.has(player, amount);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 格式化金钱数量
     * 
     * @param amount 金额
     * @return 格式化后的字符串
     */
    public static String format(double amount) {
        if (!isEnabled()) {
            return String.format("%.2f", amount);
        }
        
        try {
            return economy.format(amount);
        } catch (Exception e) {
            return String.format("%.2f", amount);
        }
    }
    
    /**
     * 获取货币名称（单数）
     */
    public static String getCurrencyName() {
        if (!isEnabled()) {
            return "金币";
        }
        
        try {
            return economy.currencyNameSingular();
        } catch (Exception e) {
            return "金币";
        }
    }
    
    /**
     * 获取货币名称（复数）
     */
    public static String getCurrencyNamePlural() {
        if (!isEnabled()) {
            return "金币";
        }
        
        try {
            return economy.currencyNamePlural();
        } catch (Exception e) {
            return "金币";
        }
    }
}
