package com.firebrick.prideas

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_review.*
import kotlinx.android.synthetic.main.titlebar.view.*
import java.text.SimpleDateFormat
import java.util.*


class ReviewActivity : AppCompatActivity() {
    lateinit var idea : Idea
    var uid = FirebaseAuth.getInstance().uid
    val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)
        idea = intent.getSerializableExtra("com.firebrick.Prideas.IDEA")!! as Idea
        if(idea.ratings.contains(uid)) {
            val rating = idea.ratings.get(uid)
            if(rating != null) {
                rating1.rating = rating.rating1.toFloat() / 2
                rating2.rating = rating.rating2.toFloat() / 2
                rating3.rating = rating.rating3.toFloat() / 2
                rating4.rating = rating.rating4.toFloat() / 2
                comment.setText(rating.comment)
            }
        }

        label1.text = Helpers.getRatingTitle(1, idea.type)
        label2.text = Helpers.getRatingTitle(2, idea.type)
        label3.text = Helpers.getRatingTitle(3, idea.type)
        label4.text = Helpers.getRatingTitle(4, idea.type)

        // Update titlebar information
        titlebar.actionButton.setImageResource(R.drawable.ic_done_black_24dp)
        titlebar.navButton.setImageResource(R.drawable.ic_arrow_back_black_24dp)
        titlebar.txtLogo.text = getString(R.string.review_idea)
        titlebar.navButton.setOnClickListener({ finish() })
        titlebar.actionButton.setOnClickListener({
            val newRatings = listOf(
                    (rating1.rating * 2).toDouble(),
                    (rating2.rating * 2).toDouble(),
                    (rating3.rating * 2).toDouble(),
                    (rating4.rating * 2).toDouble())
            database.getReference("ideas/${idea.id}/ratings/$uid").setValue(
                    Rating(
                            newRatings[0],
                            newRatings[1],
                            newRatings[2],
                            newRatings[3],
                            newRatings.average(),
                            comment.text.toString().trim(),
                            Date(),
                            1
                    )
            ).addOnCompleteListener({
                finish()
            })
        })
    }
}
