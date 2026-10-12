package com.example.notes

import org.springframework.modulith.ApplicationModule

// TC-DECL-07: no @PackageInfo. The runtime still reads it: @ApplicationModule is itself meta-annotated with
// @PackageInfo (since Modulith 1.2), so this type is a carrier and "notes" allows nothing.
@ApplicationModule(displayName = "Notes (bare carrier)", allowedDependencies = []) // TC-DECL-07
class ModuleMetadata
