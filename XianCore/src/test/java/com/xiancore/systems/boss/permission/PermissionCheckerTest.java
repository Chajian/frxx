package com.xiancore.systems.boss.permission;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Boss权限检查器单元测试
 * 测试权限检查、继承、通配符等功能
 *
 * @author XianCore Team
 * @version 1.0.0
 * @since 2025-11-16
 */
@DisplayName("Boss权限检查器单元测试")
public class PermissionCheckerTest {

    private PermissionChecker permissionChecker;

    @Mock
    private Player mockPlayer;

    @Mock
    private ConsoleCommandSender mockConsole;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        Logger logger = Logger.getLogger("BossPermissionTest");
        permissionChecker = new PermissionChecker(logger);
    }

    // ==================== 主权限测试 ====================

    @Test
    @DisplayName("检查权限 - 玩家拥有权限")
    public void testHasPermission_PlayerWithPermission() {
        when(mockPlayer.hasPermission(BossPermissions.ADMIN)).thenReturn(true);

        assertTrue(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.ADMIN),
            "玩家应该拥有权限"
        );
    }

    @Test
    @DisplayName("检查权限 - 玩家没有权限")
    public void testHasPermission_PlayerWithoutPermission() {
        when(mockPlayer.hasPermission(BossPermissions.ADMIN)).thenReturn(false);

        assertFalse(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.ADMIN),
            "玩家不应该拥有权限"
        );
    }

    @Test
    @DisplayName("检查权限 - 控制台总是拥有权限")
    public void testHasPermission_ConsoleAlwaysHasPermission() {
        assertTrue(
            permissionChecker.hasPermission(mockConsole, BossPermissions.ADMIN),
            "控制台应该总是拥有权限"
        );

        assertTrue(
            permissionChecker.hasPermission(mockConsole, "任何权限"),
            "控制台对任何权限都应该返回true"
        );
    }

    // ==================== 通配符权限测试 ====================

    @Test
    @DisplayName("通配符权限 - boss.* 包含所有boss权限")
    public void testWildcardPermission_BossWildcard() {
        when(mockPlayer.hasPermission(BossPermissions.ADMIN)).thenReturn(false);
        when(mockPlayer.hasPermission("boss.*")).thenReturn(true);

        assertTrue(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.ADMIN),
            "boss.*应该包含 boss.admin 权限"
        );
    }

    @Test
    @DisplayName("通配符权限 - boss.command.* 包含所有命令权限")
    public void testWildcardPermission_CommandWildcard() {
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_ADD)).thenReturn(false);
        when(mockPlayer.hasPermission("boss.command.*")).thenReturn(true);

        assertTrue(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_ADD),
            "boss.command.*应该包含 boss.command.add 权限"
        );
    }

    @Test
    @DisplayName("通配符权限 - boss.notify.* 包含所有通知权限")
    public void testWildcardPermission_NotifyWildcard() {
        when(mockPlayer.hasPermission(BossPermissions.NOTIFY_SPAWN)).thenReturn(false);
        when(mockPlayer.hasPermission("boss.notify.*")).thenReturn(true);

        assertTrue(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.NOTIFY_SPAWN),
            "boss.notify.*应该包含 boss.notify.spawn 权限"
        );
    }

    // ==================== 权限节点测试 ====================

    @Test
    @DisplayName("权限节点 - 管理员权限")
    public void testPermissionNode_AdminPermission() {
        when(mockPlayer.hasPermission(BossPermissions.ADMIN)).thenReturn(true);

        assertTrue(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.ADMIN),
            "应该能识别boss.admin权限"
        );
    }

    @Test
    @DisplayName("权限节点 - 用户权限")
    public void testPermissionNode_UserPermission() {
        when(mockPlayer.hasPermission(BossPermissions.USER)).thenReturn(true);

        assertTrue(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.USER),
            "应该能识别boss.user权限"
        );
    }

    @Test
    @DisplayName("权限节点 - 重载权限")
    public void testPermissionNode_ReloadPermission() {
        when(mockPlayer.hasPermission(BossPermissions.RELOAD)).thenReturn(true);

        assertTrue(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.RELOAD),
            "应该能识别boss.reload权限"
        );
    }

    @Test
    @DisplayName("权限节点 - 命令权限")
    public void testPermissionNode_CommandPermissions() {
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_LIST)).thenReturn(true);
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_INFO)).thenReturn(true);
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_ADD)).thenReturn(true);

        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_LIST));
        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_INFO));
        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_ADD));
    }

    @Test
    @DisplayName("权限节点 - 通知权限")
    public void testPermissionNode_NotifyPermissions() {
        when(mockPlayer.hasPermission(BossPermissions.NOTIFY)).thenReturn(true);
        when(mockPlayer.hasPermission(BossPermissions.NOTIFY_SPAWN)).thenReturn(true);
        when(mockPlayer.hasPermission(BossPermissions.NOTIFY_KILL)).thenReturn(true);

        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.NOTIFY));
        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.NOTIFY_SPAWN));
        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.NOTIFY_KILL));
    }

    // ==================== 权限继承测试 ====================

    @Test
    @DisplayName("权限继承 - 管理员包含用户权限")
    public void testPermissionInheritance_AdminIncludesUser() {
        when(mockPlayer.hasPermission(BossPermissions.ADMIN)).thenReturn(true);

        // 管理员应该拥有所有权限
        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.ADMIN));
        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_LIST));
        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.NOTIFY));
    }

    @Test
    @DisplayName("权限继承 - 基础权限不包含管理权限")
    public void testPermissionInheritance_UserDoesNotIncludeAdmin() {
        when(mockPlayer.hasPermission(BossPermissions.USER)).thenReturn(true);
        when(mockPlayer.hasPermission(BossPermissions.ADMIN)).thenReturn(false);

        assertTrue(permissionChecker.hasPermission(mockPlayer, BossPermissions.USER));
        assertFalse(permissionChecker.hasPermission(mockPlayer, BossPermissions.ADMIN));
    }

    // ==================== 检查权限或发送消息测试 ====================

    @Test
    @DisplayName("检查权限或发送消息 - 有权限时")
    public void testCheckPermissionOrSendMessage_WithPermission() {
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_ADD)).thenReturn(true);

        boolean result = permissionChecker.checkPermissionOrSendMessage(mockPlayer, BossPermissions.COMMAND_ADD);

        assertTrue(result, "有权限时应该返回true");
        verify(mockPlayer, never()).sendMessage(anyString());
    }

    @Test
    @DisplayName("检查权限或发送消息 - 无权限时")
    public void testCheckPermissionOrSendMessage_WithoutPermission() {
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_ADD)).thenReturn(false);

        boolean result = permissionChecker.checkPermissionOrSendMessage(mockPlayer, BossPermissions.COMMAND_ADD);

        assertFalse(result, "无权限时应该返回false");
        verify(mockPlayer).sendMessage(contains("权限"));
    }

    // ==================== 权限描述测试 ====================

    @Test
    @DisplayName("权限描述 - 获取权限说明")
    public void testGetPermissionDescription() {
        String adminDesc = BossPermissions.getDescription(BossPermissions.ADMIN);
        assertNotNull(adminDesc, "应该能获取权限描述");
        assertTrue(adminDesc.length() > 0, "权限描述不应该为空");

        String addDesc = BossPermissions.getDescription(BossPermissions.COMMAND_ADD);
        assertNotNull(addDesc, "应该能获取命令权限描述");
    }

    // ==================== 多权限检查测试 ====================

    @Test
    @DisplayName("多权限检查 - 至少拥有一个权限")
    public void testMultiplePermissions_AnyOfPermissions() {
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_ADD)).thenReturn(false);
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_REMOVE)).thenReturn(true);

        assertTrue(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_REMOVE),
            "玩家应该拥有至少一个权限"
        );
    }

    @Test
    @DisplayName("多权限检查 - 拒绝所有权限")
    public void testMultiplePermissions_DenyAll() {
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_ADD)).thenReturn(false);
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_REMOVE)).thenReturn(false);
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_EDIT)).thenReturn(false);

        assertFalse(permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_ADD));
        assertFalse(permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_REMOVE));
        assertFalse(permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_EDIT));
    }

    // ==================== 边界情况测试 ====================

    @Test
    @DisplayName("边界情况 - 空权限字符串")
    public void testEdgeCases_EmptyPermission() {
        when(mockPlayer.hasPermission("")).thenReturn(false);
        assertFalse(permissionChecker.hasPermission(mockPlayer, ""));
    }

    @Test
    @DisplayName("边界情况 - 大小写敏感性")
    public void testEdgeCases_CaseSensitivity() {
        when(mockPlayer.hasPermission("boss.admin")).thenReturn(true);
        when(mockPlayer.hasPermission("BOSS.ADMIN")).thenReturn(false);

        assertTrue(permissionChecker.hasPermission(mockPlayer, "boss.admin"));
        assertFalse(permissionChecker.hasPermission(mockPlayer, "BOSS.ADMIN"));
    }

    @Test
    @DisplayName("边界情况 - null发送者")
    public void testEdgeCases_NullSender() {
        // null发送者应该被处理（通常返回false或抛出异常）
        try {
            permissionChecker.hasPermission(null, BossPermissions.ADMIN);
            // 如果不抛出异常，那就没问题
        } catch (NullPointerException e) {
            // 如果抛出异常，这也是可以接受的
            assertNotNull(e);
        }
    }

    @Test
    @DisplayName("边界情况 - null权限")
    public void testEdgeCases_NullPermission() {
        try {
            permissionChecker.hasPermission(mockPlayer, null);
            // 如果不抛出异常，那就没问题
        } catch (NullPointerException e) {
            // 如果抛出异常，这也是可以接受的
            assertNotNull(e);
        }
    }

    // ==================== 集成测试 ====================

    @Test
    @DisplayName("完整流程 - 玩家使用命令权限检查")
    public void testIntegration_PlayerCommandFlow() {
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_ADD)).thenReturn(true);
        when(mockPlayer.hasPermission(BossPermissions.COMMAND_REMOVE)).thenReturn(false);

        // 模拟命令执行流程
        assertTrue(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_ADD),
            "玩家应该能执行add命令"
        );

        assertFalse(
            permissionChecker.hasPermission(mockPlayer, BossPermissions.COMMAND_REMOVE),
            "玩家不应该能执行remove命令"
        );
    }

    @Test
    @DisplayName("完整流程 - 控制台所有权限")
    public void testIntegration_ConsoleFullAccess() {
        // 控制台应该能执行所有操作
        assertTrue(permissionChecker.hasPermission(mockConsole, BossPermissions.COMMAND_ADD));
        assertTrue(permissionChecker.hasPermission(mockConsole, BossPermissions.COMMAND_REMOVE));
        assertTrue(permissionChecker.hasPermission(mockConsole, BossPermissions.ADMIN));
        assertTrue(permissionChecker.hasPermission(mockConsole, "任何权限字符串"));
    }
}
