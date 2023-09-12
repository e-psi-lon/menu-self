package fr.e_psi_lon.menuself


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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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
        val output = Request.get("https://api.github.com/repos/e-psi-lon/menu-self/commits/master")
        if (output == "") {
            changelog = context.getString(R.string.no_changelog)
            return
        }
        val json = JSONObject(output)
        val commits = Request.get(
            "https://api.github.com/repos/e-psi-lon/menu-self/commits?sha=${
                json.getString("sha")
            }"
        )
        if (commits == "") {
            changelog = context.getString(R.string.no_changelog)
            return
        }
        val commitsJson = JSONArray(commits)
        // On récupère le nombre de commits entre le commit de la version installée et le commit de la version disponible
        // Une fois qu'on a ce nombre, on récupère les messages des commits entre les deux en limitant à 3 et si plus on ajoute "..."
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
            "1. ${commitMessages[0]}\n2. ${commitMessages[1]}\n3. ${commitMessages[2]}\n${context.getString(R.string.more)}"
        } else {
            for (i in 0 until commitMessages.size) {
                commitMessages[i] = "${i + 1}. ${commitMessages[i]}"
            }
            commitMessages.joinToString("\n")
        }
    }

    private fun main(activity: FragmentActivity) {
        val output = Request.get("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds")
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
            if (files.length() == 1 && files.getJSONObject(0)
                    .getString("filename") == "app-release.apk"
            ) {
                val contentsUrl = files.getJSONObject(0).getString("contents_url")
                val content = Request.get(contentsUrl)
                if (content == "") {
                    return
                }
                val contentJson = JSONObject(content)
                val downloadUrl = contentJson.getString("download_url")
                val size = contentJson.getLong("size")
                downloadApk(downloadUrl, activity, size)

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

        val output = Request.download(
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
        hash = getLastCommitHash()
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
                    println("Unknown error is $e")
                    context.getString(R.string.unknown_error, e.message)
                }
            }
            activity.runOnUiThread {
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        fun getLastCommitHash(): String {
            val output =
                Request.get("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds")
            val json = JSONObject(output)
            val commit = json.getJSONObject("commit")
            val message = commit.getString("message")
            return message.split(" ")[1]
        }
    }
}