package com.example

import com.tngtech.archunit.core.domain.JavaClass
import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules

/**
 * Prints the runtime model and every violation without failing, so one run shows the ground truth for a temporary
 * snippet from IDEA-373094-TEST-CASES.md. Run it from the IDE, or `./gradlew test --tests com.example.RuntimeProbe`
 * and read build/test-results/test/TEST-com.example.RuntimeProbe.xml.
 *
 * `allowed=none` means "nothing declared" (unrestricted); a declared list always contains the shared module `shared`.
 */
class RuntimeProbe {

    @Test
    fun `prints model and violations`() {
        report("without reporting") {
            ApplicationModules.of(Application::class.java, JavaClass.Predicates.resideInAPackage("com.example.reporting"))
        }
        report("full") { ApplicationModules.of(Application::class.java) }
    }

    private fun report(title: String, factory: () -> ApplicationModules) {
        println("=== $title")
        runCatching {
            val modules = factory()
            modules.forEach { module ->
                val allowed = runCatching { module.getAllowedDependencies(modules).toString() }
                    .getOrElse { "ERROR ${it.javaClass.simpleName}: ${it.message}" }
                val parent = modules.getParentOf(module).map { it.identifier.toString() }.orElse("-")
                println("MODULE ${module.identifier} | ${module.displayName} | ${module.basePackage.name} | parent=$parent | open=${module.isOpen} | allowed=$allowed")
                module.namedInterfaces.forEach { ni -> println("  NI ${ni.name}: ${ni.map { it.simpleName }.sorted()}") }
            }
            val violations = modules.detectViolations()
            println("VIOLATIONS:\n" + if (violations.hasViolations()) violations.message else "(none)")
        }.exceptionOrNull()?.let { println("EXCEPTION ${it.javaClass.name}: ${it.message}") }
    }
}
