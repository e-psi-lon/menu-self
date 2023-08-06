package fr.e_psi_lon.menuself


// DownloadManager
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AutoUpdater : DialogFragment() {
    private lateinit var hash: String
    private lateinit var changelog: String
    private lateinit var context: Context


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
                        main(activity as MainActivity)
                    }
                }
                .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    // User cancelled the dialog
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun getChangelog() {
        val output = httpRequest("https://api.github.com/repos/e-psi-lon/menu-self/commits/master")
        if (output == "") {
            changelog = context.getString(R.string.no_changelog)
        }
        val json = JSONObject(output)
        changelog = if (hash == json.getString("sha")) {
            val commit = json.getJSONObject("commit")
            commit.getString("message")
        } else {
            context.getString(R.string.no_changelog)
        }
    }

    private fun main(activity: MainActivity) {
        val output = httpRequest("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds")
        if (output == "") {
            return
        }
        val json = JSONObject(output)
        val lastCommitHash = json.getJSONObject("commit").getString("message").split(" ")[1]
        if (lastCommitHash != BuildConfig.GIT_COMMIT_HASH) {
            val files = json.getJSONArray("files")
            if (files.length() == 0) {
                return
            }
            println(if (files.length() == 1 && files.getJSONObject(0).getString("filename") == "app-release.apk" ) "File is app-release.apk" else "File is not app-release.apk")
            if (files.length() == 1 && files.getJSONObject(0).getString("filename") == "app-release.apk" ) {
                val contentsUrl = files.getJSONObject(0).getString("contents_url")
                val content = httpRequest(contentsUrl)
                if (content == "") {
                    return
                }
                val contentJson = JSONObject(content)
                val downloadUrl = contentJson.getString("download_url")
                downloadApk(downloadUrl, activity)

            }
        }

    }

    private fun downloadApk(url: String, activity: MainActivity) {
        var outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "${context.getString(R.string.app_name)}.apk")
        if (outputFile.exists()) {
            outputFile.delete()
        }
        outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "${context.getString(R.string.app_name)}.apk")
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(context.getString(R.string.app_name))
            .setDescription("Downloading ${context.getString(R.string.app_name)}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${context.getString(R.string.app_name)}.apk")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        while (outputFile.length() != 0L) {
            Thread.sleep(100)
        }
        outputFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "${context.getString(R.string.app_name)}.apk")
        installApk(outputFile, activity)

    }

    private fun httpRequest(url: String): String {
        val urlObject = URL(url)

        var output = ""
        with(urlObject.openConnection() as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET
            if (responseCode != 200) {
                return ""
            }
            inputStream.bufferedReader().use {
                it.lines().forEach { line ->
                    output += line + "\n"
                }
            }
        }
        return output
    }

    private fun getHash() {
        hash = getLastCommitHash()
    }

    private fun installApk(file: File, activity: MainActivity) {
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
            println("Error is $e")
            val message: String = when (e) {
                is SecurityException -> {
                    context.getString(R.string.permission_denied, e.message)
                }
                else -> {
                    context.getString(R.string.unknown_error, e.message)
                }
            }
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        fun getLastCommitHash(): String {
            val output = AutoUpdater().httpRequest("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds")
            val json = JSONObject(output)
            val commit = json.getJSONObject("commit")
            val message = commit.getString("message")
            // A message is 'builds: <hash>'
            return message.split(" ")[1]
        }
    }
}