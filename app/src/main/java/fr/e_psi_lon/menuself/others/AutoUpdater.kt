package fr.e_psi_lon.menuself.others


import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import fr.e_psi_lon.menuself.BuildConfig
import fr.e_psi_lon.menuself.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import fr.e_psi_lon.menuself.data.Request as menuRequest

class AutoUpdater : DialogFragment() {
    private lateinit var hash: String
    private lateinit var changelog: String
    private lateinit var context: Context
    private lateinit var updateChannel: String


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            context = it.applicationContext
            CoroutineScope(Dispatchers.IO).launch {
                getHash()
            }
            while (!::hash.isInitialized) {
                Thread.sleep(100)
            }
            if (hash != "") {
                CoroutineScope(Dispatchers.IO).launch {
                    getChangelog()
                }
                while (!::changelog.isInitialized) {
                    Thread.sleep(100)
                }

            }
            builder.setMessage(context.getString(R.string.update_available, changelog))
                .setPositiveButton(context.getString(R.string.install_update)) { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        main(requireActivity())
                    }
                }
                .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun getChangelog() {
        val client = OkHttpClient()
        var request = Request.Builder()
            .url("https://api.github.com/repos/e-psi-lon/menu-self/commits/master")
            .build()
        val output = client.newCall(request).execute().body?.string()
        if (output == "") {
            changelog = context.getString(R.string.no_changelog)
            return
        }
        val json = output?.let { JSONObject(it) }
        request = Request.Builder()
            .url(
                "https://api.github.com/repos/e-psi-lon/menu-self/commits?sha=${
                    json!!.getString("sha")
                }"
            )
            .build()
        val commits = client.newCall(request).execute().body?.string()
        if (commits == "") {
            changelog = context.getString(R.string.no_changelog)
            return
        }
        val commitsJson = JSONArray(commits)
        val commitCount = commitsJson.length()
        val commitMessages = mutableListOf<String>()
        for (i in 0 until commitCount) {
            val commit = commitsJson.getJSONObject(i)
            if (commit.getString("sha") == BuildConfig.GIT_COMMIT_HASH) {
                break
            }
            commitMessages.add(commit.getJSONObject("commit").getString("message"))
        }
        changelog = if (commitMessages.size > 3) {
            "1. ${commitMessages[0]}\n2. ${commitMessages[1]}\n3. ${commitMessages[2]}\n${
                context.getString(
                    R.string.more
                )
            }"
        } else {
            for (i in 0 until commitMessages.size) {
                commitMessages[i] = "${i + 1}. ${commitMessages[i]}"
            }
            commitMessages.joinToString("\n")
        }
    }

    private fun main(activity: FragmentActivity) {
        val client = OkHttpClient()
        var request = Request.Builder()
            .url("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds-$updateChannel")
            .build()
        val output = client.newCall(request).execute().body?.string()
        if (output == "") {
            return
        }
        val json = output?.let { JSONObject(it) }
        val lastCommitHash = json?.getJSONObject("commit")?.getString("message")?.split(" ")?.get(1)
        if (lastCommitHash != BuildConfig.GIT_COMMIT_HASH) {
            val files = json?.getJSONArray("files")
            if (files != null) {
                if (files.length() == 0) {
                    return
                }
            }
            if (files != null) {
                if (files.length() == 1 && files.getJSONObject(0)
                        .getString("filename") == "app-release.apk"
                ) {
                    val contentsUrl = files.getJSONObject(0).getString("contents_url")
                    request = Request.Builder()
                        .url(contentsUrl)
                        .build()
                    val content = client.newCall(request).execute().body?.string()
                    if (content == "") {
                        return
                    }
                    val contentJson = content?.let { JSONObject(it) }
                    val downloadUrl = contentJson?.getString("download_url")
                    val size = contentJson?.getLong("size")
                    if (downloadUrl != null && size != null) {
                        downloadApk(downloadUrl, activity, size)
                    }

                }
            }
        }

    }

    private fun downloadApk(url: String, activity: FragmentActivity, fileSize: Long = 0) {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        var outputFile = File(cacheDir, "app-release.apk")
        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile = File(cacheDir, "app-release.apk")
        fun onDownloadError() {
            activity.runOnUiThread {
                Toast.makeText(
                    context,
                    context.getString(R.string.download_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val output = menuRequest.download(
            url,
            context,
            activity,
            outputFile,
            DownloadManager.Request.VISIBILITY_VISIBLE,
            fileSize
        )
        if (output == null) {
            onDownloadError()
            return
        } else {
            while (activity.supportFragmentManager.findFragmentByTag("downloading") != null) {
                Thread.sleep(100)
            }
            if (output.exists()) {
                activity.runOnUiThread {
                    val builder = AlertDialog.Builder(activity)
                    builder.setMessage(context.getString(R.string.install_apk, output.name))
                        .setPositiveButton(context.getString(R.string.install)) { _, _ ->
                            installApk(output, activity)
                        }
                        .setCancelable(false)
                    builder.create().show()
                }
            }
        }
    }


    private fun getHash() {
        hash = getLastCommitHash(updateChannel)
    }

    private fun installApk(file: File, activity: FragmentActivity) {
        try {
            val uri = FileProvider.getUriForFile(
                activity,
                "fr.e_psi_lon.menuself.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            val message: String = when (e) {
                is SecurityException -> {
                    context.getString(R.string.permission_denied, e.message)
                }

                else -> {
                    context.getString(R.string.unknown_error, e.message)
                }
            }
            activity.runOnUiThread {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun setChannel(channel: String) {
        updateChannel = channel
    }

    companion object {
        fun getLastCommitHash(channel: String): String {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds-$channel")
                .build()
            val output = client.newCall(request).execute().body?.string()
            if (output == "") {
                return ""
            }
            val json = output?.let { JSONObject(it) }
            return if (json != null) {
                json.getJSONObject("commit").getString("message").split(" ")[1]
            } else {
                ""
            }
        }

        fun checkForUpdates(
            activity: FragmentActivity,
            channel: String,
            start: Boolean = true
        ): Boolean {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds-$channel")
                    .build()
                val output = client.newCall(request).execute().body?.string()
                if (output == "") {
                    return false
                }
                val json = output?.let { JSONObject(it) }
                val lastCommitHash =
                    json?.getJSONObject("commit")?.getString("message")?.split(" ")?.get(1)
                return if (lastCommitHash != BuildConfig.GIT_COMMIT_HASH) {
                    if (!start) {
                        return true
                    }
                    AutoUpdater().apply {
                        setChannel(channel)
                        show(activity.supportFragmentManager, "update")
                    }
                    true
                } else
                    false
            } catch (e: Exception) {
                return false
            }
        }
    }
}