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


