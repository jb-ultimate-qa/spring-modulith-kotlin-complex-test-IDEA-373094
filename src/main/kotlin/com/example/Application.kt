package com.example

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.modulith.Modulithic

@Modulithic(
    systemName = "Kotlin Modulith",
    sharedModules = ["shared"],
    additionalPackages = ["com.other"], // TC-SRC-12
)
@SpringBootApplication
class Application // TC-VIEW-05

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
