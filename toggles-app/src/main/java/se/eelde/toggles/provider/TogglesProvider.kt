package se.eelde.toggles.provider

import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import com.izettle.wrench.core.Bolt
import com.izettle.wrench.core.WrenchProviderContract
import com.izettle.wrench.database.*
import com.izettle.wrench.preferences.ITogglesPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import se.eelde.toggles.BuildConfig
import java.util.*

class TogglesProvider : ContentProvider() {

    val applicationDao: WrenchApplicationDao by lazy {
        applicationEntryPoint.provideWrenchApplicationDao()
    }

    val scopeDao: WrenchScopeDao by lazy {
        applicationEntryPoint.provideWrenchScopeDao()
    }

    val configurationDao: WrenchConfigurationDao by lazy {
        applicationEntryPoint.provideWrenchConfigurationDao()
    }

    val configurationValueDao: WrenchConfigurationValueDao by lazy {
        applicationEntryPoint.provideWrenchConfigurationValueDao()
    }

    val predefinedConfigurationDao: WrenchPredefinedConfigurationValueDao by lazy {
        applicationEntryPoint.providePredefinedConfigurationValueDao()
    }

    val packageManagerWrapper: IPackageManagerWrapper by lazy {
        applicationEntryPoint.providePackageManagerWrapper()
    }

    val togglesPreferences: ITogglesPreferences by lazy {
        applicationEntryPoint.providesWrenchPreferences()
    }

    val applicationEntryPoint: TogglesProviderEntryPoint by lazy {
        EntryPointAccessors.fromApplication(context!!, TogglesProviderEntryPoint::class.java)
    }

    @EntryPoint
    @InstallIn(ApplicationComponent::class)
    interface TogglesProviderEntryPoint {
        fun provideWrenchApplicationDao(): WrenchApplicationDao
        fun provideWrenchConfigurationDao(): WrenchConfigurationDao
        fun provideWrenchConfigurationValueDao(): WrenchConfigurationValueDao
        fun provideWrenchScopeDao(): WrenchScopeDao
        fun providePredefinedConfigurationValueDao(): WrenchPredefinedConfigurationValueDao
        fun providePackageManagerWrapper(): IPackageManagerWrapper
        fun providesWrenchPreferences(): ITogglesPreferences
    }

    @Synchronized
    private fun getCallingApplication(applicationDao: WrenchApplicationDao): WrenchApplication {
        var wrenchApplication: WrenchApplication? = applicationDao.loadByPackageName(packageManagerWrapper.callingApplicationPackageName!!)

        if (wrenchApplication == null) {
            try {
                wrenchApplication = WrenchApplication(0, packageManagerWrapper.callingApplicationPackageName!!, packageManagerWrapper.applicationLabel)

                wrenchApplication.id = applicationDao.insert(wrenchApplication)

            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                throw RuntimeException(e)
            }
        }

        return wrenchApplication
    }

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {

        val callingApplication = getCallingApplication(applicationDao)

        if (!isTogglesApplication(callingApplication)) {
            assertValidApiVersion(togglesPreferences, uri)
        }

        var cursor: Cursor?

        when (uriMatcher.match(uri)) {
            CURRENT_CONFIGURATION_ID -> {
                val scope = getSelectedScope(context, scopeDao, callingApplication.id)
                cursor = configurationDao.getBolt(java.lang.Long.valueOf(uri.lastPathSegment!!), scope!!.id)

                if (cursor.count == 0) {
                    cursor.close()

                    val defaultScope = getDefaultScope(context, scopeDao, callingApplication.id)
                    cursor = configurationDao.getBolt(java.lang.Long.valueOf(uri.lastPathSegment!!), defaultScope!!.id)
                }
            }
            CURRENT_CONFIGURATION_KEY -> {
                val scope = getSelectedScope(context, scopeDao, callingApplication.id)
                cursor = configurationDao.getBolt(uri.lastPathSegment!!, scope!!.id)

                if (cursor.count == 0) {
                    cursor.close()

                    val defaultScope = getDefaultScope(context, scopeDao, callingApplication.id)
                    cursor = configurationDao.getBolt(uri.lastPathSegment!!, defaultScope!!.id)
                }
            }
            else -> {
                throw UnsupportedOperationException("Not yet implemented $uri")
            }
        }

        if (context != null) {
            cursor.setNotificationUri(context!!.contentResolver, uri)
        }

        return cursor
    }

