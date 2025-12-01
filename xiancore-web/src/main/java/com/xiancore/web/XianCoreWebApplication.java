package com.xiancore.web;

import lombok.extern.java.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * XianCore Web服务启动类
 *
 * 功能:
 * 1. Spring Boot应用入口
 * 2. REST API服务
 * 3. WebSocket实时通信
 * 4. 性能监控和告警系统
 *
 * 部署方式:
 * java -jar xiancore-web.jar --server.port=8080
 */
@Log
@SpringBootApplication
@Configuration
@EnableWebSocket
public class XianCoreWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(XianCoreWebApplication.class, args);
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   XianCore Web服务已启动               ║");
        System.out.println("║   访问地址: http://localhost:8080      ║");
        System.out.println("║   WebSocket: ws://localhost:8080/ws    ║");
        System.out.println("╚════════════════════════════════════════╝");
    }
}
