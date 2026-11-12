package com.example.academix

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Base64
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.FileProvider
import java.io.File

/**
 * Helps store/retrieve notice attachments as Base64 inside Firestore,
 * since Firebase Storage requires the paid Blaze plan.
 */
object AttachmentHelper {

    fun decodeBase64(data: String): Bitmap? {
        return try {
            val bytes = Base64.decode(data, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    fun showImageFullScreen(context: Context, data: String) {
        val bitmap = decodeBase64(data) ?: return

        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(context)
        imageView.setImageBitmap(bitmap)
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setBackgroundColor(Color.BLACK)
        imageView.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(
            imageView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        dialog.show()
    }

    fun openFile(context: Context, data: String, name: String): Boolean {
        return try {
            val bytes = Base64.decode(data, Base64.NO_WRAP)
            val dir = File(context.cacheDir, "attachments")
            dir.mkdirs()
            val file = File(dir, name.ifBlank { "attachment" })
            file.writeBytes(bytes)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, guessMimeType(name))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun guessMimeType(name: String): String = when {
        name.endsWith(".pdf", true) -> "application/pdf"
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".doc", true) -> "application/msword"
        name.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        else -> "*/*"
    }
}
