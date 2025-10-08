// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\model' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\model\GradientColor.java =====

package com.mpdc4gsr.libunified.ui.model;

public class GradientColor {

    private int startColor;
    private int endColor;

    public GradientColor(int startColor, int endColor) {
        this.startColor = startColor;
        this.endColor = endColor;
    }

    public int getStartColor() {
        return startColor;
    }

    public void setStartColor(int startColor) {
        this.startColor = startColor;
    }

    public int getEndColor() {
        return endColor;
    }

    public void setEndColor(int endColor) {
        this.endColor = endColor;
    }
}