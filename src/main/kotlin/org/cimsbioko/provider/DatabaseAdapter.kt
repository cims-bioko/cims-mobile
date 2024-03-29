package org.cimsbioko.provider

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.cimsbioko.App
import org.cimsbioko.data.DataWrapper
import org.cimsbioko.model.FormInstance
import org.cimsbioko.utilities.FormsHelper
import org.cimsbioko.utilities.SQLITE_MAX_HOST_PARAMETERS
import org.cimsbioko.utilities.SQLUtils.makePlaceholders

object DatabaseAdapter {

    private val helper: DatabaseHelper = DatabaseHelper(App.instance.applicationContext)

    fun attachFormToHierarchy(hierarchyPath: String, formId: Long): Long {
        with(helper.writableDatabase) {
            beginTransaction()
            findHierarchyForForm(formId)?.let { detachFromHierarchy(listOf(formId)) }
            return try {
                replaceOrThrow(FORM_PATH_TABLE_NAME, null, ContentValues().apply {
                    put(KEY_HIER_PATH, hierarchyPath)
                    put(KEY_FORM_ID, formId.toString())
                }).also { setTransactionSuccessful() }
            } finally {
                endTransaction()
            }
        }
    }

    fun findHierarchyForForm(id: Long?): String? {
        return id?.let {
            val columns = arrayOf(KEY_HIER_PATH)
            val where = String.format("%s = ?", KEY_FORM_ID)
            val whereArgs = arrayOf(id.toString())
            helper.readableDatabase.query(FORM_PATH_TABLE_NAME, columns, where, whereArgs, null, null, null)?.use {
                with(it) {
                    if (moveToNext()) {
                        getString(getColumnIndex(KEY_HIER_PATH))
                    } else null
                }
            }
        }
    }

    fun findFormsForHierarchy(hierarchyPath: String): Flow<List<FormInstance>> = flow {
        val columns = arrayOf(KEY_FORM_ID)
        val where = String.format("%s = ?", KEY_HIER_PATH)
        val whereArgs = arrayOf(hierarchyPath)
        helper.readableDatabase.query(FORM_PATH_TABLE_NAME, columns, where, whereArgs, null, null, null)?.use { c ->
            with(c) {
                val ids = mutableListOf<Long>()
                while (moveToNext()) {
                    getColumnIndex(KEY_FORM_ID)
                        .takeIf { it >= 0 }
                        ?.let { getString(it) }
                        ?.toLong()
                        ?.also { ids.add(it) }
                }
                ids.toList()
            }
        }?.let { emit(FormsHelper.getByIds(it)) }
    }.flowOn(Dispatchers.IO)

    private fun detachFromHierarchy(formIds: List<Long>) {
        if (formIds.isNotEmpty()) {
            with(helper.writableDatabase) {
                beginTransaction()
                try {
                    formIds.chunked(SQLITE_MAX_HOST_PARAMETERS).forEach { chunk ->
                        val where = "$KEY_FORM_ID in (${makePlaceholders(chunk.size)})"
                        val idStrings = arrayOfNulls<String>(chunk.size)
                        for (i in chunk.indices) {
                            val id = chunk[i]
                            idStrings[i] = id.toString()
                        }
                        delete(FORM_PATH_TABLE_NAME, where, idStrings)
                    }
                    setTransactionSuccessful()
                } finally {
                    endTransaction()
                }
            }
        }
    }

