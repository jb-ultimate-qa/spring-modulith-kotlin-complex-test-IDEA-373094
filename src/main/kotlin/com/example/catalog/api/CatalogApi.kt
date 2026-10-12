package com.example.catalog.api

interface CatalogApi { // TC-DEP-02, TC-NEST-07 target
    fun priceOf(sku: String): Long
}
