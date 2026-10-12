package com.example.order

import java.util.UUID

fun newOrderId(): String = UUID.randomUUID().toString() // TC-DECL-18
