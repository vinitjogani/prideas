package com.firebrick.prideas

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_lists.*
import kotlinx.android.synthetic.main.list_item.view.*
import kotlinx.android.synthetic.main.titlebar.view.*

class EmailActivity : AppCompatActivity() {
    val database = FirebaseDatabase.getInstance()
    val uid = FirebaseAuth.getInstance().uid!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lists)

        val id = intent.getStringExtra("com.firebrick.Prideas.LIST")?:""
        if(id == "") finish()

        main_list.layoutManager = LinearLayoutManager(this)
        main_list.adapter = EmailAdapter()

        database.getReference("/lists/$uid/$id").addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError?) {}
            override fun onDataChange(p0: DataSnapshot?) {
                if(p0 == null) return
                for (email in p0.children) {
                    (main_list.adapter as EmailAdapter).emails[email.key] = email.value as String
                    main_list.adapter.notifyDataSetChanged()
                }
            }
        })

        titlebar.actionButton.setImageResource(R.drawable.ic_email_black_24dp)
        titlebar.actionButton.setOnClickListener({
            val emailIntent = Intent()
            emailIntent.action = Intent.ACTION_SENDTO
            emailIntent.data = Uri.parse("mailto:")
            emailIntent.putExtra(Intent.EXTRA_BCC, (main_list.adapter as EmailAdapter).emails.values.toTypedArray())
            startActivity(emailIntent)
        })
        titlebar.navButton.setOnClickListener({ finish() })
        titlebar.txtLogo.text = "View list"
    }

    inner class EmailAdapter : RecyclerView.Adapter<EmailAdapter.IdeasHolder>() {
        var emails = hashMapOf<String, String>()

        override fun onBindViewHolder(holder: IdeasHolder?, position: Int) {
            val view = holder!!.view
            view.txtTitle.text = emails.values.toList()[position]
            Helpers.getUser(emails.keys.toList()[position]) {
                view.label.text = it?.displayName?:"Anonymous"
            }
            view.setOnClickListener({
                val viewIntent = Intent(holder.context, SecondaryActivity::class.java)
                viewIntent.putExtra("com.firebrick.Prideas.USERID", emails.keys.toList()[position])
                startActivity(viewIntent)
            })
        }

        override fun getItemCount(): Int {
            if(emails.size == 0) nothingHere.visibility = View.VISIBLE
            else nothingHere.visibility = View.GONE
            return emails.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): IdeasHolder {
            val newView = LayoutInflater.from(parent!!.context).inflate(R.layout.list_item, parent, false)
            return IdeasHolder(newView, parent.context)
        }

        inner class IdeasHolder(var view: View, var context: Context) : RecyclerView.ViewHolder(view)
    }
}
