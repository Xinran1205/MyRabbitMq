package com.example.charging.service;

import org.springframework.stereotype.Service;

/**
 * 模拟 MQTT 服务，用于通知充电桩/墙盒启动充电检测
 */
@Service
public class MqttService {

    public void notifyCharger(String chargerId, Long orderId) {
        // 模拟通过MQTT向充电桩发送通知
        System.out.println("MQTT通知：充电桩 " + chargerId + " 开始处理订单 " + orderId + " 的充电请求。");
        // 实际实现中，可调用MQTT客户端接口
    }
}
