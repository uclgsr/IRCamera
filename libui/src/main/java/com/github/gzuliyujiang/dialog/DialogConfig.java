package com.github.gzuliyujiang.dialog;

public final class DialogConfig {
    private static int dialogStyle = DialogStyle.Default;
    private static DialogColor dialogColor = new DialogColor();

    private DialogConfig() {
        super();
    }

    @DialogStyle
    public static int getDialogStyle() {
        return dialogStyle;
    }

    public static void setDialogStyle(@DialogStyle int style) {
        dialogStyle = style;
    }

    public static DialogColor getDialogColor() {
        if (dialogColor == null) {
            dialogColor = new DialogColor();
        }
        return dialogColor;
    }

    public static void setDialogColor(DialogColor color) {
        dialogColor = color;
    }

}
