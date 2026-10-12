package com.example.notes

import org.springframework.stereotype.Service

@Service
class NotesService { // TC-DECL-07: the temporary snippet adds an InventoryApi dependency here
    fun note(sku: String): String = "note for $sku"
}
