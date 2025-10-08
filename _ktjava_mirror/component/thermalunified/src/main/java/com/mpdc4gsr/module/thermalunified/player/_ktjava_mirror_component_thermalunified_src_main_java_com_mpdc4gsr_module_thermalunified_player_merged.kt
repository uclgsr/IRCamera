// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\player' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:43


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\player\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_player_all.kt =====

// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\player' subtree
// Files: 2; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\player\PlayerFactory.kt =====

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


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\player\SystemPlayerManager.kt =====

package com.mpdc4gsr.module.thermalunified.player

class SystemPlayerManager {
}