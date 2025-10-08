// Merged ALL .kt and .java files from the '_ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:43


// ===== FROM: _ktjava_mirror\component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event\component_thermalunified_src_main_java_com_mpdc4gsr_module_thermalunified_event_all.kt =====

// Merged .kt under 'component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event' subtree
// Files: 7; Generated 2025-10-07 23:07:44


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event\GalleryAddEvent.kt =====

package com.mpdc4gsr.module.thermalunified.event

class GalleryAddEvent


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event\GalleryDirChangeEvent.kt =====

package com.mpdc4gsr.module.thermalunified.event

import com.mpdc4gsr.libunified.app.repository.GalleryRepository.DirType

data class GalleryDirChangeEvent(val dirType: DirType)


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event\GalleryDownloadEvent.kt =====

package com.mpdc4gsr.module.thermalunified.event

data class GalleryDownloadEvent(val filename: String)


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event\ImageGalleryEvent.kt =====

package com.mpdc4gsr.module.thermalunified.event

class ImageGalleryEvent


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event\ManualFinishBean.kt =====

package com.mpdc4gsr.module.thermalunified.event

class ManualFinishBean


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event\MonitorCreateEvent.kt =====

package com.mpdc4gsr.module.thermalunified.event

class MonitorCreateEvent


// ===== component\thermalunified\src\main\java\com\mpdc4gsr\module\thermalunified\event\MonitorSaveEvent.kt =====

package com.mpdc4gsr.module.thermalunified.event

class MonitorSaveEvent