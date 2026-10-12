package com.example.shared

import org.springframework.stereotype.Service

@Service
class SharedService { // TC-DEP-04 target
    fun audit(message: String) = println(message)
}
