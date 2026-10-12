package com.example.reporting

import com.example.catalog.CatalogService
import com.example.order.internal.OrderRepository
import org.springframework.stereotype.Service

@Service
class ReportingService(
    private val orderRepository: OrderRepository, // TC-SRC-01: ILLEGAL at runtime (closed module, not allowed)
    private val catalogService: CatalogService, // TC-SRC-01: ILLEGAL at runtime (exposed type, but only "inventory" is allowed)
) {
    fun report(orderId: String): String = "${orderRepository.statusOf(orderId)} ${catalogService.name()}"
}
