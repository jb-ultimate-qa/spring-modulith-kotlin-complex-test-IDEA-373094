package com.example.order

import com.example.catalog.api.CatalogApi
import com.example.shared.SharedService
import com.example.shared.internal.SharedInternal
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val catalogApi: CatalogApi, // TC-DEP-02
    private val sharedService: SharedService, // TC-DEP-04
    private val sharedInternal: SharedInternal, // TC-DECL-06
    private val events: ApplicationEventPublisher,
) {

    fun complete(sku: String): OrderCompleted {
        val event = OrderCompleted(newOrderId())
        sharedService.audit("order ${event.orderId}: $sku costs ${catalogApi.priceOf(sku)} ${sharedInternal.currency()}")
        events.publishEvent(event)
        return event
    }
}
