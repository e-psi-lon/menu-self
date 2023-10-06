package fr.e_psi_lon.menuself.data

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import fr.e_psi_lon.menuself.R
import fr.e_psi_lon.menuself.others.DownloadingProgress
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class Request {
    companion object {
        fun formatSize(size: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var i = 0
            var size2 = size.toDouble()
            while (size2 > 1024 && i < units.size - 1) {
                size2 /= 1024
                i++
            }
            return "%.2f".format(size2) + " " + units[i]
        }

        fun get(url: String, headers: MutableMap<Any, Any> = mutableMapOf()): String {
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

        fun getImage(url: String, headers: Map<String, String> = emptyMap()): Bitmap? {
            try {
                val imageUrl = URL(url)
                val connection = imageUrl.openConnection() as HttpURLConnection
                connection.setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0"
                ) // Exemple d'en-tête HTTP pour simuler un navigateur

                for ((key, value) in headers) {
                    connection.setRequestProperty(key, value)
                }

                val inputStream: InputStream = connection.inputStream
                return BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun post(
            url: String,
            data: MutableMap<Any, Any>,
            headers: MutableMap<Any, Any> = mutableMapOf()
        ): String {
            try {
                val urlObject = URL(url)
                var output = ""
                with(urlObject.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    for (header in headers) {
                        setRequestProperty(header.key.toString(), header.value.toString())
                    }
                    val postData = StringBuilder()
                    for (datum in data) {
                        if (postData.isNotEmpty()) {
                            postData.append('&')
                        }
                        postData.append(datum.key)
                        postData.append('=')
                        postData.append(datum.value)
                    }
                    val postDataBytes = postData.toString().toByteArray(charset("UTF-8"))
                    doOutput = true
                    outputStream.write(postDataBytes)
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

        fun download(
            url: String,
            context: Context,
            activity: FragmentActivity,
            outputFile: File,
            visibility: Int,
            fileSize: Long,
            allowOverMetered: Boolean = true,
            allowOverRoaming: Boolean = true
        ): File? {
            val request = DownloadManager.Request(url.toUri()).apply {
                setTitle(context.getString(R.string.app_name))
                setDescription(context.getString(R.string.downloading, outputFile.name))
                setNotificationVisibility(visibility)
                setDestinationUri(outputFile.toUri())
                setAllowedOverMetered(allowOverMetered)
                setAllowedOverRoaming(allowOverRoaming)
            }
            val downloadManager =
                activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)
            var previousSize = -1L
            var currentSize = 0L
            var cancel = false
            activity.runOnUiThread {
                DownloadingProgress().apply {
                    setUrl(url)
                    setOutputFile(outputFile)
                    setFileSize(fileSize)
                    setDownloadManager(downloadManager)
                    setDownloadId(downloadId.toString())
                    show(activity.supportFragmentManager, "downloading")
                }
            }

            while (true) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)
                if (cancel) {
                    cursor.close()
                    return null
                }
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    var status: Int
                    if (columnIndex < 0) {
                        continue
                    } else {
                        status = cursor.getInt(columnIndex)
                    }
                    activity.runOnUiThread {
                        val dialog =
                            if (activity.supportFragmentManager.findFragmentByTag("downloading") == null) {
                                return@runOnUiThread
                            } else {
                                activity.supportFragmentManager.findFragmentByTag("downloading") as DownloadingProgress
                            }
                        dialog.setProgress(((currentSize * 100) / fileSize).toInt(), currentSize)
                        if (dialog.cancel) {
                            cancel = true
                        }
                    }
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        break
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        outputFile.delete()
                        return null
                    }
                    val sizeColumnIndex =
                        cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    if (sizeColumnIndex < 0) {
                        continue
                    } else {
                        currentSize = cursor.getLong(sizeColumnIndex)
                    }
                    if (currentSize > previousSize) {
                        previousSize = currentSize
                    } else {
                        Thread.sleep(100)
                    }
                }
                cursor.close()
            }
            activity.runOnUiThread {
                val dialog =
                    activity.supportFragmentManager.findFragmentByTag("downloading") as DownloadingProgress
                dialog.dismiss()

            }
            return File(outputFile.parent, outputFile.name)
        }

        fun isNetworkAvailable(context: Context): Boolean {
            return try {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetworkInfo = connectivityManager.activeNetwork
                activeNetworkInfo != null
            } catch (e: Exception) {
                false
            }
        }
    }
}