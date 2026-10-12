package com.example.catalog

import com.example.ModuleIds
import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(id = ModuleIds.CATALOG, displayName = "Catalog") // TC-DECL-02, TC-DECL-03
@PackageInfo
object ModuleMetadata // TC-DECL-04
