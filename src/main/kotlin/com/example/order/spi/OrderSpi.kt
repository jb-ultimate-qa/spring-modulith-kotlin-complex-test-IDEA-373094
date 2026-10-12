package com.example.order.spi

interface OrderSpi { // TC-DECL-09
    fun onOrder(orderId: String)
}
