package com.xiancore.bridge.economy;

import com.xiancore.XianCore;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * 经济桥接系统
 * 负责与 Vault 经济系统的集成
 *
 * @author Olivia Diaz
 * @version 1.0.0
 */
@Getter
public class EconomyBridge {

    private final XianCore plugin;
    private Economy economy = null;
    private boolean initialized = false;

    public EconomyBridge(XianCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化经济桥接
     */
    public void initialize() {
        if (initialized) {
            return;
        }

        if (!setupEconomy()) {
            plugin.getLogger().warning("  §e! 未能连接到 Vault 经济系统");
            return;
        }

        initialized = true;
        plugin.getLogger().info("  §a✓ 经济桥接初始化完成");
    }

    /**
     * 设置经济系统
     *
     * @return 是否成功
     */
    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * 检查经济系统是否可用
     *
     * @return 是否可用
     */
    public boolean isAvailable() {
        return economy != null;
    }
}
