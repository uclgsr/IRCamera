package com.ircamera.shared.utils;

/**
 * Consolidated HexDump utility combining functionality from multiple modules.
 * This replaces the duplicate HexDump implementations found in:
 * - libir/src/main/java/com/infisense/usbir/utils/HexDump.java (178 lines)
 * - libapp/src/main/java/com/guide/zm04c/matrix/utils/HexDump.java (63 lines) 
 * - libapp/src/main/java/com/mpdc4gsr/lib/core/matrix/utils/HexDump.java (149 lines)
 * - component/thermal-ir/.../HexDump.java (179 lines)
 * 
 * Provides all functionality while maintaining backward compatibility.
 */
public class HexDump {
    private final static char[] HEX_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private final static char[] HEX_LOWER_CASE_DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * Basic hex string dump - minimal implementation
     */
    public static String dumpHexString(byte[] array) {
        if (array == null) return "(null)";
        return dumpHexString(array, 0, array.length);
    }

    /**
     * Hex string dump with offset and length
     */
    public static String dumpHexString(byte[] array, int offset, int length) {
        if (array == null) return "(null)";
        StringBuilder result = new StringBuilder();

        byte[] line = new byte[16];
        int lineIndex = 0;

        result.append("\n0x");
        result.append(toHexString(offset));

        for (int i = offset; i < offset + length; i++) {
            if (lineIndex == 16) {
                result.append(" ");

                for (int j = 0; j < 16; j++) {
                    if (line[j] > ' ' && line[j] < '~') {
                        result.append(new String(line, j, 1));
                    } else {
                        result.append(".");
                    }
                }

                result.append("\n0x");
                result.append(toHexString(i));
                lineIndex = 0;
            }

            byte b = array[i];
            result.append(" ");
            result.append(HEX_DIGITS[(b >> 4) & 0x0F]);
            result.append(HEX_DIGITS[b & 0x0F]);

            line[lineIndex++] = b;
        }

        if (lineIndex > 0) {
            int count = (16 - lineIndex) * 3;
            count++;
            for (int i = 0; i < count; i++) {
                result.append(" ");
            }

            for (int i = 0; i < lineIndex; i++) {
                if (line[i] > ' ' && line[i] < '~') {
                    result.append(new String(line, i, 1));
                } else {
                    result.append(".");
                }
            }
        }

        return result.toString();
    }

    /**
     * Convert byte array to hex string (simple format)
     */
    public static String toHexString(byte[] ba) {
        if (ba == null) return "(null)";
        return toHexString(ba, 0, ba.length);
    }

    /**
     * Convert byte array to hex string with offset and length
     */
    public static String toHexString(byte[] ba, int offset, int length) {
        if (ba == null) return "(null)";
        StringBuilder sb = new StringBuilder(length * 2);
        for (int i = offset; i < offset + length; i++) {
            sb.append(String.format("%02x", ba[i] & 0xff));
        }
        return sb.toString();
    }

    /**
     * Convert integer to hex string
     */
    public static String toHexString(int i) {
        return Integer.toHexString(i);
    }

    /**
     * Convert byte to hex string
     */
    public static String toHexString(byte b) {
        return String.format("%02x", b & 0xff);
    }

    /**
     * Format a line of hex data with ASCII representation
     */
    private static String formatHexLine(byte[] line, int lineLength) {
        StringBuilder result = new StringBuilder();
        
        // Hex portion
        for (int i = 0; i < 16; i++) {
            if (i < lineLength) {
                result.append(String.format("%02x ", line[i] & 0xff));
            } else {
                result.append("   ");
            }
        }
        
        result.append(" ");
        
        // ASCII portion
        for (int i = 0; i < lineLength; i++) {
            if (line[i] > ' ' && line[i] < '~') {
                result.append((char) line[i]);
            } else {
                result.append(".");
            }
        }
        
        return result.toString();
    }
}