    fun addSyncResult(fingerprint: String?, startTime: Long, endTime: Long, result: String?) {
        with(helper.writableDatabase) {
            beginTransaction()
            try {
                insert(SYNC_HISTORY_TABLE_NAME, null, ContentValues().apply {
                    put(KEY_FINGERPRINT, fingerprint)
                    put(KEY_START_TIME, startTime / MILLIS_IN_SECOND)
                    put(KEY_END_TIME, endTime / MILLIS_IN_SECOND)
                    put(KEY_RESULT, result)
                })
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    fun pruneSyncResults(daysToKeep: Int) {
        with(helper.writableDatabase) {
            beginTransaction()
            try {
                delete(SYNC_HISTORY_TABLE_NAME, "$KEY_START_TIME > date('now','-$daysToKeep days')", null)
                setTransactionSuccessful()
            } finally {
                endTransaction()
            }
        }
    }

    val syncResults: Array<Number>
        get() {
            val db = helper.readableDatabase
            val columns = arrayOf(KEY_START_TIME, "(end_time-start_time)/60.0")
            val where = "$KEY_RESULT = 'success'"
            val results = ArrayList<Number>()
            db.query(SYNC_HISTORY_TABLE_NAME, columns, where, null, null, null, KEY_START_TIME)?.use { cursor ->
                results.apply {
                    with(cursor) {
                        while (moveToNext()) {
                            add(getFloat(0)) // start time (seconds since unix epoch)
                            add(getFloat(1)) // sync duration (minutes)
                        }
                    }
                }
            }
            return results.toTypedArray()
        }

    fun addFavorite(item: DataWrapper): Long {
        with(helper.writableDatabase) {
            beginTransaction()
            return try {
                insert(FAVORITE_TABLE_NAME, null, ContentValues().apply { put(KEY_HIER_PATH, item.hierarchyId) })
                        .also { setTransactionSuccessful() }
            } finally {
                endTransaction()
            }
        }
    }

    fun removeFavorite(hierarchyId: String): Long {
        with(helper.writableDatabase) {
            beginTransaction()
            return try {
                delete(FAVORITE_TABLE_NAME, "$KEY_HIER_PATH = ?", arrayOf(hierarchyId)).toLong().also { setTransactionSuccessful() }
            } finally {
                endTransaction()
            }
        }
    }

    val favoriteIds: List<String>
        get() {
            val db = helper.readableDatabase
            val columns = arrayOf(KEY_HIER_PATH)
            val results = ArrayList<String>()
            db.query(FAVORITE_TABLE_NAME, columns, null, null, null, null, null)?.use {
                with(it) {
                    while (moveToNext()) {
                        results.add(getString(0))
                    }
                }
            }
            return results
        }

    private class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        private val tag = DatabaseHelper::class.java.simpleName

        private fun execSQL(db: SQLiteDatabase, vararg statements: String) {
            for (statement in statements) {
                db.execSQL(statement)
            }
        }

        override fun onCreate(db: SQLiteDatabase) {
            Log.i(tag, "creating database")
            execSQL(db, FORM_PATH_CREATE, SYNC_HISTORY_CREATE, START_TIME_IDX_CREATE, FAVORITE_CREATE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.i(tag, "upgrading db version $oldVersion to $newVersion")
            if (oldVersion < 17) {
                execSQL(db, SYNC_HISTORY_CREATE, START_TIME_IDX_CREATE)
            }
            if (oldVersion < 18) {
                execSQL(db, FAVORITE_CREATE)
            }
            if (oldVersion < 19) {
                execSQL(db, "DROP TABLE IF EXISTS $FORM_PATH_TABLE_NAME")
                execSQL(db, FORM_PATH_CREATE)
            }
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.i(tag, "downgrading db version $oldVersion to $newVersion")
            if (newVersion < 19) {
                execSQL(db, "DROP TABLE IF EXISTS $FORM_PATH_TABLE_NAME")
            }
            if (newVersion < 18) {
                db.execSQL("DROP TABLE IF EXISTS $FAVORITE_TABLE_NAME")
            }
            if (newVersion < 17) {
                db.execSQL("DROP TABLE IF EXISTS $SYNC_HISTORY_TABLE_NAME")
            }
        }
    }
}

private const val DATABASE_NAME = "entityData"
private const val DATABASE_VERSION = 19
private const val FORM_PATH_TABLE_NAME = "path_to_forms"
private const val FORM_PATH_IDX_NAME = "path_id"
private const val KEY_HIER_PATH = "hierarchyPath"
private const val KEY_FORM_ID = "form_id"
private const val SYNC_HISTORY_TABLE_NAME = "sync_history"
private const val KEY_FINGERPRINT = "fingerprint"
private const val START_TIME_IDX_NAME = "start_time_idx"
private const val KEY_START_TIME = "start_time"
private const val KEY_END_TIME = "end_time"
private const val KEY_RESULT = "result"
private const val FORM_PATH_CREATE = "CREATE TABLE IF NOT EXISTS $FORM_PATH_TABLE_NAME (" +
        "$KEY_HIER_PATH TEXT, " +
        "$KEY_FORM_ID TEXT, " +
        "CONSTRAINT $FORM_PATH_IDX_NAME UNIQUE ($KEY_FORM_ID)" +
        ")"
private const val SYNC_HISTORY_CREATE = "CREATE TABLE IF NOT EXISTS $SYNC_HISTORY_TABLE_NAME (" +
        "$KEY_FINGERPRINT TEXT NOT NULL, " +
        "$KEY_START_TIME INTEGER NOT NULL, " +
        "$KEY_END_TIME INTEGER NOT NULL, " +
        "$KEY_RESULT TEXT NOT NULL" +
        ")"
private const val START_TIME_IDX_CREATE = "CREATE INDEX IF NOT EXISTS $START_TIME_IDX_NAME ON $SYNC_HISTORY_TABLE_NAME($KEY_START_TIME)"
private const val FAVORITE_TABLE_NAME = "favorite"
private const val FAVORITE_CREATE = "CREATE TABLE IF NOT EXISTS $FAVORITE_TABLE_NAME ($KEY_HIER_PATH TEXT PRIMARY KEY)"
private const val MILLIS_IN_SECOND = 1000