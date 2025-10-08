// Merged ALL .kt and .java files from the '_ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:45


// ===== FROM: _ktjava_mirror\libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\libunified_src_main_java_com_mpdc4gsr_libunified_app_bean_event_all.kt =====

// Merged .kt under 'libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event' subtree
// Files: 5; Generated 2025-10-07 23:07:49


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\GalleryDelEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

class GalleryDelEvent


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\PDFEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

class PDFEvent


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\ReportCreateEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

public data class ReportCreateEvent(val name: String = "")


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\TS004ResetEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

data class TS004ResetEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = "reset_requested"
)


// ===== libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\VersionUpData.kt =====

package com.mpdc4gsr.libunified.app.bean.event

data class VersionUpData(
    val versionNo: String,
    val isForcedUpgrade: Boolean,
    val description: String,
    val downPageUrl: String,
    val sizeStr: String,
)