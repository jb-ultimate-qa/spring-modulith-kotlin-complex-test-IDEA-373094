package com.example.inventory

interface InventoryApi { // TC-DEP-01, TC-DEP-05 target
    fun stockOf(sku: String): Int
}
