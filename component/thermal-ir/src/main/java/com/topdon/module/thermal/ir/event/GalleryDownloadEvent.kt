package com.topdon.module.thermal.ir.event

/**
// 有一张 TS004 thermal imaging图片或视频从远端下载完毕事件.
// @param filename 下载successful的文件名，如 xxx.jpg
 */
/**
 * Gallery download event for thermal imaging system communication.
 * Facilitates decoupled component interaction.
 */
data class GalleryDownloadEvent(val filename: String)
