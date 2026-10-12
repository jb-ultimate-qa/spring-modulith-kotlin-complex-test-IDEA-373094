package com.example.catalog.events

import org.springframework.modulith.NamedInterface

@NamedInterface("events") // TC-DECL-17
data class CatalogChanged(val id: String)
