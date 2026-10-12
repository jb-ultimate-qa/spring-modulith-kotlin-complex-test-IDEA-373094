package com.example.order.internal

fun internalHelper(): String = "helper" // TC-API-12

val someProperty: String = "property" // TC-API-13 (not const: a const val is inlined and leaves no reference)

fun String.internalExt(): String = "$this!" // TC-API-13

object OrderInternalObject { // TC-API-08
    fun describe(): String = "internal object"
}

typealias RepositoryAlias = OrderRepository // TC-API-11

interface OrderCallback { // TC-API-07, TC-API-15
    fun onOrder(orderId: String)
}
