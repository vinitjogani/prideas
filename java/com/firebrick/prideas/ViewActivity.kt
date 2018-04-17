package com.firebrick.prideas

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_view.*
import kotlinx.android.synthetic.main.comment_item.view.*
import kotlinx.android.synthetic.main.titlebar.view.*
import java.text.SimpleDateFormat

class ViewActivity : AppCompatActivity() {

    lateinit var idea: Idea
    lateinit var author: PrideasUser
    val uid = FirebaseAuth.getInstance().uid
    var database = FirebaseDatabase.getInstance()
    var saved: Boolean = false
    var listed: Boolean = false
    val dateFormat = SimpleDateFormat("MMM dd, yyyy")

    override fun onCreate(savedInstanceState: Bundle?) {
        // Bootstrap the view
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)
        val filtered = intent.data?.path

        comments.layoutManager = LinearLayoutManager(this)
        comments.adapter = CommentsAdapter()

        if(filtered != null && filtered.isNotEmpty() && filtered.startsWith("/idea")) {
            if(uid == null) {
                startActivity(Intent(this, MainActivity::class.java))
            }
            val id = filtered.replace("/idea/", "")
            Helpers.getIdea(id) {
                if(it != null) { idea = it; updateUi() }
                else finish()
            }
        }
        else {
            idea = intent.getSerializableExtra("com.firebrick.Prideas.IDEA") as Idea
            updateUi()
        }

        // Update titlebar information
        titlebar.actionButton.setImageResource(R.drawable.ic_rate_review_black_24dp)
        titlebar.navButton.setImageResource(R.drawable.ic_arrow_back_black_24dp)
        titlebar.txtLogo.text = getString(R.string.view)
        titlebar.navButton.setOnClickListener({
            finish()
        })
        titlebar.actionButton.setOnClickListener({
            val reviewIntent = Intent(this, ReviewActivity::class.java)
            reviewIntent.putExtra("com.firebrick.Prideas.IDEA", idea)
            startActivityForResult(reviewIntent, 1)
        })

        // Update view programatically
        val profileListener = View.OnClickListener {
            val profileIntent = Intent(this@ViewActivity, SecondaryActivity::class.java)
            profileIntent.putExtra("com.firebrick.Prideas.USERID", idea.uid)
            startActivity(profileIntent)
        }
        cvAuthor.setOnClickListener(profileListener)
        txtAuthor.setOnClickListener(profileListener)
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            imgCover.requestLayout()
            val y = scrollView.scrollY
            val newHeight = imgCover.measuredWidth - y
            if (newHeight >= 0) { linearLayout.setPadding(0, y, 0, 0); imgCover.layoutParams.height = newHeight }
            else { imgCover.layoutParams.height = 0; linearLayout.setPadding(0, imgCover.measuredWidth, 0, 0) }

