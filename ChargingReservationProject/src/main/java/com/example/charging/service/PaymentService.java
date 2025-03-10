package com.example.charging.service;

import com.example.charging.model.Order;
import com.example.charging.model.OrderStatus;
import com.example.charging.repository.OrderRepository;
import com.example.charging.util.WechatPayUtil;
import com.example.charging.util.XmlUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RestTemplate restTemplate; // 用于调用微信支付接口

    // 微信支付相关常量（示例）
    private static final String WECHAT_PAY_API = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    private static final String WECHAT_PAY_KEY = "your_wechat_pay_key";

    /**
     * 调用微信支付接口，生成支付二维码链接
     */
    public String initiateWechatPay(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        // 构造微信支付请求参数（伪代码示例）
        Map<String, String> params = new HashMap<>();
        params.put("appid", "your_app_id");
        params.put("mch_id", "your_merchant_id");
        params.put("nonce_str", UUID.randomUUID().toString().replace("-", ""));
        params.put("body", "预约充电订单");
        params.put("out_trade_no", order.getId().toString());
        params.put("total_fee", "金额（单位分）");
        params.put("spbill_create_ip", "服务器IP");
        params.put("notify_url", "https://yourdomain.com/api/payment/callback");
        params.put("trade_type", "NATIVE"); // 扫码支付
        // 生成签名（调用你们的签名工具方法）
        String sign = WechatPayUtil.generateSign(params, WECHAT_PAY_KEY);
        params.put("sign", sign);

        // 将参数转换为XML格式（可使用工具类实现）
        String xmlRequest = XmlUtil.mapToXml(params);
        // 调用微信支付接口
        String xmlResponse = restTemplate.postForObject(WECHAT_PAY_API, xmlRequest, String.class);
        // 解析XML响应，获取支付二维码链接
        Map<String, String> responseMap = XmlUtil.xmlToMap(xmlResponse);
        if ("SUCCESS".equals(responseMap.get("return_code")) && "SUCCESS".equals(responseMap.get("result_code"))) {
            return responseMap.get("code_url"); // 支付二维码链接
        } else {
            throw new RuntimeException("微信支付调用失败：" + responseMap.get("return_msg"));
        }
    }

    /**
     * 支付成功后调用：更新订单状态，释放锁，并投递充电启动消息
     */
    public void handlePaymentSuccess(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        order.setStatus(OrderStatus.PAID);
        order.setPayTime(System.currentTimeMillis());
        orderRepository.save(order);

        // 释放预约充电锁
        String lockKey = "charging:lock:" + order.getChargerId() + ":" + order.getTimeSlot();
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }

        // 推送充电启动延时消息
        long delay = Math.max(0, order.getAppointmentTime() - System.currentTimeMillis());
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", order.getId());
        String messageBody;
        try {
            messageBody = new ObjectMapper().writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("消息构造失败", e);
        }
        MessageProperties props = new MessageProperties();
        props.setHeader("x-delay", delay);
        Message message = new Message(messageBody.getBytes(StandardCharsets.UTF_8), props);
        // 充电启动消息队列
        rabbitTemplate.send("charging.start.exchange", "charging.start.routing", message);
    }
}

