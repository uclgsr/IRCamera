package com.guide.zm04c.matrix.utils;

/**
 * HexDump utility with minimal implementation
 * Contains only the essential methods referenced by GuideUsbManager
 */
public class HexDump {
    
    public static String dumpHexString(byte[] array) {
        return dumpHexString(array, 0, array.length);
    }
    
    public static String dumpHexString(byte[] array, int offset, int length) {
        StringBuilder result = new StringBuilder();
        
        byte[] line = new byte[16];
        int lineIndex = 0;
        
        for (int i = offset; i < offset + length; i++) {
            if (lineIndex == 16) {
                result.append(formatHexLine(line, lineIndex));
                result.append('\n');
                lineIndex = 0;
            }
            
            byte b = array[i];
            line[lineIndex++] = b;
        }
        
        if (lineIndex > 0) {
            result.append(formatHexLine(line, lineIndex));
        }
        
        return result.toString();
    }
    
    private static String formatHexLine(byte[] data, int length) {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int b = data[i] & 0xFF;
            result.append(String.format("%02X ", b));
        }
        
        // Pad remaining space if line is not full
        for (int i = length; i < 16; i++) {
            result.append("   ");
        }
        
        result.append(" ");
        
        // Add ASCII representation
        for (int i = 0; i < length; i++) {
            char c = (char) (data[i] & 0xFF);
            if (c >= 32 && c <= 126) {
                result.append(c);
            } else {
                result.append('.');
            }
        }
        
        return result.toString();
    }
}