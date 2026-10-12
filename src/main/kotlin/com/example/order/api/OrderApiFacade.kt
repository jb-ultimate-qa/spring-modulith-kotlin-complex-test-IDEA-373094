package com.example.order.api

import com.example.inventory.InventoryApi
import com.example.order.api.internal.ApiSecret
import org.springframework.stereotype.Service

@Service
class OrderApiFacade(
    private val inventoryApi: InventoryApi, // TC-DEP-01, TC-NEST-07
    private val apiSecret: ApiSecret,
) {
    fun stockOf(sku: String): String = "${inventoryApi.stockOf(sku)} ${apiSecret.token()}"
}
