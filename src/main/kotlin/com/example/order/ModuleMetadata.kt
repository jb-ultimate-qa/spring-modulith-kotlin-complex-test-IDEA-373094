package com.example.order

import com.example.ModuleIds
import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(
    displayName = "Order Module", // TC-DECL-02
    allowedDependencies = [ModuleIds.INVENTORY, "catalog :: api", "shared :: *"], // TC-DEP-01, TC-DEP-02, TC-DEP-04
)
@PackageInfo
class ModuleMetadata // TC-DECL-01
