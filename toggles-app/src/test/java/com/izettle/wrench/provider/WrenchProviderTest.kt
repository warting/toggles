package com.izettle.wrench.provider

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.izettle.wrench.core.Bolt
import com.izettle.wrench.core.Nut
import com.izettle.wrench.core.WrenchProviderContract
import com.izettle.wrench.database.WrenchDatabase
import com.izettle.wrench.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config
import se.eelde.toggles.BuildConfig
import javax.inject.Singleton

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@UninstallModules(DatabaseModule::class)
@Config(application = HiltTestApplication::class)
class WrenchProviderTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var wrenchProvider: WrenchProvider

    @Module
    @InstallIn(ApplicationComponent::class)
    object TestModule {
        @Singleton
        @Provides
        fun provideWrenchDb(@ApplicationContext context: Context): WrenchDatabase {
            return Room.inMemoryDatabaseBuilder(context, WrenchDatabase::class.java).allowMainThreadQueries().build()
        }
    }

    @Before
    fun setUp() {
        hiltRule.inject()

        val contentProviderController = Robolectric.buildContentProvider(WrenchProvider::class.java).create(BuildConfig.CONFIG_AUTHORITY)
        wrenchProvider = contentProviderController.get()

    }

    @Test
    fun testInsertBolt() {
        val insertBoltKey = "insertBoltKey"

        val uri = WrenchProviderContract.boltUri()
        val insertBolt = getBolt(insertBoltKey)
        val insertBoltUri = wrenchProvider.insert(uri, insertBolt.toContentValues())
        Assert.assertNotNull(insertBoltUri)

        var cursor = wrenchProvider.query(WrenchProviderContract.boltUri(insertBoltKey), null, null, null, null)
        Assert.assertNotNull(cursor)
        Assert.assertEquals(1, cursor!!.count)

        cursor.moveToFirst()
        var queryBolt = Bolt.fromCursor(cursor)

        Assert.assertEquals(insertBolt.key, queryBolt.key)
        Assert.assertEquals(insertBolt.value, queryBolt.value)
        Assert.assertEquals(insertBolt.type, queryBolt.type)

        cursor = wrenchProvider.query(WrenchProviderContract.boltUri(Integer.parseInt(insertBoltUri!!.lastPathSegment!!).toLong()), null, null, null, null)
        Assert.assertNotNull(cursor)
        Assert.assertEquals(1, cursor!!.count)

        cursor.moveToFirst()
        queryBolt = Bolt.fromCursor(cursor)

        Assert.assertEquals(insertBolt.key, queryBolt.key)
        Assert.assertEquals(insertBolt.value, queryBolt.value)
        Assert.assertEquals(insertBolt.type, queryBolt.type)
    }

    @Test
    fun testUpdateBolt() {
        val updateBoltKey = "updateBoltKey"

        val uri = WrenchProviderContract.boltUri()
        val insertBolt = getBolt(updateBoltKey)
        val insertBoltUri = wrenchProvider.insert(uri, insertBolt.toContentValues())
        Assert.assertNotNull(insertBoltUri)

        var cursor = wrenchProvider.query(WrenchProviderContract.boltUri(updateBoltKey), null, null, null, null)
        Assert.assertNotNull(cursor)
        Assert.assertTrue(cursor!!.moveToFirst())

        val providerBolt = Bolt.fromCursor(cursor)
        Assert.assertEquals(insertBolt.key, providerBolt.key)
        Assert.assertEquals(insertBolt.value, providerBolt.value)
        Assert.assertEquals(insertBolt.type, providerBolt.type)

        val updateBolt = Bolt(providerBolt.id, providerBolt.type, providerBolt.key, providerBolt.value!! + providerBolt.value!!)

        val update = wrenchProvider.update(WrenchProviderContract.boltUri(updateBolt.id), updateBolt.toContentValues(), null, null)
        Assert.assertEquals(1, update)

        cursor = wrenchProvider.query(WrenchProviderContract.boltUri(updateBoltKey), null, null, null, null)
        Assert.assertNotNull(cursor)

        Assert.assertTrue(cursor!!.moveToFirst())
        val updatedBolt = Bolt.fromCursor(cursor)

        Assert.assertEquals(insertBolt.value!! + insertBolt.value!!, updatedBolt.value)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingInsertForBoltWithId() {
        wrenchProvider.insert(WrenchProviderContract.boltUri(0), getBolt("dummyBolt").toContentValues())
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingInsertForBoltWithKey() {
        wrenchProvider.insert(WrenchProviderContract.boltUri("fake"), getBolt("dummyBolt").toContentValues())
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingUpdateForBoltWithKey() {
        wrenchProvider.update(WrenchProviderContract.boltUri("fake"), getBolt("dummyBolt").toContentValues(), null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingUpdateForBolts() {
        wrenchProvider.update(WrenchProviderContract.boltUri(), getBolt("dummyBolt").toContentValues(), null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingUpdateForNuts() {
        wrenchProvider.update(WrenchProviderContract.nutUri(), getNut("dummyNut").toContentValues(), null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingQueryForBolts() {
        wrenchProvider.update(WrenchProviderContract.boltUri(), getBolt("dummyBolt").toContentValues(), null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingQueryForNuts() {
        wrenchProvider.update(WrenchProviderContract.nutUri(), getNut("dummyNut").toContentValues(), null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingDeleteForBoltWithId() {
        wrenchProvider.delete(WrenchProviderContract.boltUri(0), null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingDeleteForBoltWithKey() {
        wrenchProvider.delete(WrenchProviderContract.boltUri("fake"), null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingDeleteForBolts() {
        wrenchProvider.delete(WrenchProviderContract.boltUri(), null, null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testMissingDeleteForNuts() {
        wrenchProvider.delete(WrenchProviderContract.nutUri(), null, null)
    }

    private fun getNut(value: String): Nut {
        return Nut(0, value)
    }

    private fun getBolt(key: String): Bolt {
        return Bolt(0L, "bolttype", key, "boltvalue")
    }
}