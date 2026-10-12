package com.example.order.internal

annotation class OrderTag // TC-API-10

sealed interface OrderState { // TC-API-14
    data object New : OrderState
    data class Paid(val amount: Long) : OrderState
}

enum class OrderKind { SINGLE, BULK } // TC-API-14
