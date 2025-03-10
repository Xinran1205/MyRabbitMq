package com.example.charging.service;

import com.example.charging.model.Order;
import com.example.charging.model.OrderStatus;
import com.example.charging.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ReservationService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderRepository orderRepository; // Spring Data JPA 仓库

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 支付超时延时：10分钟（毫秒）
    private static final long PAYMENT_TIMEOUT_MS = 10 * 60 * 1000;

    // RabbitMQ 延时队列相关常量
    public static final String PAYMENT_TIMEOUT_EXCHANGE = "payment.timeout.exchange";
    public static final String PAYMENT_TIMEOUT_ROUTING_KEY = "payment.timeout.routing";

    /**
     * 预约充电：锁定指定充电桩和时间段，创建订单并入队支付超时消息
     */
    public Order reserveCharging(String chargerId, String timeSlot, Long userId, long appointmentTime) {
        String lockKey = "charging:lock:" + chargerId + ":" + timeSlot;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                // 检查充电时段是否可用（省略具体业务逻辑）
                Order order = new Order();
                order.setChargerId(chargerId);
                order.setTimeSlot(timeSlot);
                order.setUserId(userId);
                order.setAppointmentTime(appointmentTime);
                order.setStatus(OrderStatus.PENDING_PAYMENT);
                order.setCreateTime(System.currentTimeMillis());
                orderRepository.save(order);

                // 投递支付超时延时消息
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", order.getId());
                String messageBody = new ObjectMapper().writeValueAsString(payload);
                MessageProperties props = new MessageProperties();
                props.setHeader("x-delay", PAYMENT_TIMEOUT_MS);
                Message message = new Message(messageBody.getBytes(StandardCharsets.UTF_8), props);
                rabbitTemplate.send(PAYMENT_TIMEOUT_EXCHANGE, PAYMENT_TIMEOUT_ROUTING_KEY, message);

                // 此处锁在支付成功或超时处理时释放
                return order;
            } else {
                throw new RuntimeException("预约资源繁忙，请稍后重试");
            }
        } catch (InterruptedException | JsonProcessingException e) {
            throw new RuntimeException("预约失败", e);
        }
    }
}

