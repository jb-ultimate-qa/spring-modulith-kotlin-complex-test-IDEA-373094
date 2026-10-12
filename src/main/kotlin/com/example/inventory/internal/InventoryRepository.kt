package com.example.inventory.internal

import com.example.inventory.InventoryApi
import org.springframework.stereotype.Repository

@Repository
class InventoryRepository : InventoryApi {

    private val reserved = mutableListOf<String>()

    override fun stockOf(sku: String): Int = levelOf(sku).qty

    fun reserve(orderId: String) {
        reserved += orderId
    }

    private fun levelOf(sku: String) = StockLevel(sku, 10 - reserved.size)
}
