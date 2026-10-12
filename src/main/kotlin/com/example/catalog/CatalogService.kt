package com.example.catalog

import com.example.catalog.api.CatalogApi
import com.example.catalog.internal.CatalogRepository
import com.example.pricing.internal.PriceTable
import org.springframework.stereotype.Service

@Service
class CatalogService( // TC-DEP-03, TC-NEST-08 target
    private val repository: CatalogRepository,
    private val priceTable: PriceTable, // TC-DECL-06b
) : CatalogApi {

    override fun priceOf(sku: String): Long = priceTable.priceOf(repository.normalize(sku))

    fun name(): String = "catalog"
}
