@file:Suppress("MaxLineLength")

package se.eelde.toggles.database.dao.application

import android.database.Cursor
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import se.eelde.toggles.database.WrenchConfiguration
import se.eelde.toggles.database.WrenchConfigurationWithValues
import se.eelde.toggles.database.tables.ConfigurationTable
import se.eelde.toggles.database.tables.ConfigurationValueTable
import se.eelde.toggles.database.tables.ScopeTable
import java.util.Date

@Suppress("TooManyFunctions")
@Dao
interface TogglesConfigurationDao {

    @Query(
        "SELECT configuration.id, " +
            " configuration.configurationKey, " +
            " configuration.configurationType," +
            " configurationValue.value" +
            " FROM " + ConfigurationTable.TABLE_NAME +
            " INNER JOIN " + ConfigurationValueTable.TABLE_NAME + " ON configuration.id = configurationValue.configurationId " +
            " WHERE configuration.id = (:configurationId) AND configurationValue.scope = (:scopeId)"
    )
    fun getToggle(configurationId: Long, scopeId: Long): Cursor

    @Query(
        "SELECT configuration.id, " +
            " configuration.configurationKey, " +
            " configuration.configurationType," +
            " configurationValue.value" +
            " FROM " + ConfigurationTable.TABLE_NAME +
            " INNER JOIN " + ConfigurationValueTable.TABLE_NAME + " ON configuration.id = configurationValue.configurationId " +
            " WHERE configuration.configurationKey = (:configurationKey) AND configurationValue.scope = (:scopeId)"
    )
    fun getToggle(configurationKey: String, scopeId: Long): Cursor

    @Query(
        "SELECT configuration.id, " +
            " configuration.configurationKey, " +
            " configuration.configurationType," +
            " configurationValue.value," +
            " scope.name as scope" +
            " FROM " + ConfigurationTable.TABLE_NAME +
            " INNER JOIN " + ConfigurationValueTable.TABLE_NAME + " ON configuration.id = configurationValue.configurationId " +
            " INNER JOIN " + ScopeTable.TABLE_NAME + " ON configurationValue.scope = scope.id " +
            " WHERE configuration.configurationKey = (:configurationKey) AND configurationValue.scope IN (:scopeId)"
    )
    fun getToggles(configurationKey: String, scopeId: List<Long>): Cursor

    @Query(
        "SELECT configuration.id, " +
            " configuration.configurationKey, " +
            " configuration.configurationType," +
            " configurationValue.value," +
            " scope.name as scope" +
            " FROM " + ConfigurationTable.TABLE_NAME +
            " INNER JOIN " + ConfigurationValueTable.TABLE_NAME + " ON configuration.id = configurationValue.configurationId " +
            " INNER JOIN " + ScopeTable.TABLE_NAME + " ON configurationValue.scope = scope.id " +
            " WHERE configuration.configurationKey = (:configurationKey)"
    )
    fun getToggles(configurationKey: String): Cursor

    @Query(
        "SELECT * " +
            " FROM " + ConfigurationTable.TABLE_NAME +
            " WHERE configuration.applicationId = (:applicationId) AND configuration.configurationKey = (:configurationKey)"
    )
    fun getWrenchConfiguration(applicationId: Long, configurationKey: String): WrenchConfiguration?

    @Query(
        "SELECT * " +
            " FROM " + ConfigurationTable.TABLE_NAME +
            " WHERE configuration.applicationId = (:applicationId) AND configuration.configurationKey = (:configurationKey)"
    )
    fun getWrenchConfigurationByKey(
        applicationId: Long,
        configurationKey: String
    ): WrenchConfiguration?

    @Query(
        "SELECT * " +
            " FROM " + ConfigurationTable.TABLE_NAME +
            " WHERE configuration.applicationId = (:applicationId) AND configuration.id = (:configurationId)"
    )
    fun getWrenchConfigurationById(applicationId: Long, configurationId: Long): WrenchConfiguration?

    @Query("SELECT * FROM configuration WHERE id = :configurationId")
    fun getConfiguration(configurationId: Long): Flow<WrenchConfiguration>

    @Transaction
    @Query(
        "SELECT id, applicationId, configurationKey, configurationType FROM configuration WHERE applicationId = :applicationId ORDER BY lastUse DESC"
    )
    fun getApplicationConfigurations(applicationId: Long): Flow<List<WrenchConfigurationWithValues>>

    @Transaction
    @Query(
        "SELECT id, applicationId, configurationKey, configurationType FROM configuration WHERE applicationId = :applicationId AND configurationKey LIKE :query ORDER BY lastUse DESC"
    )
    fun getApplicationConfigurations(
        applicationId: Long,
        query: String
    ): Flow<List<WrenchConfigurationWithValues>>

    @Query("DELETE FROM configuration WHERE applicationId = :callingApplication AND id = :id")
    fun deleteConfiguration(callingApplication: Long, id: Long): Int

    @Query(
        "DELETE FROM configuration WHERE applicationId = :callingApplication AND configurationKey = :configurationKey"
    )
    fun deleteConfiguration(callingApplication: Long, configurationKey: String): Int

    @Insert
    fun insert(wrenchConfiguration: WrenchConfiguration): Long

    @Query("UPDATE configuration set lastUse=:date WHERE id= :configurationId")
    suspend fun touch(configurationId: Long, date: Date)

    @Query(
        "UPDATE configuration SET configurationKey = :key, configurationType = :type WHERE applicationId = :callingApplication AND id= :id"
    )
    fun updateConfiguration(callingApplication: Long, id: Long, key: String, type: String): Int

    @Query("SELECT * FROM configuration WHERE id = :configurationId and applicationId = :callingApplication")
    fun getConfigurationCursor(callingApplication: Long, configurationId: Long): Cursor

    @Query(
        "SELECT * FROM configuration WHERE configurationKey = :configurationKey and applicationId = :callingApplication"
    )
    fun getConfigurationCursor(callingApplication: Long, configurationKey: String): Cursor
}
