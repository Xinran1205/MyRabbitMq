package com.example.charging.config;

import org.redisson.api.RedissonClient;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig extends RedissonAutoConfiguration {
    // 这里采用 spring-boot-starter-redisson 自动加载配置，可在 application.yml 中配置
}
