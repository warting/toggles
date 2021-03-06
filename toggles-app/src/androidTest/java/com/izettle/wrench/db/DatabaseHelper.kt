package com.izettle.wrench.db

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL
import androidx.sqlite.db.SupportSQLiteDatabase
import com.izettle.wrench.database.tables.ApplicationTable
import com.izettle.wrench.database.tables.ConfigurationTable
import org.junit.Assert.assertNotNull

object DatabaseHelper {
    fun insertWrenchConfiguration(db: SupportSQLiteDatabase, applicationId: Long, key: String, type: String): Long {
        val configurationValues = ContentValues()
        configurationValues.put(ConfigurationTable.COL_APP_ID, applicationId)
        configurationValues.put(ConfigurationTable.COL_KEY, key)
        configurationValues.put(ConfigurationTable.COL_TYPE, type)
        return db.insert(ConfigurationTable.TABLE_NAME, CONFLICT_FAIL, configurationValues)

    }

    fun getWrenchConfigurationByKey(db: SupportSQLiteDatabase, key: String): Cursor {
        val query = db.query("SELECT * FROM " + ConfigurationTable.TABLE_NAME + " WHERE " + ConfigurationTable.COL_KEY + "=?", arrayOf<Any>(key))
        assertNotNull(query)
        return query
    }

    fun insertWrenchApplication(db: SupportSQLiteDatabase, applicationLabel: String, packageName: String): Long {
        val applicationValues = ContentValues()
        applicationValues.put(ApplicationTable.COL_APP_LABEL, applicationLabel)
        applicationValues.put(ApplicationTable.COL_PACK_NAME, packageName)
        return db.insert(ApplicationTable.TABLE_NAME, CONFLICT_FAIL, applicationValues)
    }
}