            imgCover.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        deleteButton.setOnClickListener({
            AlertDialog.Builder(this).setPositiveButton("Yes") { _, _ ->
                FirebaseStorage.getInstance().getReference(idea.id).delete()
                FirebaseDatabase.getInstance().getReference("ideas/${idea.id}").removeValue()
                finish()
            }.setNegativeButton("NO") { _, _ -> }.setTitle("Are you sure you want to delete your brilliant idea?").show()
        })
        editButton.setOnClickListener({
            val editIntent = Intent(this, AddActivity::class.java)
            editIntent.putExtra("com.firebrick.Prideas.IDEA", idea)
            startActivityForResult(editIntent, 1)
        })
        shareButton.setOnClickListener({
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, "http://prideas.co/idea/${idea.id}")
            shareIntent.type = "text/plain"
            startActivity(shareIntent)
        })
        saveButton.setOnClickListener({
            saved = if(!saved) {
                database.getReference("/users/$uid/saved/${idea.id}").setValue(1)
                Toast.makeText(this, "Idea added to your collections!", Toast.LENGTH_SHORT).show()
                true
            } else {
                database.getReference("/users/$uid/saved/${idea.id}").removeValue()
                Toast.makeText(this, "Idea removed from your collections!", Toast.LENGTH_SHORT).show()
                false
            }

            if(!saved) saveButton.setImageResource(R.drawable.ic_bookmark_border_black_24dp)
            else saveButton.setImageResource(R.drawable.ic_bookmark_black_24dp)
        })
        contributeButton.setOnClickListener({
            if(!listed) {
                AlertDialog.Builder(this).setPositiveButton("Yes") { _, _ ->
                    database.getReference("/lists/${idea.uid}/${idea.id}/$uid").setValue(FirebaseAuth.getInstance().currentUser!!.email!!)
                    Toast.makeText(this, "Your email was added to this idea's list!", Toast.LENGTH_SHORT).show()
                    listed = true
                    contributeButton.setImageResource(R.drawable.ic_email_black_24dp)
                }.setNegativeButton("NO") { _, _ -> }
                        .setTitle("""Are you sure you want to explore further possibilities with this idea?""")
                        .setMessage("""Your email will be shared with the author of the post, and they can contact you as they seem fit.

You can get a chance to be an alpha tester, development partner, or even an analyst to create your own app based on the idea. However, note that not all authors are open to take the idea further!""").show()

            } else {
                database.getReference("/lists/${idea.uid}/${idea.id}/$uid").removeValue()
                Toast.makeText(this, "Your email has been removed from this idea's list!", Toast.LENGTH_SHORT).show()
                listed = false
                contributeButton.setImageResource(R.drawable.ic_mail_outline_black_24dp)
            }
        })
        database.getReference("/lists/${idea.uid}/${idea.id}/$uid").addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) {}

            override fun onDataChange(p0: DataSnapshot?) {
                if(p0?.value != null) listed = true
                if(listed) contributeButton.setImageResource(R.drawable.ic_email_black_24dp)
            }

        })
        Helpers.getUser(uid!!) {
            saved = it!!.saved.containsKey(idea.id)
            if(!saved) saveButton.setImageResource(R.drawable.ic_bookmark_border_black_24dp)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if(intent == null) return
        val filtered = intent.data?.path

        if(filtered != null && filtered.isNotEmpty() && filtered.startsWith("/idea")) {
            if(uid == null) {
                startActivity(Intent(this, MainActivity::class.java))
            }
            val id = filtered.replace("/idea/", "")
            Helpers.getIdea(id) {
                if(it != null) { idea = it; updateUi() }
                else finish()
            }
        }
        else {
            idea = intent.getSerializableExtra("com.firebrick.Prideas.IDEA") as Idea
            updateUi()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1) {
            Helpers.getIdea(idea.id) {
                idea = it!!
                val counts = it.ratings.map { it.value }.toMutableList()
                idea.counts.count = counts.size
                idea.counts.rating1 = counts.map{it.rating1}.average()
                idea.counts.rating2 = counts.map{it.rating2}.average()
                idea.counts.rating3 = counts.map{it.rating3}.average()
                idea.counts.rating4 = counts.map{it.rating4}.average()
                idea.counts.overall = counts.map{it.overall}.average()
                runOnUiThread { updateUi() }
            }
        }
    }

    fun updateUi() {
        Helpers.getUser(idea.uid) {
            if(it != null) {
                txtAuthor.text = it.displayName
                Glide.with(this).load(it.image).into(imgAuthor)
            }
        }
        if (idea.uid == FirebaseAuth.getInstance().currentUser!!.uid) pnlAdmin.visibility = View.VISIBLE

        txtTitle.text = idea.title
        txtDescription.text = idea.description
        txtDate.text = "•  " + dateFormat.format(idea.createdAt)
        if(idea.description.trim() == "") txtDescription.visibility = View.GONE

        if(idea.tags.trim() == "") tagLayout.visibility = View.GONE
        tagLayout.removeAllViews()
        val headingTag = TextView(this, null, 0, R.style.TagTheme)
        headingTag.text = "Tags:"
        tagLayout.addView(headingTag)
        for(tag in idea.tags.split(",")) {
            if(tag.trim() != "") {
                val newTag = TextView(this, null, 0, R.style.TagTheme)
                newTag.text = tag.trim()
                tagLayout.addView(newTag, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
        }

        imgCover.post {
            imgCover.requestLayout()
            if (idea.image != "") Glide.with(this).load(idea.image).into(imgCover)
            else imgCover.setImageResource(R.drawable.default_image)
            imgCover.layoutParams.height = imgCover.measuredWidth
            imgCover.scaleType = ImageView.ScaleType.CENTER_CROP
        }
        label1.text = Helpers.getRatingTitle(1, idea.type)
        label2.text = Helpers.getRatingTitle(2, idea.type)
        label3.text = Helpers.getRatingTitle(3, idea.type)
        label4.text = Helpers.getRatingTitle(4, idea.type)

        val ratings = idea.counts
        overallRating.requestLayout()
        overallRating.progress = (ratings.overall.toFloat() * 10).toInt()
        txtOverallRating.text = Helpers.round(ratings.overall / 2, 1).toString()
        rating1.progress = (ratings.rating1 * 10).toInt()
        rating2.progress = (ratings.rating2 * 10).toInt()
        rating3.progress = (ratings.rating3 * 10).toInt()
        rating4.progress = (ratings.rating4 * 10).toInt()
        ratingsCount.text = idea.ratings.size.toString() + " rating(s)"

        comments.adapter.notifyDataSetChanged()
    }

    inner class CommentsAdapter : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): CommentViewHolder {
            return CommentViewHolder(
                    LayoutInflater.from(parent!!.context).inflate(R.layout.comment_item, parent, false),
                    parent.context
            )
        }

        override fun getItemCount(): Int {
            val size =  idea.ratings.filter { it.value.comment != "" }.size
            if(size == 0) commentsLabel.visibility = View.GONE
            else commentsLabel.visibility = View.VISIBLE
            return size
        }

        override fun onBindViewHolder(holder: CommentViewHolder?, position: Int) {
            val ratings = idea.ratings.filter { it.value.comment != "" }
            val userId = ratings.keys.toList()[position]
            val rating = ratings[userId]!!
            val view = holder!!.view
            Helpers.getUser(userId) {
                view.txtCommentAuthor.text = it!!.displayName
                Glide.with(holder.context).load(it.image).into(view.profilePicture)
                val userIntent = Intent(holder.context, SecondaryActivity::class.java)
                userIntent.putExtra("com.firebrick.Prideas.USERID", userId)
                view.txtCommentAuthor.setOnClickListener({startActivity(userIntent)})
                view.profilePicture.setOnClickListener({startActivity(userIntent)})
                view.commentText.text = rating.comment
                view.txtCommentDate.text = dateFormat.format(rating.createdAt)
                view.txtRating.text = Helpers.round(rating.overall / 2.0, 1).toString()
            }

        }

        inner class CommentViewHolder (val view: View, val context: Context) : RecyclerView.ViewHolder(view)
    }
}
