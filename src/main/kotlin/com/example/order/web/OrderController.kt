package com.example.order.web

import com.example.catalog.CatalogService
import com.example.order.api.OrderApiFacade
import com.example.order.internal.OrderRepository
import com.example.order.web.internal.OrderWebInternal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderRepository: OrderRepository, // TC-NEST-02
    private val catalogService: CatalogService, // TC-NEST-08
    private val orderApiFacade: OrderApiFacade, // TC-NEST-05
    private val webInternal: OrderWebInternal,
) {

    @GetMapping("/{orderId}")
    fun order(@PathVariable orderId: String): String =
        listOf(
            orderRepository.statusOf(orderId),
            catalogService.name(),
            orderApiFacade.stockOf(orderId),
            webInternal.render(),
        ).joinToString()
}
