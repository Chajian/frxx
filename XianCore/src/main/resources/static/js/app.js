// XianCore Boss管理系统 - 主应用JS文件

const API_BASE = '/api';
let stompClient = null;
let isWebSocketConnected = false;

// ========================================
// WebSocket连接
// ========================================
function connectWebSocket() {
    if (isWebSocketConnected) return;

    const socket = new SockJS('/ws/boss');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('✓ WebSocket已连接');
        isWebSocketConnected = true;

        // 订阅Boss事件
        stompClient.subscribe('/topic/boss-events', function(message) {
            const event = JSON.parse(message.body);
            console.log('✓ 收到Boss事件:', event);
            onBossEventReceived(event);
        });

        // 订阅击杀事件
        stompClient.subscribe('/topic/kill-events', function(message) {
            const event = JSON.parse(message.body);
            console.log('✓ 收到击杀事件:', event);
            onKillEventReceived(event);
        });

        // 订阅统计更新
        stompClient.subscribe('/topic/stats-update', function(message) {
            const stats = JSON.parse(message.body);
            console.log('✓ 收到统计更新:', stats);
            onStatsUpdateReceived(stats);
        });

        // 订阅告警
        stompClient.subscribe('/topic/alerts', function(message) {
            const alert = JSON.parse(message.body);
            console.log('✓ 收到告警:', alert);
            onAlertReceived(alert);
        });

        // 订阅系统状态
        stompClient.subscribe('/topic/system-status', function(message) {
            const status = JSON.parse(message.body);
            console.log('✓ 收到系统状态:', status);
            onSystemStatusReceived(status);
        });

        // 订阅个人通知
        stompClient.subscribe('/user/queue/notifications', function(message) {
            const notification = JSON.parse(message.body);
            console.log('✓ 收到个人通知:', notification);
            onNotificationReceived(notification);
        });

        // 启动心跳
        startHeartbeat();
    }, function(error) {
        console.error('✗ WebSocket连接失败:', error);
        isWebSocketConnected = false;
        // 5秒后重试
        setTimeout(connectWebSocket, 5000);
    });
}

function disconnectWebSocket() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect(function() {
            console.log('✓ WebSocket已断开');
            isWebSocketConnected = false;
        });
    }
}

// ========================================
// 心跳检测
// ========================================
function startHeartbeat() {
    setInterval(() => {
        if (stompClient && stompClient.connected) {
            stompClient.send('/app/ping', {}, JSON.stringify({}));
        }
    }, 30000); // 每30秒发送一次心跳
}

// ========================================
// WebSocket事件处理
// ========================================
function onBossEventReceived(event) {
    const notification = document.createElement('div');
    notification.className = 'notification notification-boss';
    notification.innerHTML = `
        <strong>${event.bossName}</strong> (Tier ${event.tier})
        <br>${event.eventType} @ ${event.world}
    `;
    document.body.appendChild(notification);

    // 3秒后移除
    setTimeout(() => notification.remove(), 3000);

    // 如果在Boss列表页面，刷新列表
    if (document.querySelector('.section.active').id === 'bosses') {
        loadBosses();
    }
}

function onKillEventReceived(event) {
    const notification = document.createElement('div');
    notification.className = 'notification notification-kill';
    notification.innerHTML = `
        <strong>${event.killerName}</strong> 击杀了
        <strong>${event.bossName}</strong> (Tier ${event.tier})
        <br>总伤害: ${event.totalDamage.toFixed(1)}
    `;
    document.body.appendChild(notification);

    setTimeout(() => notification.remove(), 5000);

    // 刷新统计
    if (document.querySelector('.section.active').id === 'stats') {
        loadRankings();
    }
}

function onStatsUpdateReceived(stats) {
    // 更新仪表板数据
    if (document.querySelector('.section.active').id === 'dashboard') {
        document.getElementById('total-spawned').textContent = formatNumber(stats.totalBossesSpawned);
        document.getElementById('total-killed').textContent = formatNumber(stats.totalBossesKilled);
        document.getElementById('active-bosses').textContent = stats.currentActiveBosses;
        document.getElementById('total-players').textContent = stats.activePlayers;
    }
}

function onAlertReceived(alert) {
    const notification = document.createElement('div');
    const className = 'notification notification-' + alert.severity.toLowerCase();
    notification.className = className;
    notification.innerHTML = `
        <strong>[${alert.severity}] ${alert.title}</strong>
        <br>${alert.message}
    `;
    document.body.appendChild(notification);

    setTimeout(() => notification.remove(), 5000);
}

function onSystemStatusReceived(status) {
    // 更新系统状态页面
    if (document.querySelector('.section.active').id === 'health') {
        document.getElementById('cpu-usage').style.width = status.cpuUsage + '%';
        document.getElementById('cpu-text').textContent = status.cpuUsage.toFixed(1) + '%';
        document.getElementById('memory-usage').style.width = status.memoryUsage + '%';
        document.getElementById('memory-text').textContent = status.memoryUsage.toFixed(1) + '%';
        document.getElementById('active-threads').textContent = status.activeConnections;
    }
}

