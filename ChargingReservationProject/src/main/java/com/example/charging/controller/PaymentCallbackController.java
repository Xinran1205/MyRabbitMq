package com.example.charging.controller;

import com.example.charging.model.Order;
import com.example.charging.model.OrderStatus;
import com.example.charging.repository.OrderRepository;
import com.example.charging.service.PaymentService;
import com.example.charging.util.XmlUtil;
import com.example.charging.util.WechatPayUtil;
import org.redisson.api.RedissonClient;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentCallbackController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    /**
     * 微信支付回调接口（POST请求，XML格式）
     */
    @PostMapping("/callback")
    public String paymentCallback(@RequestBody String xmlData) {
        try {
            Map<String, String> callbackData = XmlUtil.xmlToMap(xmlData);
            // 验证签名
            if (!WechatPayUtil.validateSign(callbackData, WechatPayUtil.WECHAT_PAY_KEY)) {
                return generateWxResponse("FAIL", "签名验证失败");
            }
            if ("SUCCESS".equals(callbackData.get("return_code")) &&
                    "SUCCESS".equals(callbackData.get("result_code"))) {
                Long orderId = Long.valueOf(callbackData.get("out_trade_no"));
                paymentService.handlePaymentSuccess(orderId);
                return generateWxResponse("SUCCESS", "OK");
            } else {
                // 支付失败处理
                Long orderId = Long.valueOf(callbackData.get("out_trade_no"));
                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null && order.getStatus().equals(OrderStatus.PENDING_PAYMENT)) {
                    order.setStatus(OrderStatus.FAILED);
                    orderRepository.save(order);
                }
                return generateWxResponse("SUCCESS", "OK");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return generateWxResponse("FAIL", "处理异常");
        }
    }

    private String generateWxResponse(String returnCode, String returnMsg) {
        return "<xml><return_code><![CDATA[" + returnCode + "]]></return_code>" +
                "<return_msg><![CDATA[" + returnMsg + "]]></return_msg></xml>";
    }
}

