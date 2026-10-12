package com.example.shared.internal

import org.springframework.stereotype.Component

@Component
class SharedInternal { // TC-DECL-06: reachable from everywhere because the module is OPEN
    fun currency(): String = "EUR"
}
