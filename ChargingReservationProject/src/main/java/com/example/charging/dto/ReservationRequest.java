package com.example.charging.dto;

public class ReservationRequest {
    private String chargerId;
    private String timeSlot;
    private Long userId;
    // 预约充电开始时间，单位：毫秒
    private long appointmentTime;

    // Getters and Setters
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
}
