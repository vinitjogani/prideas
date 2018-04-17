package com.firebrick.prideas

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_add.*
import android.content.Intent
import com.google.firebase.storage.FirebaseStorage
import android.view.View
import android.view.ViewManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.titlebar.view.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class AddActivity : AppCompatActivity() {
    // Instance variables
    val database = FirebaseDatabase.getInstance()
    var user = FirebaseAuth.getInstance().currentUser!!
    var key: String = ""
    var imgUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        // Bootstrap the view
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add)

        // Edit mode or create mode?
        val idea = intent.getSerializableExtra("com.firebrick.Prideas.IDEA") as Idea?
        if(idea != null) {
            key = idea.id
            txtTitle.setText(idea.title)
            txtDescription.setText(idea.description)
            txtTags.setText(idea.tags)
            ideaType.setSelection(idea.type)
            imgUrl = idea.image
            if (idea.image != "") Glide.with(this).load(idea.image).into(imgCover)
            else imgCover.setImageResource(R.drawable.default_image)
            (addOnly.parent!! as ViewManager).removeView(addOnly)
            titlebar.txtLogo.text = getString(R.string.edit_idea)
        }
        else {
            key = database.getReference("ideas").push().key
            titlebar.txtLogo.text = getString(R.string.new_idea)
        }

        // Update titlebar information
        titlebar.actionButton.setImageResource(R.drawable.ic_done_black_24dp)
        titlebar.navButton.setImageResource(R.drawable.ic_arrow_back_black_24dp)
        titlebar.navButton.setOnClickListener({ finish() })
        titlebar.actionButton.setOnClickListener({
            if(txtTitle.text.toString().trim().length < 3) {
                Toast.makeText(this, "The title should be at least three characters long", Toast.LENGTH_SHORT).show()
            }
            else {
                val newIdea = Idea(
                        id = key,
                        title = txtTitle.text.toString(),
                        uid = user.uid,
                        counts = idea?.counts ?: Rating(),
                        description = txtDescription.text.toString(),
                        image = imgUrl,
                        type = ideaType.selectedItemPosition,
                        tags = txtTags.text.toString(),
                        createdAt = Date(),
                        ratings = idea?.ratings ?: hashMapOf<String, Rating>()
                )
                database.getReference("/ideas").child(key).setValue(newIdea).addOnCompleteListener({
                    val viewIntent = Intent(this, ViewActivity::class.java)
                    viewIntent.putExtra("com.firebrick.Prideas.IDEA", newIdea)
                    startActivity(viewIntent)
                    finish()
                })
            }
        })

        // Update view settings programatically
        imgCover.post {
            imgCover.requestLayout()
            imgCover.layoutParams.height = imgCover.measuredWidth
        }
        imgCover.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_OPEN_DOCUMENT
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1)
        }
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            imgCover.requestLayout()
            val y = scrollView.scrollY
            val newHeight = imgCover.measuredWidth - y
            if (newHeight >= 0) { imgCover.layoutParams.height = newHeight; linearLayout.setPadding(0, y, 0, 0) }
            else { imgCover.layoutParams.height = 0; linearLayout.setPadding(0, imgCover.measuredWidth, 0, 0) }

            imgCover.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val spinnerAdapter = ArrayAdapter.createFromResource(this, R.array.idea_types, R.layout.support_simple_spinner_dropdown_item)
        ideaType.adapter = spinnerAdapter
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 1 && resultCode == RESULT_OK) {
            freezeUi()
            val inputStream = contentResolver.openInputStream(data!!.data)
            val fileRef = FirebaseStorage.getInstance().getReference("$key.png")
            fileRef.putBytes(inputStream.readBytes()).addOnCompleteListener({
                fileRef.downloadUrl.addOnCompleteListener({
                    imgUrl = URL(it.result.toString()).toString()
                    Glide.with(this).load(imgUrl).into(imgCover)
                    imgCover.scaleType = ImageView.ScaleType.CENTER_CROP
                    freezeUi(false)
                })
            })
        }
    }

    fun freezeUi(freeze: Boolean = true) {
        loadingButton.visibility = if(freeze) View.VISIBLE else View.INVISIBLE
        drawerLayout.isEnabled = !freeze
        titlebar.actionButton.isEnabled = !freeze
    }
}