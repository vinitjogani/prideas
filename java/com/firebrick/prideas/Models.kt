package com.firebrick.prideas

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.design.widget.NavigationView
import android.view.MenuItem
import android.widget.ArrayAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.Serializable
import android.support.v4.content.ContextCompat.startActivity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


data class Filter (
        var title: String = "",
        var tags: String = "",
        var type: Int = 3,
        var sort: Int = 0
)

data class Rating (
        var rating1: Double = 5.0,
        var rating2: Double = 5.0,
        var rating3: Double = 5.0,
        var rating4: Double = 5.0,
        var overall: Double = 5.0,
        var comment: String = "",
        var createdAt: Date = Date(2018, 1, 1),
        var count: Int = 1
) : Serializable

data class Idea (
        var id: String = "",
        var title: String="",
        var description: String="",
        var tags: String="",
        var uid: String="",
        var image: String="",
        var counts: Rating = Rating(),
        var ratings: HashMap<String, Rating> = hashMapOf(),
        var createdAt: Date = Date(2018, 1, 1),
        var type: Int = 0
) : Serializable

data class PrideasUser (
        var displayName: String = "",
        var image: String = "",
        var saved: HashMap<String, Int> = hashMapOf()
)

object Helpers {
    var database = FirebaseDatabase.getInstance()!!

    fun nav (context: Context, navId: Int, callback: (Boolean)->Unit) = NavigationView.OnNavigationItemSelectedListener {
        var nothing: Boolean = false
        when (it.itemId){
            navId -> nothing= true
            R.id.signout -> {
                FirebaseAuth.getInstance().signOut(); context.startActivity(Intent(context, MainActivity::class.java))
            }
            R.id.home -> (context as Activity).finish()
            R.id.profile_menu -> context.startActivity(Intent(context, SecondaryActivity::class.java))
            R.id.lists -> context.startActivity(Intent(context, ListsActivity::class.java))
            R.id.store -> {
                val uri = Uri.parse("market://details?id=" + context.packageName)
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    context.startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + context.packageName)))
                }
            }
            R.id.about -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://vinitjogani.github.io")))
        }
        callback(nothing)
        true
    }

    var ratingTitles = listOf(
            listOf("Implementation", "Concept", "Features", "Design"),
            listOf("Uniqueness", "Completeness", "Feasibility", "Necessity"),
            listOf("Severity", "Frequency", "Scale", "Unsolved"),
            listOf("Latest", "Popular")
    )

    fun getUser(uid: String, callback: (PrideasUser?) -> Unit) {
        database.getReference("users/$uid").addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) { callback(null) }
            override fun onDataChange(p0: DataSnapshot?) { callback(p0?.getValue(PrideasUser::class.java)) }
        })
    }

    fun getIdea(id: String, callback: (Idea?) -> Unit) {
        database.getReference("ideas/$id").addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onCancelled(p0: DatabaseError?) { callback(null) }
            override fun onDataChange(p0: DataSnapshot?) { callback(p0?.getValue(Idea::class.java)) }
        })
    }

    fun getRatingTitle(num: Int, type: Int) : String {
        return ratingTitles[type][num - 1]
    }


    fun round(value: Double, precision: Int): Double {
        val scale = Math.pow(10.0, precision.toDouble()).toInt()
        return Math.round(value * scale).toDouble() / scale
    }

}