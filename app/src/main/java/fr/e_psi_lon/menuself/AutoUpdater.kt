package fr.e_psi_lon.menuself

// Utiliser une lib interne a Java ou Kotlin pour faire une requete HTTP

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
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


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            CoroutineScope(Dispatchers.IO).launch {
                getHash()
            }
            while (!::hash.isInitialized) {
                Thread.sleep(100)
            }
            if (hash != "") {
                // On cherche le commit sur la branche master
                CoroutineScope(Dispatchers.IO).launch {
                    getChangelog()
                }
                while (!::changelog.isInitialized) {
                    Thread.sleep(100)
                }

            }
            builder.setMessage(getString(R.string.update_available, changelog))
                .setPositiveButton(getString(R.string.install_update)) { _, _ ->
                    CoroutineScope(Dispatchers.IO).launch {
                        main(activity as MainActivity)
                    }
                }
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    // User cancelled the dialog
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private suspend fun getChangelog() {
        val output = httpRequest("https://api.github.com/repos/e-psi-lon/menu-self/commits/master")
        println("Output (in getting changelog) is $output")
        if (output == "") {
            changelog = getString(R.string.no_changelog)
        }
        val json = JSONObject(output)
        println("Hash is $hash")
        println("Json hash is ${json.getString("sha").take(8)}")
        changelog = if (hash == json.getString("sha").take(8)) {
            // On obtient la valeur message de du champ commit
            val commit = json.getJSONObject("commit")
            println("Commit is $commit")
            println("Message is ${commit.getString("message")}")
            commit.getString("message")
        } else {
            getString(R.string.no_changelog)
        }
    }

    private suspend fun main(activity: MainActivity) {
        val output = httpRequest("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds")
        if (output == "") {
            return
        }
        println("Starting request")
        val json = JSONObject(output)
        val lastCommitHash = json.getString("sha").take(8)
        if (lastCommitHash != BuildConfig.GIT_COMMIT_HASH) {
            // On obtient le champ "files" qui est une liste de fichiers
            val files = json.getJSONArray("files")
            if (files.length() == 0) {
                return
            }
            if (files.length() == 1 && files.getJSONObject(0).getString("filename") == "apk-release.apk" ) {
                // On obtient le champ "contents_url" qui est l'url du fichier (pour l'API GitHub)
                val contentsUrl = files.getJSONObject(0).getString("contents_url")
                val content = httpRequest(contentsUrl)
                if (content == "") {
                    return
                }
                val contentJson = JSONObject(content)
                val downloadUrl = contentJson.getString("download_url")
                println("Download url is $downloadUrl")
                downloadApk(downloadUrl, activity)

            }
        }

    }

    private suspend fun downloadApk(url: String, activity: MainActivity) {
        var output: File?
        println("Downloading apk from $url")
        with(URL(url).openConnection()as HttpURLConnection) {
            requestMethod = "GET"  // optional default is GET
            if (responseCode != 200) {
                return
            }
            inputStream.use { inputStream ->
                val outputFile = File(activity.filesDir, "update.apk")
                outputFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                output = outputFile
            }
        }
        println("Downloaded apk")
        installApk(output!!, activity)

    }

    private suspend fun httpRequest(url: String): String {
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

    private suspend fun getHash() {
        hash = getLastCommitHash()
    }

    private fun installApk(file: File, activity: MainActivity) {
        println("Installing apk")
        val uri = FileProvider.getUriForFile(
            activity,
            "fr.e_psi_lon.menuself.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
    }

    companion object {
        suspend fun getLastCommitHash(): String {
            val output = AutoUpdater().httpRequest("https://api.github.com/repos/e-psi-lon/menu-self/commits/builds")
            val json = JSONObject(output)
            val commit = json.getJSONObject("commit")
            val message = commit.getString("message")
            // A message is 'builds: <hash>'
            return message.split(" ")[1].take(8)
        }
    }
}