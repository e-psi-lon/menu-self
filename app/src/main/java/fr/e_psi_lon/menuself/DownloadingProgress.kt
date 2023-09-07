package fr.e_psi_lon.menuself

import android.app.AlertDialog
import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.io.File


class DownloadingProgress : DialogFragment() {
    var cancel: Boolean = false
    private lateinit var context: Context
    private lateinit var url: String
    private lateinit var outputFile: File
    private var fileSize: Long = 0
    private lateinit var downloadManager: DownloadManager
    private lateinit var titleDownloading: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var downloadId: String


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            context = it.applicationContext
            builder.setView(R.layout.downloading_progress)
            // Il faut un bouton cancel
            builder.setCancelable(true)
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun setElements() {
        while (dialog == null) {
            Thread.sleep(100)
        }
        titleDownloading = dialog!!.findViewById(R.id.titleDownloading)
        progressBar = dialog!!.findViewById(R.id.progressBar)
    }

    override fun onCancel(dialog: DialogInterface) {
        cancel = true
        downloadManager.remove(downloadId.toLong())
        outputFile.delete()
        super.onCancel(dialog)
    }

    fun setUrl(url: String) {
        this.url = url
    }

    fun setOutputFile(outputFile: File) {
        this.outputFile = outputFile
    }

    fun setFileSize(fileSize: Long) {
        this.fileSize = fileSize
    }

    fun setDownloadManager(downloadManager: DownloadManager) {
        this.downloadManager = downloadManager
    }

    fun setDownloadId(downloadId: String) {
        this.downloadId = downloadId
    }

    fun setProgress(progress: Int, downloadedSize: Long) {
        if (!::progressBar.isInitialized || !::titleDownloading.isInitialized) {
            setElements()
            return
        }
        progressBar.progress = progress
        titleDownloading.text = context.getString(
            R.string.downloading_progress,
            outputFile.name,
            Request.formatSize(downloadedSize),
            Request.formatSize(fileSize)
        )
    }

}