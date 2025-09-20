package com.mpdc4gsr.commons.util


object PDFUtils {
    fun getPdfName(name: String): String {
        var name = name
        name = name.replace('+', '-')
        name = name.replace(' ', '-')
        name = name.replace('/', '-')
        name = name.replace('?', '-')
        name = name.replace('%', '-')
        name = name.replace('#', '-')
        name = name.replace('&', '-')
        name = name.replace('=', '-')
        name = name.replace('\\', '-')
        name = name.replace(':', '-')
        name = name.replace('*', '-')
        name = name.replace('|', '-')
        name = name.replace('<', '-')
        name = name.replace('>', '-')
        name = name.replace('"', '-')
        return name
    }
}
