package com.example.charging.consumer;

import com.example.charging.model.Order;
import com.example.charging.repository.OrderRepository;
import com.example.charging.config.RabbitMQConfig;
import com.example.charging.model.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class PaymentTimeoutConsumer {

    @Autowired
    private OrderRepository orderRepository;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_TIMEOUT_QUEUE)
    public void processTimeout(Message message) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> payload = new ObjectMapper().readValue(json, Map.class);
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null && order.getStatus().equals(OrderStatus.PENDING_PAYMENT)) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelTime(System.currentTimeMillis());
                orderRepository.save(order);
                System.out.println("订单 " + orderId + " 超时未支付，已取消。");

                // 然后这里告诉微信支付平台取消订单（伪代码示例）
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
