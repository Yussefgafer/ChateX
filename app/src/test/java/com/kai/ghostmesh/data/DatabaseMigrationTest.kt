package com.kai.ghostmesh.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.kai.ghostmesh.core.data.local.AppDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.Ignore
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Ignore("Requires Room schema files to be present in app/schemas")
    @Test
    fun migrate12To13() {
        var db = helper.createDatabase(TEST_DB, 12)

        db.execSQL("INSERT INTO profiles (id, name, status, lastSeen) VALUES ('peer1', 'Peer One', 'Active', 123456)")
        db.close()

        db = helper.runMigrationsAndValidate(TEST_DB, 13, true)

        val cursor = db.query("SELECT * FROM profiles WHERE id = 'peer1'")
        cursor.moveToFirst()
        assertEquals("Peer One", cursor.getString(cursor.getColumnIndex("name")))
        assertEquals(0, cursor.getInt(cursor.getColumnIndex("batteryLevel")))

        val bestEndpointIndex = cursor.getColumnIndex("bestEndpoint")
        assertTrue("bestEndpoint column should exist", bestEndpointIndex != -1)

        cursor.close()
    }
}
