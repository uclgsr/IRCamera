package com.mpdc4gsr.commons.base.entity

import android.content.Context
import android.content.ContextWrapper
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import java.io.File
import java.util.Objects

class DatabaseContext(base: Context?, dbDir: File) : ContextWrapper(base) {
    private val dbDir: File

    init {
        Objects.requireNonNull<File?>(dbDir, "dbDir is null")
        this.dbDir = dbDir
    }

    override fun getDatabasePath(name: String): File {
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }
        return File(dbDir, name)
    }

    override fun openOrCreateDatabase(
        name: String,
        mode: Int,
        factory: CursorFactory?,
        errorHandler: DatabaseErrorHandler?
    ): SQLiteDatabase? {
        return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory)
    }

    override fun openOrCreateDatabase(name: String, mode: Int, factory: CursorFactory?): SQLiteDatabase? {
        return super.openOrCreateDatabase(getDatabasePath(name).getName(), mode, factory)
    }
}
