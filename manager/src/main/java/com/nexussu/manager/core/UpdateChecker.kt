package com.nexussu.manager.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    // Replace with your actual GitHub username and repo name
    private const val GITHUB_API = "https://api.github.com/repos/binit-ops/NexusSU/releases/latest"

    // NEW: Accepts the current installed version to compare against
    suspend fun checkForUpdates(context: Context, currentVersion: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestVersion = json.optString("tag_name", "v0.0.0")
                    val htmlUrl = json.optString("html_url", "")
                    
                    // Compare the real versions
                    if (latestVersion != currentVersion) {
                        return@withContext htmlUrl
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }
}
