package com.example.pricing.internal

import org.springframework.stereotype.Component

@Component
class PriceTable { // TC-DECL-06b: internal type of an OPEN module
    fun priceOf(sku: String): Long = sku.length * 100L
}
