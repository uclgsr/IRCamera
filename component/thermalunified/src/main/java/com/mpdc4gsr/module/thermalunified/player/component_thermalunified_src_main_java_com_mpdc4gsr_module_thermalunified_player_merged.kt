// Merged ALL .kt and .java files from the 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\player' directory and its subdirectories.
// Total files: 2 | Generated on: 2025-10-08 01:42:35


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\player\PlayerFactory.kt =====

package com.mpdc4gsr.module.thermalunified.player

object PlayerFactory {
    const val SYSTEM_PLAYER = 0
    const val IJK_PLAYER = 1
    const val EXO_PLAYER = 2

    @JvmStatic
    fun setPlayManager(playerType: Int) {
    }

    @JvmStatic
    fun setPlayManager(clazz: Class<*>) {
    }
}


// ===== FROM: component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\player\SystemPlayerManager.kt =====

package com.mpdc4gsr.module.thermalunified.player

class SystemPlayerManager {
}