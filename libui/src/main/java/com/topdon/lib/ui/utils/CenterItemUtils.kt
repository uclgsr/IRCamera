package com.topdon.lib.ui.utils

/**
 * @author: CaiSongL
 * @date: 2023/3/31 9:56
 */
internal object CenterItemUtils {
    fun getMinDifferItem(itemHeights: List<CenterViewItem>): CenterViewItem {
        var minItem = itemHeights[0] 
        for (i in itemHeights.indices) {
            // 遍历Get/Retrieve最小差值
            if (itemHeights[i].differ <= minItem.differ) {
                minItem = itemHeights[i]
            }
        }
        return minItem
    }

    
/**
 * Custom Center view item view for thermal imaging display.
 * Provides specialized rendering and interaction capabilities.
 */
/**
 * CenterViewItem implements custom user interface component functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
    class CenterViewItem
    
    
    (var position: Int, var differ: Int)
}
