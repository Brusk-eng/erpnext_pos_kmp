package com.erpnext.pos.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.erpnext.pos.utils.instanceKeyFromUrl
import kotlinx.coroutines.Dispatchers

actual class DatabaseBuilder(private val context: Context) {
  private fun resolveCurrentSite(): String? {
    val authPrefs = context.getSharedPreferences(AUTH_INFO_PREF_FILE, Context.MODE_PRIVATE)
    return authPrefs.getString(CURRENT_SITE_KEY, null)
  }

  private fun resolveDatabaseName(): String = "app_database_${instanceKeyFromUrl(resolveCurrentSite())}"

  actual fun build(): AppDatabase =
      Room.databaseBuilder(
              context,
              AppDatabase::class.java,
              resolveDatabaseName(),
          )
          .setDriver(BundledSQLiteDriver())
          .setQueryCoroutineContext(Dispatchers.IO)
          .fallbackToDestructiveMigration(true)
          .build()

  private companion object {
    const val AUTH_INFO_PREF_FILE = "auth_info_prefs_v1"
    const val CURRENT_SITE_KEY = "current_site"
  }
}
