package com.xiancore.common.network;

import com.google.gson.Gson;
import com.xiancore.common.constant.Constants;
import com.xiancore.common.dto.*;
import lombok.extern.java.Log;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Web服务客户端 - 插件通过HTTP与Web服务进行通信
 *
 * 职责:
 * 1. 发送Boss数据到Web服务
 * 2. 获取统计和排名信息
 * 3. 处理配置更新
 * 4. 管理网络连接和异常
 *
 * 设计原理:
 * - 使用Apache HttpClient进行HTTP通信
 * - 使用Gson进行JSON序列化/反序列化
 * - 共享DTO确保数据一致性
 * - 异步发送降低延迟
 */
@Log
public class WebServiceClient {

    private final String baseUrl;
    private final Gson gson;
    private final CloseableHttpClient httpClient;
    private volatile boolean connected = false;

    /**
     * 构造函数
     * @param baseUrl Web服务基URL，默认为 http://localhost:8080
     */
    public WebServiceClient(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl : Constants.DEFAULT_WEB_SERVICE_URL;
        this.gson = new Gson();
        this.httpClient = HttpClients.createDefault();

        // 尝试连接
        try {
            this.testConnection();
        } catch (Exception e) {
            log.log(Level.WARNING, "初始化连接到Web服务失败: " + baseUrl, e);
        }
    }

    /**
     * 测试与Web服务的连接
     */
    public void testConnection() throws Exception {
        String url = baseUrl + "/api/health";
        HttpGet request = new HttpGet(url);
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Constants.HTTP_TIMEOUT_MS)
                .setSocketTimeout(Constants.HTTP_TIMEOUT_MS)
                .build();
        request.setConfig(config);

        try {
            var response = httpClient.execute(request);
            connected = response.getStatusLine().getStatusCode() == 200;

            if (connected) {
                log.log(Level.INFO, "成功连接到Web服务: " + baseUrl);
            } else {
                log.log(Level.WARNING, "Web服务连接失败，状态码: " + response.getStatusLine().getStatusCode());
            }
            EntityUtils.consumeQuietly(response.getEntity());
        } finally {
            request.releaseConnection();
        }
    }

    /**
     * 报告Boss被击杀
     */
    public void reportBossKill(BossDTO bossData) {
        if (!connected) {
            log.log(Level.WARNING, "未连接到Web服务，无法报告Boss击杀");
            return;
        }

        asyncExecute(() -> {
            try {
                String url = baseUrl + Constants.API_BASE_PATH + "/bosses/" + bossData.getBossId() + "/kill";
                HttpPut request = new HttpPut(url);

                String jsonBody = gson.toJson(bossData);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));
                request.setHeader("Content-Type", "application/json");

                var response = httpClient.execute(request);
                if (response.getStatusLine().getStatusCode() == 200) {
                    log.log(Level.INFO, "成功上报Boss击杀: " + bossData.getBossName());
                } else {
                    log.log(Level.WARNING, "上报Boss击杀失败，状态码: " + response.getStatusLine().getStatusCode());
                }
                EntityUtils.consumeQuietly(response.getEntity());
                request.releaseConnection();
            } catch (IOException e) {
                log.log(Level.SEVERE, "报告Boss击杀异常", e);
            }
        });
    }

    /**
     * 报告伤害记录
     */
    public void reportDamage(DamageRecordDTO damageRecord) {
        if (!connected) {
            return;
        }

        asyncExecute(() -> {
            try {
                String url = baseUrl + Constants.API_BASE_PATH + "/damage";
                HttpPost request = new HttpPost(url);

                String jsonBody = gson.toJson(damageRecord);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));
                request.setHeader("Content-Type", "application/json");

                var response = httpClient.execute(request);
                EntityUtils.consumeQuietly(response.getEntity());
                request.releaseConnection();
            } catch (IOException e) {
                log.log(Level.SEVERE, "报告伤害异常", e);
            }
        });
    }

    /**
     * 同步Boss数据
     */
    public void syncBossData(BossDTO bossData) {
        if (!connected) {
            return;
        }

        asyncExecute(() -> {
            try {
                String url = baseUrl + Constants.API_BASE_PATH + "/bosses";
                HttpPost request = new HttpPost(url);

                String jsonBody = gson.toJson(bossData);
                request.setEntity(new StringEntity(jsonBody, "UTF-8"));
                request.setHeader("Content-Type", "application/json");

                var response = httpClient.execute(request);
                EntityUtils.consumeQuietly(response.getEntity());
                request.releaseConnection();
            } catch (IOException e) {
                log.log(Level.SEVERE, "同步Boss数据异常", e);
            }
        });
    }

    /**
     * 获取玩家统计信息
     */
    public PlayerStatsDTO getPlayerStats(String playerId) {
        if (!connected) {
            return null;
        }

        try {
            String url = baseUrl + Constants.API_BASE_PATH + "/stats/player/" + playerId;
            HttpGet request = new HttpGet(url);
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(Constants.HTTP_TIMEOUT_MS)
                    .setSocketTimeout(Constants.HTTP_TIMEOUT_MS)
                    .build();
            request.setConfig(config);

            var response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == 200) {
                String jsonBody = EntityUtils.toString(response.getEntity());
                ApiResponse<PlayerStatsDTO> apiResponse = gson.fromJson(
                    jsonBody,
                    ApiResponse.class
                );
                if (apiResponse.getCode() == 0) {
                    // 重新解析data字段为PlayerStatsDTO
                    String dataJson = gson.toJson(apiResponse.getData());
                    return gson.fromJson(dataJson, PlayerStatsDTO.class);
                }
            }
            EntityUtils.consumeQuietly(response.getEntity());
            request.releaseConnection();
        } catch (IOException e) {
            log.log(Level.SEVERE, "获取玩家统计异常", e);
        }

        return null;
    }

    /**
     * 异步执行网络请求
     */
    private void asyncExecute(Runnable task) {
        new Thread(task).start();
    }

    /**
     * 关闭连接
     */
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * 重新连接
     */
    public void reconnect() {
        try {
            testConnection();
        } catch (Exception e) {
            log.log(Level.WARNING, "重新连接失败", e);
        }
    }
}
