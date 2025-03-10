package com.example.charging.repository;

import com.example.charging.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // 根据需要可以添加自定义查询方法
}
