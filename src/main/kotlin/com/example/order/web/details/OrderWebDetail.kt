package com.example.order.web.details

import com.example.order.internal.OrderRepository
import org.springframework.stereotype.Component

@Component
class OrderWebDetail(
    private val orderRepository: OrderRepository, // TC-NEST-03
) {
    fun detail(orderId: String): String = orderRepository.statusOf(orderId)
}
