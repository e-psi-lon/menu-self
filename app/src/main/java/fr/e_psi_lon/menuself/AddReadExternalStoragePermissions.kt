package fr.e_psi_lon.menuself

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class AddReadExternalStoragePermissions: DialogFragment() {
    private lateinit var context: Context
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            context = it.applicationContext
            builder.setMessage(context.getString(R.string.ask_permissions, context.getString(R.string.read_external_storage)))
                .setPositiveButton(context.getString(R.string.ok)) { _, _ ->
                    activity?.requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
                }
                .setNegativeButton(context.getString(R.string.cancel)) { dialog, _ ->
                    dialog.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}