// Merged ALL .kt and .java files from the 'libunified\src\main\java\com\mpdc4gsr\libunified\ui\exception' directory and its subdirectories.
// Total files: 1 | Generated on: 2025-10-08 01:42:39


// ===== FROM: libunified\src\main\java\com\mpdc4gsr\libunified\ui\exception\DrawingDataSetNotCreatedException.java =====

package com.mpdc4gsr.libunified.ui.exception;

public class DrawingDataSetNotCreatedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DrawingDataSetNotCreatedException() {
        super("Have to create a new drawing set first. Call ChartData's createNewDrawingDataSet() method");
    }

}