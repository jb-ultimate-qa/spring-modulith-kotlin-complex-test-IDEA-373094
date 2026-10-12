package com.example.order.api

import com.example.ModuleIds
import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(allowedDependencies = [ModuleIds.INVENTORY]) // TC-NEST-07, TC-DEP-16
@PackageInfo
class ModuleMetadata
