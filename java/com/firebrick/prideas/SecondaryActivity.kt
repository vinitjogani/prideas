package com.firebrick.prideas

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_secondary.*
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.main_item.view.*


class SecondaryActivity : AppCompatActivity() {

    val database = FirebaseDatabase.getInstance()
    var uid = FirebaseAuth.getInstance().uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secondary)

        if(uid == null) startActivity(Intent(this, MainActivity::class.java))

        val request = intent.getStringExtra("com.firebrick.Prideas.USERID")
        var id = request?:FirebaseAuth.getInstance().uid!!
        val filtered = intent.data?.path
        if(filtered != null && filtered.isNotEmpty() && filtered.startsWith("/user")) {
            id = filtered.replace("/user/","")
        }

        if(id == uid) {
            navButton.setImageResource(R.drawable.ic_menu_black_24dp)
            navButton.setOnClickListener({ drawerLayout.openDrawer(Gravity.START) })
        }
        else {
            navButton.setImageResource(R.drawable.ic_arrow_back_black_24dp)
            navButton.setOnClickListener({ finish() })
        }

        updateData(id)

        //Tab 1
        var spec = tabHost.newTabSpec("1")
        spec.setContent(R.id.tab1)
        spec.setIndicator("Profile")
        tabHost.addTab(spec)

        //Tab 2
        spec = tabHost.newTabSpec("2")
        spec.setContent(R.id.tab2)
        spec.setIndicator("Saved")
        tabHost.addTab(spec)

        profileList.layoutManager = LinearLayoutManager(this)
        profileList.adapter = IdeasAdapter(1)

        savedList.layoutManager = LinearLayoutManager(this)
        savedList.adapter = IdeasAdapter(2)

        navigation.setNavigationItemSelectedListener (Helpers.nav(this, R.id.profile_menu){
            if(!it) finish()
            else drawerLayout.closeDrawer(Gravity.START)
        })
        picture.setOnClickListener({
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_TEXT, "http://prideas.co/user/$id")
            shareIntent.type = "text/plain"
            startActivity(shareIntent)
        })
    }

    override fun onNewIntent(intent: Intent?) {
        if(uid == null) startActivity(Intent(this, MainActivity::class.java))
        if (intent == null) return

        val request = intent.getStringExtra("com.firebrick.Prideas.USERID")
        var id = request?:FirebaseAuth.getInstance().uid!!
        val filtered = intent.data?.path
        if(filtered != null && filtered.isNotEmpty() && filtered.startsWith("/user")) {
            id = filtered.replace("/user/","")
        }

        if(id == uid) {
            navButton.setImageResource(R.drawable.ic_menu_black_24dp)
            navButton.setOnClickListener({ drawerLayout.openDrawer(Gravity.START) })
        }
        else {
            navButton.setImageResource(R.drawable.ic_arrow_back_black_24dp)
            navButton.setOnClickListener({ finish() })
        }

        updateData(id)
    }

    fun updateData(id: String) {
        Helpers.getUser(id) {
            if(it == null) finish()
            Glide.with(this).load(it!!.image).into(picture)
            txtLogo.text = it.displayName
        }
        tabHost.setup()

        database.getReference("/users/$id/saved").addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {}
            override fun onDataChange(p0: DataSnapshot?) {
                if(p0 == null) return
                for (idea in p0.children) {
                    Helpers.getIdea(idea.key) {
                        if(it != null) {
                            (savedList.adapter as IdeasAdapter).ideas.add(it)
                            savedList.adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        })

        database.getReference("/ideas").orderByChild("uid").equalTo(id).addChildEventListener(object: ChildEventListener {
            override fun onChildMoved(p0: DataSnapshot?, p1: String?) {}

            override fun onChildChanged(p0: DataSnapshot?, p1: String?) {}

            override fun onChildAdded(p0: DataSnapshot?, p1: String?) {
                if(p0 == null) return
                val idea = p0.getValue(Idea::class.java)!!
                (profileList.adapter as IdeasAdapter).ideas.add(idea)
                profileList.adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(p0: DataSnapshot?) {}

            override fun onCancelled(p0: DatabaseError?) {}
        })

    }

    inner class IdeasAdapter(val tab: Int) : RecyclerView.Adapter<IdeasAdapter.IdeasHolder>() {
        var ideas = mutableListOf<Idea>()

        override fun onBindViewHolder(holder: IdeasHolder?, position: Int) {
            val idea = ideas[position]
            val view = holder!!.view

            Helpers.getUser(idea.uid) {
                view.requestLayout()
                view.txtAuthor.text = it?.displayName
            }
            view.txtTitle.text = idea.title
            view.txtRating.text = Helpers.round(idea.counts.overall / 2, 1).toString()
            view.imgCover.post({
                if (idea.image != "") Glide.with(holder.context).load(idea.image).into(view.imgCover)
                else view.imgCover.setImageResource(R.drawable.default_image)
                view.imgCover.layoutParams.height = view.imgCover.measuredWidth
                view.imgCover.scaleType = ImageView.ScaleType.CENTER_CROP
            })
            view.setOnClickListener({
                val viewIntent = Intent(holder.context, ViewActivity::class.java)
                viewIntent.putExtra("com.firebrick.Prideas.IDEA", ideas[position])
                startActivity(viewIntent)
            })
        }

        override fun getItemCount(): Int {
            if (tab == 1) {
                if (ideas.size == 0) nothingHere.visibility = View.VISIBLE
                else nothingHere.visibility = View.GONE
            }
            else {
                if (ideas.size == 0) nothingHere2.visibility = View.VISIBLE
                else nothingHere2.visibility = View.GONE
            }
            return ideas.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): IdeasHolder {
            val newView = LayoutInflater.from(parent!!.context).inflate(R.layout.main_item, parent, false)
            return IdeasHolder(newView, parent.context)
        }

        inner class IdeasHolder(var view: View, var context: Context) : RecyclerView.ViewHolder(view)
    }
}
