package com.example.charging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // 支付超时延时队列配置
    public static final String PAYMENT_TIMEOUT_EXCHANGE = "payment.timeout.exchange";
    public static final String PAYMENT_TIMEOUT_QUEUE = "payment.timeout.queue";
    public static final String PAYMENT_TIMEOUT_ROUTING_KEY = "payment.timeout.routing";

    // 充电启动延时队列配置
    public static final String CHARGING_DELAY_EXCHANGE = "charging.delay.exchange";
    public static final String CHARGING_DELAY_QUEUE = "charging.delay.queue";
    public static final String CHARGING_DELAY_ROUTING_KEY = "charging.delay.routing";

    @Bean
    public CustomExchange paymentTimeoutExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(PAYMENT_TIMEOUT_EXCHANGE, "x-delayed-message", true, false, args);
    }

    @Bean
    public Queue paymentTimeoutQueue() {
        return new Queue(PAYMENT_TIMEOUT_QUEUE, true);
    }

    @Bean
    public Binding paymentTimeoutBinding(Queue paymentTimeoutQueue, CustomExchange paymentTimeoutExchange) {
        return BindingBuilder.bind(paymentTimeoutQueue)
                .to(paymentTimeoutExchange)
                .with(PAYMENT_TIMEOUT_ROUTING_KEY)
                .noargs();
    }

    @Bean
    public CustomExchange chargingDelayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(CHARGING_DELAY_EXCHANGE, "x-delayed-message", true, false, args);
    }

    @Bean
    public Queue chargingDelayQueue() {
        return new Queue(CHARGING_DELAY_QUEUE, true);
    }

    @Bean
    public Binding chargingDelayBinding(Queue chargingDelayQueue, CustomExchange chargingDelayExchange) {
        return BindingBuilder.bind(chargingDelayQueue)
                .to(chargingDelayExchange)
                .with(CHARGING_DELAY_ROUTING_KEY)
                .noargs();
    }
}
