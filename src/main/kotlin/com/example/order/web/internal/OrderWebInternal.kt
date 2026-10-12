package com.example.order.web.internal

import org.springframework.stereotype.Component

@Component
class OrderWebInternal { // NC-NEST-01, NC-NEST-04 target
    fun render(): String = "web"
}
