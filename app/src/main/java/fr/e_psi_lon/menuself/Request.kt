package fr.e_psi_lon.menuself

import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity


class Request {

    companion object {
        fun get(url: String): String {
            try {
                val urlObject = URL(url)

                var output = ""
                with(urlObject.openConnection() as HttpURLConnection) {
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
            } catch (e: Exception) {
                return ""
            }
        }

        fun download(url: String, context: Context, activity: AppCompatActivity, outputFile: File, visibility : Int, allowOverMetered : Boolean = true, allowOverRoaming : Boolean = true) : File {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(context.getString(R.string.app_name))
                .setDescription(context.getString(R.string.downloading, outputFile.name))
                .setNotificationVisibility(visibility)
                .setDestinationInExternalPublicDir(outputFile.parent, outputFile.name)
                .setAllowedOverMetered(allowOverMetered)
                .setAllowedOverRoaming(allowOverRoaming)
            val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)
            while (outputFile.length() != 0L) {
                Thread.sleep(100)
            }
            return File(outputFile.parent, outputFile.name)
        }
    }


}