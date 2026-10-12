package com.example.catalog.internal

import org.springframework.stereotype.Repository

@Repository
class CatalogRepository {
    fun normalize(sku: String): String = sku.trim().uppercase()
}