    private fun isTogglesApplication(callingApplication: WrenchApplication): Boolean {
        return callingApplication.packageName == BuildConfig.APPLICATION_ID
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {

        val callingApplication = getCallingApplication(applicationDao)

        if (!isTogglesApplication(callingApplication)) {
            assertValidApiVersion(togglesPreferences, uri)
        }

        val insertId: Long
        when (uriMatcher.match(uri)) {
            CURRENT_CONFIGURATIONS -> {
                var bolt = Bolt.fromContentValues(values!!)

                bolt = fixRCTypes(bolt)

                var wrenchConfiguration: WrenchConfiguration? = configurationDao.getWrenchConfiguration(callingApplication.id, bolt.key)

                if (wrenchConfiguration == null) {
                    wrenchConfiguration = WrenchConfiguration(0, callingApplication.id, bolt.key, bolt.type)

                    wrenchConfiguration.id = configurationDao.insert(wrenchConfiguration)
                }

                val defaultScope = getDefaultScope(context, scopeDao, callingApplication.id)

                val wrenchConfigurationValue = WrenchConfigurationValue(0, wrenchConfiguration.id, bolt.value, defaultScope!!.id)
                wrenchConfigurationValue.configurationId = wrenchConfiguration.id
                wrenchConfigurationValue.value = bolt.value
                wrenchConfigurationValue.scope = defaultScope.id

                wrenchConfigurationValue.id = configurationValueDao.insertSync(wrenchConfigurationValue)

                insertId = wrenchConfiguration.id
            }
            PREDEFINED_CONFIGURATION_VALUES -> {
                val fullConfig = WrenchPredefinedConfigurationValue.fromContentValues(values!!)
                insertId = predefinedConfigurationDao.insert(fullConfig)
            }
            else -> {
                throw UnsupportedOperationException("Not yet implemented $uri")
            }
        }

        context!!.contentResolver.notifyChange(Uri.withAppendedPath(uri, insertId.toString()), null, false)

        return ContentUris.withAppendedId(uri, insertId)
    }

    private fun fixRCTypes(bolt: Bolt): Bolt {
        return when (bolt.type) {
            "java.lang.String" -> bolt.copy(bolt.id, Bolt.TYPE.STRING, bolt.key, bolt.value)
            "java.lang.Integer" -> bolt.copy(bolt.id, Bolt.TYPE.INTEGER, bolt.key, bolt.value)
            "java.lang.Enum" -> bolt.copy(bolt.id, Bolt.TYPE.ENUM, bolt.key, bolt.value)
            "java.lang.Boolean" -> bolt.copy(bolt.id, Bolt.TYPE.BOOLEAN, bolt.key, bolt.value)
            else -> {
                bolt
            }
        }
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        val callingApplication = getCallingApplication(applicationDao)

        if (!isTogglesApplication(callingApplication)) {
            assertValidApiVersion(togglesPreferences, uri)
        }

        return super.bulkInsert(uri, values)
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {

        val callingApplication = getCallingApplication(applicationDao)

        if (!isTogglesApplication(callingApplication)) {
            assertValidApiVersion(togglesPreferences, uri)
        }

        val updatedRows: Int
        when (uriMatcher.match(uri)) {
            CURRENT_CONFIGURATION_ID -> {
                val bolt = Bolt.fromContentValues(values!!)
                val scope = getSelectedScope(context, scopeDao, callingApplication.id)
                updatedRows = configurationValueDao.updateConfigurationValueSync(java.lang.Long.parseLong(uri.lastPathSegment!!), scope!!.id, bolt.value!!)
                if (updatedRows == 0) {
                    val wrenchConfigurationValue = WrenchConfigurationValue(0, java.lang.Long.parseLong(uri.lastPathSegment!!), bolt.value, scope.id)
                    configurationValueDao.insertSync(wrenchConfigurationValue)
                }
            }
            else -> {
                throw UnsupportedOperationException("Not yet implemented $uri")
            }
        }

        if (updatedRows > 0) {
            context!!.contentResolver.notifyChange(uri, null, false)
        }

        return updatedRows
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val callingApplication = getCallingApplication(applicationDao)

        if (!isTogglesApplication(callingApplication)) {
            assertValidApiVersion(togglesPreferences, uri)
        }

        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun getType(uri: Uri): String? {
        val callingApplication = getCallingApplication(applicationDao)

        if (!isTogglesApplication(callingApplication)) {
            assertValidApiVersion(togglesPreferences, uri)
        }

        when (uriMatcher.match(uri)) {
            CURRENT_CONFIGURATIONS -> {
                return "vnd.android.cursor.dir/vnd.${BuildConfig.APPLICATION_ID}.currentConfiguration"
            }
            CURRENT_CONFIGURATION_ID -> {
                return "vnd.android.cursor.item/vnd.${BuildConfig.APPLICATION_ID}.currentConfiguration"
            }
            CURRENT_CONFIGURATION_KEY -> {
                return "vnd.android.cursor.dir/vnd.${BuildConfig.APPLICATION_ID}.currentConfiguration"
            }
            PREDEFINED_CONFIGURATION_VALUES -> {
                return "vnd.android.cursor.dir/vnd..${BuildConfig.APPLICATION_ID}..predefinedConfigurationValue"
            }
            else -> {
                throw UnsupportedOperationException("Not yet implemented")
            }
        }
    }

    companion object {

        private const val CURRENT_CONFIGURATION_ID = 1
        private const val CURRENT_CONFIGURATION_KEY = 2
        private const val CURRENT_CONFIGURATIONS = 3
        private const val PREDEFINED_CONFIGURATION_VALUES = 5
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            uriMatcher.addURI(WrenchProviderContract.WRENCH_AUTHORITY, "currentConfiguration/#", CURRENT_CONFIGURATION_ID)
            uriMatcher.addURI(WrenchProviderContract.WRENCH_AUTHORITY, "currentConfiguration/*", CURRENT_CONFIGURATION_KEY)
            uriMatcher.addURI(WrenchProviderContract.WRENCH_AUTHORITY, "currentConfiguration", CURRENT_CONFIGURATIONS)
            uriMatcher.addURI(WrenchProviderContract.WRENCH_AUTHORITY, "predefinedConfigurationValue", PREDEFINED_CONFIGURATION_VALUES)
        }

        @Synchronized
        private fun getDefaultScope(context: Context?, scopeDao: WrenchScopeDao?, applicationId: Long): WrenchScope? {
            if (context == null) {
                return null
            }

            var scope: WrenchScope? = scopeDao!!.getDefaultScope(applicationId)

            if (scope == null) {
                scope = WrenchScope()
                scope.applicationId = applicationId
                val id = scopeDao.insert(scope)
                scope.id = id
            }
            return scope
        }

        @Synchronized
        private fun getSelectedScope(context: Context?, scopeDao: WrenchScopeDao?, applicationId: Long): WrenchScope? {
            if (context == null) {
                return null
            }

            var scope: WrenchScope? = scopeDao!!.getSelectedScope(applicationId)

            if (scope == null) {
                val defaultScope = WrenchScope()
                defaultScope.applicationId = applicationId
                defaultScope.id = scopeDao.insert(defaultScope)

                val customScope = WrenchScope()
                customScope.applicationId = applicationId
                customScope.timeStamp = Date(defaultScope.timeStamp.time + 1000)
                customScope.name = WrenchScope.SCOPE_USER
                customScope.id = scopeDao.insert(customScope)

                scope = customScope
            }
            return scope
        }

        private fun assertValidApiVersion(togglesPreferences: ITogglesPreferences, uri: Uri) {
            when (getApiVersion(uri)) {
                API_1 -> {
                    return
                }
                API_INVALID -> {
                    var l: Long = 0
                    try {
                        l = Binder.clearCallingIdentity()
                        if (togglesPreferences.getBoolean("Require valid toggles api version", false)) {
                            throw IllegalArgumentException("This content provider requires you to provide a valid api-version in a queryParameter")
                        }
                    } finally {
                        Binder.restoreCallingIdentity(l)
                    }
                }
                else -> {
                    var l: Long = 0
                    try {
                        l = Binder.clearCallingIdentity()
                        if (togglesPreferences.getBoolean("Require valid toggles api version", false)) {
                            throw IllegalArgumentException("This content provider requires you to provide a valid api-version in a queryParameter")
                        }
                    } finally {
                        Binder.restoreCallingIdentity(l)
                    }
                }
            }
        }

        @TogglesApiVersion
        private fun getApiVersion(uri: Uri): Int {
            val queryParameter = uri.getQueryParameter(WrenchProviderContract.WRENCH_API_VERSION)
            return if (queryParameter != null) {
                Integer.valueOf(queryParameter)
            } else {
                API_INVALID
            }
        }
    }
}
