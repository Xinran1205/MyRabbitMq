package com.example.charging.service;

import com.example.charging.config.RabbitMQConfig;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ReservationService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 支付超时延时：10分钟（毫秒）
    private static final long PAYMENT_TIMEOUT_MS = 10 * 60 * 1000;

    public Order reserveCharging(String chargerId, String timeSlot, Long userId, long appointmentTime) {
        String lockKey = "charging:lock:" + chargerId + ":" + timeSlot;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 尝试获取锁
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                // 在锁保护下检查资源可用性（省略检查逻辑）
                // 减少脏数据风险：
                // 通过先获取锁，再校验数据库状态，可以确保在校验过程中没有其他并发请求修改同一时段的预约状态，从而保证数据一致性。
                // 分布式锁的作用：
                // 分布式锁的核心作用就是防止并发问题。只有在锁内进行关键业务逻辑（如校验时间是否被预约、创建订单等），
                // 才能确保这些操作的安全性。
                List<Order> existingOrders = orderRepository.findByChargerIdAndTimeSlotAndStatusIn(
                        chargerId,
                        timeSlot,
                        Arrays.asList(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID)
                );
                if (existingOrders != null && !existingOrders.isEmpty()) {
                    // 这里抛异常也可以释放锁！ 因为在finally中会确保锁被释放！！！
                    throw new RuntimeException("当前时段已被预约，请选择其他时段");
                }

                // 创建订单
                Order order = new Order();
                order.setChargerId(chargerId);
                order.setTimeSlot(timeSlot);
                order.setUserId(userId);
                order.setAppointmentTime(appointmentTime);
                order.setStatus(OrderStatus.PENDING_PAYMENT);
                order.setCreateTime(System.currentTimeMillis());
                orderRepository.save(order);

                // **** 重点：订单创建完成就释放锁 ****
                lock.unlock();

                // 发送支付超时延时消息
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", order.getId());
                String messageBody = new ObjectMapper().writeValueAsString(payload);
                MessageProperties props = new MessageProperties();
                props.setHeader("x-delay", PAYMENT_TIMEOUT_MS);
                Message message = new Message(messageBody.getBytes(StandardCharsets.UTF_8), props);
                rabbitTemplate.send(RabbitMQConfig.PAYMENT_TIMEOUT_EXCHANGE, RabbitMQConfig.PAYMENT_TIMEOUT_ROUTING_KEY, message);

                return order;
            } else {
                throw new RuntimeException("预约资源繁忙，请稍后重试");
            }
        } catch (InterruptedException | JsonProcessingException e) {
            throw new RuntimeException("预约失败", e);
        } finally {
            // 保证锁一定被释放，防止上面unlock没执行到（比如 save(order)抛了异常时）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

