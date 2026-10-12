package com.other.billing

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

// TC-SRC-13: without the explicit id this module is also "billing", and ApplicationModules.of() fails with
// "IllegalStateException: Duplicate key billing".
@ApplicationModule(id = "other-billing", displayName = "Other Billing") // TC-SRC-12, TC-VIEW-02
@PackageInfo
class ModuleMetadata
