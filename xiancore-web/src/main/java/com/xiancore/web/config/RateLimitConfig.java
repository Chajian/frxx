package com.xiancore.web.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 速率限制配置
 * 限制客户端的请求频率
 */
@Configuration
public class RateLimitConfig {

    /**
     * 速率限制过滤器
     */
    public static class RateLimitFilter extends OncePerRequestFilter {

        private final Map<String, Bucket> cachesBuckets = new ConcurrentHashMap<>();

        // 每个IP地址每分钟最多请求100次
        private static final int REQUESTS_PER_MINUTE = 100;

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {

            String clientIp = getClientIpAddress(request);
            Bucket bucket = cachesBuckets.computeIfAbsent(clientIp, ip -> createNewBucket());

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"code\":429,\"message\":\"Rate limit exceeded\"}");
            }
        }

        /**
         * 创建新的限流桶
         */
        private Bucket createNewBucket() {
            Bandwidth limit = Bandwidth.classic(REQUESTS_PER_MINUTE, Refill.intervally(REQUESTS_PER_MINUTE, Duration.ofMinutes(1)));
            return Bucket4j.builder()
                    .addLimit(limit)
                    .build();
        }

        /**
         * 获取客户端IP地址
         */
        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0];
            }
            return request.getRemoteAddr();
        }
    }
}
