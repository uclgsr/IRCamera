package com.mpdc4gsr.component.thermal.event

import com.mpdc4gsr.component.shared.app.repository.GalleryRepository.DirType

data class GalleryDirChangeEvent(
    val dirType: DirType,
)



