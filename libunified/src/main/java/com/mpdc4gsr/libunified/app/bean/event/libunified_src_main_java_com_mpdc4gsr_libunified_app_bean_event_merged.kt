// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event' directory and its subdirectories.
// Total files: 5 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\GalleryDelEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

class GalleryDelEvent


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\PDFEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

class PDFEvent


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\ReportCreateEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

public data class ReportCreateEvent(val name: String = "")


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\TS004ResetEvent.kt =====

package com.mpdc4gsr.libunified.app.bean.event

data class TS004ResetEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val reason: String = "reset_requested"
)


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\app\bean\event\VersionUpData.kt =====

package com.mpdc4gsr.libunified.app.bean.event

data class VersionUpData(
    val versionNo: String,
    val isForcedUpgrade: Boolean,
    val description: String,
    val downPageUrl: String,
    val sizeStr: String,
)