function onNotificationReceived(notification) {
    console.log('✓ 个人通知:', notification);
    if (notification.message) {
        alert(notification.message);
    }
}

// ========================================
// 页面初始化
// ========================================
document.addEventListener('DOMContentLoaded', function() {
    console.log('XianCore Boss管理系统已加载');
    loadDashboard();
    startAutoRefresh();
    connectWebSocket();
});

// 页面卸载时断开连接
window.addEventListener('beforeunload', function() {
    disconnectWebSocket();
});

// ========================================
// 导航切换
// ========================================
function showSection(sectionId) {
    // 隐藏所有section
    document.querySelectorAll('.section').forEach(section => {
        section.classList.remove('active');
    });

    // 隐藏所有nav-link
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });

    // 显示选中的section
    document.getElementById(sectionId).classList.add('active');

    // 高亮nav-link
    event.target.classList.add('active');

    // 加载对应数据
    switch(sectionId) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'bosses':
            loadBosses();
            break;
        case 'stats':
            loadRankings();
            break;
        case 'config':
            loadConfig();
            break;
        case 'health':
            loadHealthStatus();
            break;
    }
}

// ========================================
// 仪表板
// ========================================
function loadDashboard() {
    fetch(`${API_BASE}/stats`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const stats = data.data;
                document.getElementById('total-spawned').textContent = formatNumber(stats.totalBossesSpawned);
                document.getElementById('total-killed').textContent = formatNumber(stats.totalBossesKilled);
                document.getElementById('active-bosses').textContent = stats.currentActiveBosses;
                document.getElementById('total-players').textContent = stats.totalPlayers;
            }
        })
        .catch(error => console.error('加载仪表板失败:', error));

    // 加载图表
    loadCharts();
}

function loadCharts() {
    // 最近7天击杀统计
    const killCtx = document.getElementById('killChart');
    if (killCtx && !window.killChart) {
        window.killChart = new Chart(killCtx, {
            type: 'line',
            data: {
                labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
                datasets: [{
                    label: '击杀数',
                    data: [12, 19, 8, 15, 22, 18, 24],
                    borderColor: '#667eea',
                    backgroundColor: 'rgba(102, 126, 234, 0.1)',
                    tension: 0.3,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        position: 'top',
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    // Boss类型分布
    const typeCtx = document.getElementById('bossTypeChart');
    if (typeCtx && !window.bossTypeChart) {
        window.bossTypeChart = new Chart(typeCtx, {
            type: 'doughnut',
            data: {
                labels: ['骷髅王', '冰巨人', '天翼龙', '深渊恶魔'],
                datasets: [{
                    data: [120, 95, 72, 48],
                    backgroundColor: [
                        '#667eea',
                        '#764ba2',
                        '#f093fb',
                        '#4facfe'
                    ]
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: {
                        position: 'right',
                    }
                }
            }
        });
    }
}

// ========================================
// Boss管理
// ========================================
function loadBosses() {
    fetch(`${API_BASE}/bosses`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const tbody = document.getElementById('bosses-list');
                tbody.innerHTML = '';

                data.data.forEach((boss, index) => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${boss.id}</td>
                        <td>${boss.type}</td>
                        <td>${boss.world}</td>
                        <td>${Math.round(boss.x)}, ${Math.round(boss.y)}, ${Math.round(boss.z)}</td>
                        <td>${boss.tier}</td>
                        <td>${boss.health.toFixed(1)}</td>
                        <td><span class="badge badge-${boss.status.toLowerCase()}">${boss.status}</span></td>
                        <td>${formatDate(boss.spawnTime)}</td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="editBoss('${boss.id}')">编辑</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteBoss('${boss.id}')">删除</button>
                        </td>
                    `;
                    tbody.appendChild(row);
                });
            }
        })
        .catch(error => console.error('加载Boss列表失败:', error));
}

function showCreateBossForm() {
    alert('创建Boss功能待实现');
}

function editBoss(bossId) {
    alert('编辑Boss功能待实现: ' + bossId);
}

function deleteBoss(bossId) {
    if (confirm('确定要删除该Boss吗?')) {
        fetch(`${API_BASE}/bosses/${bossId}`, {
            method: 'DELETE'
        })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert('Boss已删除');
                loadBosses();
            }
        })
        .catch(error => console.error('删除Boss失败:', error));
    }
}

// ========================================
// 统计分析
// ========================================
function loadRankings() {
    const type = document.getElementById('rank-type').value || 'kills';

    fetch(`${API_BASE}/stats/ranking?type=${type}&limit=10`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const tbody = document.getElementById('rankings-list');
                tbody.innerHTML = '';

                data.data.forEach((player, index) => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${player.rank}</td>
                        <td>${player.playerName}</td>
                        <td>${player.totalKills}</td>
                        <td>${player.totalDamage.toFixed(1)}</td>
                        <td>${player.averageDamage.toFixed(1)}</td>
                        <td>${formatDate(player.lastKillTime)}</td>
                    `;
                    tbody.appendChild(row);
                });
            }
        })
        .catch(error => console.error('加载排名失败:', error));
}

// ========================================
// 配置管理
// ========================================
function loadConfig() {
    fetch(`${API_BASE}/config`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const config = data.data;
                if (config['max-active-bosses']) {
                    document.getElementById('max-active-bosses').value = config['max-active-bosses'].value;
                }
                if (config['min-online-players']) {
                    document.getElementById('min-online-players').value = config['min-online-players'].value;
                }
                if (config['check-interval']) {
                    document.getElementById('check-interval').value = config['check-interval'].value;
                }
                if (config['enable-rewards']) {
                    document.getElementById('enable-rewards').checked = config['enable-rewards'].value;
                }
                if (config['enable-damage-tracking']) {
                    document.getElementById('enable-damage-tracking').checked = config['enable-damage-tracking'].value;
                }
            }
        })
        .catch(error => console.error('加载配置失败:', error));
}

function saveConfig() {
    const configData = {
        'max-active-bosses': parseInt(document.getElementById('max-active-bosses').value),
        'min-online-players': parseInt(document.getElementById('min-online-players').value),
        'check-interval': parseInt(document.getElementById('check-interval').value),
        'enable-rewards': document.getElementById('enable-rewards').checked,
        'enable-damage-tracking': document.getElementById('enable-damage-tracking').checked
    };

    fetch(`${API_BASE}/config`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(configData)
    })
    .then(response => response.json())
    .then(data => {
        const messageDiv = document.getElementById('config-message');
        if (data.success) {
            messageDiv.textContent = '✅ 配置保存成功';
            messageDiv.className = 'message success';
        } else {
            messageDiv.textContent = '❌ 配置保存失败: ' + data.message;
            messageDiv.className = 'message error';
        }
    })
    .catch(error => {
        console.error('保存配置失败:', error);
        const messageDiv = document.getElementById('config-message');
        messageDiv.textContent = '❌ 保存配置失败';
        messageDiv.className = 'message error';
    });
}

function validateConfig() {
    const configData = {
        'max-active-bosses': parseInt(document.getElementById('max-active-bosses').value),
        'min-online-players': parseInt(document.getElementById('min-online-players').value),
        'check-interval': parseInt(document.getElementById('check-interval').value)
    };

    fetch(`${API_BASE}/config/validate`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(configData)
    })
    .then(response => response.json())
    .then(data => {
        const messageDiv = document.getElementById('config-message');
        if (data.data.valid) {
            messageDiv.textContent = '✅ 配置验证通过';
            messageDiv.className = 'message success';
        } else {
            messageDiv.textContent = '❌ 验证失败: ' + data.data.errors.join(', ');
            messageDiv.className = 'message error';
        }
    })
    .catch(error => console.error('验证配置失败:', error));
}

