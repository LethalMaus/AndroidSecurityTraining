package dev.jamescullimore.android_security_training.perm

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle

class DemoProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        // No real storage; return a single-row MatrixCursor with a greeting
        val c = MatrixCursor(arrayOf("value"))
        c.addRow(arrayOf("hello from DemoProvider: ${uri.path}"))
        return c
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.item/vnd.demo.value"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return Bundle().apply { putString("result", "method=$method") }
    }
}
