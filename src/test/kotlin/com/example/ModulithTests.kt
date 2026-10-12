package com.example

import com.tngtech.archunit.core.domain.JavaClass
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import kotlin.test.assertTrue

class ModulithTests {

    private val modules = ApplicationModules.of(
        Application::class.java,
        JavaClass.Predicates.resideInAPackage("com.example.reporting"),
    )

    @Test
    fun `structure is detected`() {
        modules.forEach(::println)
        val ids = modules.map { it.identifier.toString() }
        listOf("order", "order.web", "order.web.details", "order.api", "inventory", "catalog", "shared",
               "billing", "pricing", "notes", "other-billing").forEach { assertTrue(it in ids, "missing module $it") }
    }

    @Test
    fun `verification passes without reporting`() {
        modules.verify()
    }

    @Test
    fun `reporting violates its allowed dependencies`() {
        val all = ApplicationModules.of(Application::class.java)
        val violations = runCatching { all.verify() }.exceptionOrNull()
        println(violations?.message)
        assertTrue(violations != null && "reporting" in violations.message.orEmpty())
    }
}