function resetConfig() {
    if (confirm('确定要重置配置吗?')) {
        loadConfig();
        const messageDiv = document.getElementById('config-message');
        messageDiv.textContent = '⚠️ 已重置为上次保存的配置';
        messageDiv.className = 'message success';
    }
}

// ========================================
// 系统健康状态
// ========================================
function loadHealthStatus() {
    fetch(`${API_BASE}/stats/health`)
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                const health = data.data;
                document.getElementById('cpu-usage').style.width = health.cpu_usage + '%';
                document.getElementById('cpu-text').textContent = health.cpu_usage.toFixed(1) + '%';
                document.getElementById('memory-usage').style.width = health.memory_usage + '%';
                document.getElementById('memory-text').textContent = health.memory_usage.toFixed(1) + '%';
                document.getElementById('active-threads').textContent = health.active_threads;
                document.getElementById('database-status').textContent = health.database_connected ? '✅ 已连接' : '❌ 未连接';
                document.getElementById('websocket-status').textContent = health.websocket_connected ? '✅ 已连接' : '❌ 未连接';
            }
        })
        .catch(error => console.error('加载系统状态失败:', error));
}

// ========================================
// 工具函数
// ========================================
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

function formatDate(timestamp) {
    const date = new Date(timestamp);
    return date.toLocaleDateString('zh-CN') + ' ' + date.toLocaleTimeString('zh-CN');
}

function startAutoRefresh() {
    // 每30秒自动刷新一次
    setInterval(() => {
        const activeSection = document.querySelector('.section.active');
        if (activeSection) {
            const sectionId = activeSection.id;
            if (sectionId === 'dashboard') {
                loadDashboard();
            } else if (sectionId === 'bosses') {
                loadBosses();
            } else if (sectionId === 'stats') {
                loadRankings();
            } else if (sectionId === 'health') {
                loadHealthStatus();
            }
        }
    }, 30000);
}
