package com.example.charging.service;

import com.example.charging.config.RabbitMQConfig;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RestTemplate restTemplate;

    // 微信支付相关常量（示例）
    public static final String WECHAT_PAY_API = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    public static final String WECHAT_PAY_KEY = "your_wechat_pay_key";

    // 跟 ReservationService 里的延时保持一致 (10分钟)
    private static final long PAYMENT_TIMEOUT_MS = 10 * 60 * 1000;

    /**
     * 调用微信支付接口，生成支付二维码链接
     */
    public String initiateWechatPay(Long orderId) throws Exception {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 幂等或状态检查：如果订单已经是CANCELLED、PAID、FAILED，说明不可再发起支付
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            throw new RuntimeException("该订单已无法支付，当前状态：" + order.getStatus());
        }

        // 1. 计算订单剩余支付时间
        long now = System.currentTimeMillis();
        long orderCreateTime = order.getCreateTime();
        // 原本计算 timeLeft:
        long timeLeft = (orderCreateTime + PAYMENT_TIMEOUT_MS) - now;
        // 比如我们减去 5 秒，这个是5秒缓冲
        long bufferMillis = 5_000;
        timeLeft = Math.max(0, timeLeft - bufferMillis);

        if (timeLeft <= 0) {
            throw new RuntimeException("订单已超过支付时效");
        }

        // 2. 构造微信支付请求参数
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

        // 3. 根据 timeLeft 计算微信的 time_expire
        // 这里非常重要！微信过期
        long expireTimeMillis = now + timeLeft;
        String timeExpire = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date(expireTimeMillis));
        params.put("time_expire", timeExpire);

        // 4. 生成签名
        String sign = WechatPayUtil.generateSign(params, WECHAT_PAY_KEY);
        params.put("sign", sign);

        // 5. 请求微信统一下单接口
        String xmlRequest = XmlUtil.mapToXml(params);
        String xmlResponse = restTemplate.postForObject(WECHAT_PAY_API, xmlRequest, String.class);

        // 6. 解析XML响应，获取支付二维码链接
        Map<String, String> responseMap = XmlUtil.xmlToMap(xmlResponse);
        if ("SUCCESS".equals(responseMap.get("return_code")) && "SUCCESS".equals(responseMap.get("result_code"))) {
            // 返回微信支付二维码链接
            return responseMap.get("code_url");
        } else {
            throw new RuntimeException("微信支付调用失败：" + responseMap.get("return_msg"));
        }
    }

    /**
     * 支付成功后调用：更新订单状态，发送充电启动延时消息
     */
    public void handlePaymentSuccess(Long orderId) {
        // 不变，保留你原先的幂等逻辑即可
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 幂等校验：只有在 PENDING_PAYMENT 时才处理支付成功
        if (!OrderStatus.PENDING_PAYMENT.equals(order.getStatus())) {
            // 如果已是 CANCELLED，说明超时后用户又支付了
            // 可在这里调用退款接口，或记录异常，具体根据业务策略处理
            return;
        }

        order.setStatus(OrderStatus.PAID);
        order.setPayTime(System.currentTimeMillis());
        orderRepository.save(order);

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
        rabbitTemplate.send(RabbitMQConfig.CHARGING_DELAY_EXCHANGE, RabbitMQConfig.CHARGING_DELAY_ROUTING_KEY, message);
    }
}

