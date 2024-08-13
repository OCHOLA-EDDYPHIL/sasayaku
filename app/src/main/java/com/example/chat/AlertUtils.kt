package com.example.chat

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

object AlertUtils {
    fun showAlert(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "OK",
        positiveButtonAction: ((DialogInterface, Int) -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton(positiveButtonText) { dialog, which ->
            positiveButtonAction?.invoke(dialog, which) ?: dialog.dismiss()
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }
}