package com.example.charging.model;

import javax.persistence.*;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String chargerId;
    private String timeSlot;
    private Long userId;
    // 预约充电开始时间（毫秒）
    private long appointmentTime;
    // 订单状态：PENDING_PAYMENT, PAID, CANCELLED, FAILED
    private String status;
    private Long createTime;
    private Long payTime;
    private Long cancelTime;

    // Getters and Setters
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getChargerId() {
        return chargerId;
    }
    public void setChargerId(String chargerId) {
        this.chargerId = chargerId;
    }
    public String getTimeSlot() {
        return timeSlot;
    }
    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public long getAppointmentTime() {
        return appointmentTime;
    }
    public void setAppointmentTime(long appointmentTime) {
        this.appointmentTime = appointmentTime;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Long getCreateTime() {
        return createTime;
    }
    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
    public Long getPayTime() {
        return payTime;
    }
    public void setPayTime(Long payTime) {
        this.payTime = payTime;
    }
    public Long getCancelTime() {
        return cancelTime;
    }
    public void setCancelTime(Long cancelTime) {
        this.cancelTime = cancelTime;
    }
}
