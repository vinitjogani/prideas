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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_lists.*
import kotlinx.android.synthetic.main.list_item.view.*
import kotlinx.android.synthetic.main.titlebar.view.*

class ListsActivity : AppCompatActivity() {

    val database = FirebaseDatabase.getInstance()
    val uid = FirebaseAuth.getInstance().uid!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lists)

        main_list.layoutManager = LinearLayoutManager(this)
        main_list.adapter = IdeasAdapter()

        database.getReference("/ideas").orderByChild("uid").equalTo(uid).addChildEventListener(object: ChildEventListener {
            override fun onChildMoved(p0: DataSnapshot?, p1: String?) { }

            override fun onChildChanged(p0: DataSnapshot?, p1: String?) { }

            override fun onChildAdded(p0: DataSnapshot?, p1: String?) {
                if(p0 == null) return
                val idea = p0.getValue(Idea::class.java)!!
                (main_list.adapter as IdeasAdapter).idea_titles[idea.id] = idea.title
                main_list.adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(p0: DataSnapshot?) {}

            override fun onCancelled(p0: DatabaseError?) {}

        })

        titlebar.txtLogo.text = "Mailing lists"
        titlebar.navButton.setImageResource(R.drawable.ic_menu_black_24dp)
        titlebar.actionButton.visibility = View.GONE
        titlebar.navButton.setOnClickListener({ drawerLayout.openDrawer(Gravity.START) })
        navigation.setNavigationItemSelectedListener (Helpers.nav(this, R.id.lists){
            if(!it) finish()
            else drawerLayout.closeDrawer(Gravity.START)
        })
    }

    inner class IdeasAdapter : RecyclerView.Adapter<IdeasAdapter.IdeasHolder>() {
        var idea_titles = hashMapOf<String, String>()

        override fun onBindViewHolder(holder: IdeasHolder?, position: Int) {
            val view = holder!!.view
            view.txtTitle.text = idea_titles.values.toList()[position]
            view.setOnClickListener({
                val viewIntent = Intent(holder.context, EmailActivity::class.java)
                viewIntent.putExtra("com.firebrick.Prideas.LIST", idea_titles.keys.toList()[position])
                startActivity(viewIntent)
            })
        }

        override fun getItemCount(): Int {
            if(idea_titles.size == 0) nothingHere.visibility = View.VISIBLE
            else nothingHere.visibility = View.GONE
            return idea_titles.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): IdeasHolder {
            val newView = LayoutInflater.from(parent!!.context).inflate(R.layout.list_item, parent, false)
            return IdeasHolder(newView, parent.context)
        }

        inner class IdeasHolder(var view: View, var context: Context) : RecyclerView.ViewHolder(view)
    }
}
