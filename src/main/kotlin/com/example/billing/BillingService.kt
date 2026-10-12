package com.example.billing

import org.springframework.stereotype.Service

@Service
class BillingService { // TC-DEP-05, TC-VIEW-07: no cross-module reference in the committed state
    fun invoice(orderId: String): String = "invoice for $orderId"
}
