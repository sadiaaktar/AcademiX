package com.example.academix

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class PostNoticeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var etTitle: TextInputEditText
    private lateinit var etContent: TextInputEditText
    private lateinit var categoryContainer: LinearLayout
    private lateinit var sectionContainer: LinearLayout
    private lateinit var btnPost: MaterialButton

    private lateinit var attachmentPreview: LinearLayout
    private lateinit var ivAttachmentPreview: ImageView
    private lateinit var tvAttachmentName: TextView
    private lateinit var btnRemoveAttachment: TextView

    private var selectedCategory: String = NoticeCategories.GENERAL
    private var selectedSection: String = "All"
    private var posterName: String = ""
    private var editingNoticeId: String? = null

    private var attachmentData: String = ""
    private var attachmentName: String = ""
    private var attachmentType: String = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) attachFile(uri)
    }

    private val pickDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) attachFile(uri)
    }

    companion object {
        private const val ARG_NOTICE_ID = "notice_id"
        private const val ARG_TITLE = "title"
        private const val ARG_CONTENT = "content"
        private const val ARG_CATEGORY = "category"
        private const val ARG_SECTION = "section"
        private const val ARG_ATTACHMENT_DATA = "attachment_data"
        private const val ARG_ATTACHMENT_NAME = "attachment_name"
        private const val ARG_ATTACHMENT_TYPE = "attachment_type"

        fun editInstance(notice: Notice): PostNoticeFragment =
            PostNoticeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_NOTICE_ID, notice.id)
                    putString(ARG_TITLE, notice.title)
                    putString(ARG_CONTENT, notice.content)
                    putString(ARG_CATEGORY, notice.category)
                    putString(ARG_SECTION, notice.section)
                    putString(ARG_ATTACHMENT_DATA, notice.attachmentData)
                    putString(ARG_ATTACHMENT_NAME, notice.attachmentName)
                    putString(ARG_ATTACHMENT_TYPE, notice.attachmentType)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_post_notice, container, false)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        etTitle = view.findViewById(R.id.etTitle)
        etContent = view.findViewById(R.id.etContent)
        categoryContainer = view.findViewById(R.id.categoryContainer)
        sectionContainer = view.findViewById(R.id.sectionContainer)
        btnPost = view.findViewById(R.id.btnPost)

        attachmentPreview = view.findViewById(R.id.attachmentPreview)
        ivAttachmentPreview = view.findViewById(R.id.ivAttachmentPreview)
        tvAttachmentName = view.findViewById(R.id.tvAttachmentName)
        btnRemoveAttachment = view.findViewById(R.id.btnRemoveAttachment)

        view.findViewById<TextView>(R.id.tvBack).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_notice)
        }

        view.findViewById<View>(R.id.btnAttachPhoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        view.findViewById<View>(R.id.btnAttachFile).setOnClickListener {
            pickDocumentLauncher.launch(arrayOf("*/*"))
        }
        btnRemoveAttachment.setOnClickListener {
            clearAttachment()
        }

        readEditArgs()
        buildCategoryChips()
        buildSectionChips()
        loadPosterName()

        btnPost.setOnClickListener {
            postNotice()
        }

        return view
    }

    private fun readEditArgs() {
        val args = arguments ?: return
        val noticeId = args.getString(ARG_NOTICE_ID) ?: return

        editingNoticeId = noticeId
        etTitle.setText(args.getString(ARG_TITLE) ?: "")
        etContent.setText(args.getString(ARG_CONTENT) ?: "")
        selectedCategory = args.getString(ARG_CATEGORY) ?: NoticeCategories.GENERAL
        selectedSection = args.getString(ARG_SECTION) ?: "All"
        btnPost.text = "Update Notice"

        attachmentData = args.getString(ARG_ATTACHMENT_DATA) ?: ""
        attachmentName = args.getString(ARG_ATTACHMENT_NAME) ?: ""
        attachmentType = args.getString(ARG_ATTACHMENT_TYPE) ?: ""
        if (attachmentData.isNotEmpty()) {
            attachmentPreview.visibility = View.VISIBLE
            tvAttachmentName.text = attachmentName.ifBlank { "Attachment" }
            btnRemoveAttachment.visibility = View.VISIBLE

            if (attachmentType == "image") {
                val bitmap = AttachmentHelper.decodeBase64(attachmentData)
                if (bitmap != null) {
                    ivAttachmentPreview.visibility = View.VISIBLE
                    ivAttachmentPreview.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun buildCategoryChips() {
        categoryContainer.removeAllViews()

        for (category in NoticeCategories.all) {
            val chip = TextView(requireContext())
            chip.text = category
            chip.tag = category
            chip.textSize = 12f
            chip.gravity = Gravity.CENTER
            chip.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip)
            chip.setPadding(dp(14), dp(8), dp(14), dp(8))

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(8)
            chip.layoutParams = lp

            chip.setOnClickListener { selectCategory(category) }
            categoryContainer.addView(chip)
        }

        selectCategory(selectedCategory)
    }

    private fun selectCategory(category: String) {
        selectedCategory = category

        for (i in 0 until categoryContainer.childCount) {
            val chip = categoryContainer.getChildAt(i) as TextView
            val isSelected = chip.tag == category
            if (isSelected) {
                chip.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.brand_purple)
                )
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
                chip.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
            }
        }
    }

    private fun buildSectionChips() {
        sectionContainer.removeAllViews()

        val options = listOf("All") + RoutineCatalog.teacherSections()

        for (option in options) {
            val label = if (option == "All") "All sections" else "Section $option"

            val chip = TextView(requireContext())
            chip.text = label
            chip.tag = option
            chip.textSize = 12f
            chip.gravity = Gravity.CENTER
            chip.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_chip)
            chip.setPadding(dp(14), dp(8), dp(14), dp(8))

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = dp(8)
            chip.layoutParams = lp

            chip.setOnClickListener { selectSection(option) }
            sectionContainer.addView(chip)
        }

        selectSection(selectedSection)
    }

    private fun selectSection(section: String) {
        selectedSection = section

        for (i in 0 until sectionContainer.childCount) {
            val chip = sectionContainer.getChildAt(i) as TextView
            val isSelected = chip.tag == section
            if (isSelected) {
                chip.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.brand_purple)
                )
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                chip.backgroundTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.white)
                )
                chip.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.text_secondary)
                )
            }
        }
    }

    private fun loadPosterName() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                posterName = doc.getString("fullName") ?: "Teacher"
            }
    }

    private fun attachFile(uri: Uri) {
        val fileName = queryFileName(uri)
        val mimeType = requireContext().contentResolver.getType(uri) ?: ""
        val isImage = mimeType.startsWith("image/")

        val base64 = if (isImage) encodeImage(uri) else encodeFile(uri)

        if (base64 == null) {
            Toast.makeText(requireContext(), "Cannot attach this file", Toast.LENGTH_LONG).show()
            return
        }

        attachmentData = base64
        attachmentName = fileName
        attachmentType = if (isImage) "image" else "file"

        attachmentPreview.visibility = View.VISIBLE
        tvAttachmentName.text = fileName
        btnRemoveAttachment.visibility = View.VISIBLE

        if (isImage) {
            val bitmap = AttachmentHelper.decodeBase64(base64)
            if (bitmap != null) {
                ivAttachmentPreview.visibility = View.VISIBLE
                ivAttachmentPreview.setImageBitmap(bitmap)
            }
        } else {
            ivAttachmentPreview.visibility = View.GONE
        }
    }

    private fun encodeImage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            val scaled = scaleBitmap(bitmap, 1024)
            val baos = ByteArrayOutputStream()
            var quality = 75
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            while (baos.size() > 650 * 1024 && quality > 30) {
                baos.reset()
                quality -= 10
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            }
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun encodeFile(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.use { it.readBytes() }
            if (bytes.size > 700 * 1024) {
                Toast.makeText(
                    requireContext(),
                    "File too large (max ~700KB)",
                    Toast.LENGTH_LONG
                ).show()
                return null
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val scale = maxSize.toFloat() / maxOf(width, height)
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun queryFileName(uri: Uri): String {
        var name = "attachment"
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) name = it.getString(idx)
            }
        }
        return name
    }

    private fun clearAttachment() {
        attachmentData = ""
        attachmentName = ""
        attachmentType = ""
        attachmentPreview.visibility = View.GONE
        ivAttachmentPreview.visibility = View.GONE
        ivAttachmentPreview.setImageDrawable(null)
    }

    private fun postNotice() {
        val title = etTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isEmpty()) {
            etTitle.error = "Enter a title"
            etTitle.requestFocus()
            return
        }

        if (content.isEmpty()) {
            etContent.error = "Enter a description"
            etContent.requestFocus()
            return
        }

        val uid = auth.currentUser?.uid ?: return

        btnPost.isEnabled = false

        val notice = hashMapOf<String, Any>(
            "title" to title,
            "content" to content,
            "category" to selectedCategory,
            "section" to selectedSection,
            "postedByUid" to uid,
            "postedByName" to posterName.ifBlank { "Teacher" },
            "timestamp" to FieldValue.serverTimestamp()
        )

        if (attachmentData.isNotEmpty()) {
            notice["attachmentData"] = attachmentData
            notice["attachmentName"] = attachmentName
            notice["attachmentType"] = attachmentType
        }

        val isEditing = editingNoticeId != null
        val successMessage = if (isEditing) "Notice updated" else "Notice posted"

        val writeTask = if (isEditing) {
            db.collection("notices").document(editingNoticeId!!).set(notice)
        } else {
            db.collection("notices").add(notice)
        }

        writeTask
            .addOnSuccessListener {
                btnPost.isEnabled = true
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
                (activity as? MainActivity)?.selectTab(R.id.nav_notice)
            }
            .addOnFailureListener { e ->
                btnPost.isEnabled = true
                Toast.makeText(
                    requireContext(),
                    "Failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
