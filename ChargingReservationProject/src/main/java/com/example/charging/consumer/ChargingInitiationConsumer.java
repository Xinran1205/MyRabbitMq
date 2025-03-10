package com.example.charging.consumer;

import com.example.charging.model.Order;
import com.example.charging.model.OrderStatus;
import com.example.charging.repository.OrderRepository;
import com.example.charging.service.MqttService;
import com.example.charging.config.RabbitMQConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class ChargingInitiationConsumer {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MqttService mqttService;

    @RabbitListener(queues = RabbitMQConfig.CHARGING_DELAY_QUEUE)
    public void processChargingInitiation(Message message) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> payload = new ObjectMapper().readValue(json, Map.class);
            Long orderId = Long.valueOf(payload.get("orderId").toString());
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null && order.getStatus().equals(OrderStatus.PAID)) {
                // 通过MQTT通知充电桩启动充电检测
                mqttService.notifyCharger(order.getChargerId(), order.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
