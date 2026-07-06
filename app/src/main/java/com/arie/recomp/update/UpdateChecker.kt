package com.arie.recomp.update

import com.arie.recomp.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Checks the public GitHub release for a newer build. This is the app's only
 * network call besides Health Connect (which is local). Fails silently when
 * offline — the app never depends on it.
 */
object UpdateChecker {

    const val DOWNLOAD_URL =
        "https://github.com/afellnersfs/Recomp/releases/download/latest/recomp.apk"
    private const val API =
        "https://api.github.com/repos/afellnersfs/Recomp/releases/latest"

    /** Returns the newer version name (e.g. "1.2") if one is published, else null. */
    suspend fun newerVersion(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(API).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            // Release name is "Recomp v1.2" — set by the build workflow.
            val remote = JSONObject(body).optString("name")
                .substringAfterLast("v", "").trim()
            remote.takeIf { it.isNotEmpty() && isNewer(it, BuildConfig.VERSION_NAME) }
        }.getOrNull()
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv != lv) return rv > lv
        }
        return false
    }
}
