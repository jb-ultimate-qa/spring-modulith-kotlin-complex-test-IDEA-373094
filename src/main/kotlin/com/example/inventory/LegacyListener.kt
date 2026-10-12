package com.example.inventory

import com.example.order.OrderCompleted
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionalEventListener

@Component
class LegacyListener {

    @Async
    @Transactional(readOnly = true)
    @TransactionalEventListener(id = "legacy")
    fun on(event: OrderCompleted) { // TC-EVT-04, TC-QF-05
        println("legacy listener: ${event.orderId}")
    }
}
