package com.example.inventory

import com.example.inventory.internal.InventoryRepository
import com.example.order.OrderCompleted
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class InventoryEvents(private val repository: InventoryRepository) { // TC-EVT-01

    @ApplicationModuleListener // TC-EVT-01
    fun on(event: OrderCompleted) { // TC-EVT-02
        repository.reserve(event.orderId)
    }
}
