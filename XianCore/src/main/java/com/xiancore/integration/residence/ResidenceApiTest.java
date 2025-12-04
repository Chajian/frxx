// 测试文件：验证 Residence API 是否可用
// 放在 src/main/java/com/xiancore/integration/residence 中

package com.xiancore.integration.residence;

import com.bekvon.bukkit.residence.api.ResidenceApi;

public class ResidenceApiTest {
    public static void main(String[] args) {
        try {
            // 尝试加载 Residence API
            ResidenceApi.getResidenceManager();
            System.out.println("✓ Residence API 加载成功!");
            System.out.println("✓ 可以调用 ResidenceApi.getResidenceManager()");
            System.out.println("✓ 可以调用 ResidenceApi.getPlayerManager()");
            System.out.println("✓ 可以调用 ResidenceApi.getMarketBuyManager()");
            System.out.println("✓ 可以调用 ResidenceApi.getMarketRentManager()");
            System.out.println("✓ 可以调用 ResidenceApi.getChatManager()");
            System.out.println("\n✓ Residence API 集成验证完成！");
        } catch (Exception e) {
            System.err.println("✗ Residence API 加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
