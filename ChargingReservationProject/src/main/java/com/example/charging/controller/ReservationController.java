package com.example.charging.controller;

import com.example.charging.dto.ReservationRequest;
import com.example.charging.model.Order;
import com.example.charging.service.PaymentService;
import com.example.charging.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 顶层 Controller，用于处理预约充电及支付请求
 */
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PaymentService paymentService;

    /**
     * 预约充电接口：锁定资源、创建订单，并投递支付超时延时消息
     */
    @PostMapping("/reserve")
    public ResponseEntity<Order> reserveCharging(@RequestBody ReservationRequest request) {
        Order order = reservationService.reserveCharging(
                request.getChargerId(),
                request.getTimeSlot(),
                request.getUserId(),
                request.getAppointmentTime()
        );
        return ResponseEntity.ok(order);
    }

    // 上面这个业务结束后，对于以下接口，前端允许的时间应该在10分钟内，页面失效！
    /**
     * 微信支付发起接口：返回支付二维码链接或支付 URL
     */
    @PostMapping("/pay")
    public ResponseEntity<String> initiateWechatPay(@RequestParam Long orderId) throws Exception {
        String qrCodeUrl = paymentService.initiateWechatPay(orderId);
        return ResponseEntity.ok(qrCodeUrl);
    }
}
