//package com.example.notes
//
//import com.example.inventory.InventoryApi
//import org.springframework.stereotype.Service
//
//@Service
//class NotesService(private val inventoryApi: InventoryApi) { // TC-DECL-07
//    fun note(sku: String): String = "$sku: ${inventoryApi.stockOf(sku)}"
//}


package com.example.notes

import com.example.inventory.InventoryApi
import org.springframework.stereotype.Service

@Service
class NotesService(private val inventoryApi: InventoryApi) { // TC-DECL-07
    fun note(sku: String): String = "$sku: ${inventoryApi.stockOf(sku)}"
}