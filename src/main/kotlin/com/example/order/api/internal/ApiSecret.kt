package com.example.order.api.internal

import org.springframework.stereotype.Component

@Component
class ApiSecret { // NC-NEST-03 target
    fun token(): String = "secret"
}
