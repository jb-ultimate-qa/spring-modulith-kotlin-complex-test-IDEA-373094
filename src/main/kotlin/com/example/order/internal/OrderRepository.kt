package com.example.order.internal

import org.springframework.stereotype.Repository

@Repository
class OrderRepository { // TC-API-01 … TC-API-22 target

    fun statusOf(orderId: String): String = "OPEN:$orderId"

    companion object {
        fun create(): OrderRepository = OrderRepository() // TC-API-09, TC-API-16
    }
}
