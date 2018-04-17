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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_item.view.*
import kotlinx.android.synthetic.main.titlebar.view.*

class MainActivity : AppCompatActivity() {
    // Instance variables
    var database : FirebaseDatabase = FirebaseDatabase.getInstance()
    var auth : FirebaseAuth = FirebaseAuth.getInstance()
    var lastId: String? = ""
    var lastNum: Double = 10.0
    var filter = Filter()

    // Firebase event listener
    private var listener = object: ChildEventListener {
        override fun onCancelled(p0: DatabaseError?){}
        override fun onChildMoved(p0: DataSnapshot?, p1: String?) {}

        override fun onChildChanged(p0: DataSnapshot?, p1: String?) {
            var changed = false
            for (item in (main_list.adapter as IdeasAdapter).ideas) {
                val idea = p0!!.getValue(Idea::class.java)!!
                if (item.id == idea.id) {
                    item.image = idea.image
                    item.description = idea.description
                    item.title = idea.title
                    item.ratings = idea.ratings
                    item.tags = idea.tags
                    item.counts = idea.counts
                    changed = true
                    break
                }
            }
            if (changed) {
                updateAdapter()
            }
        }

        override fun onChildAdded(p0: DataSnapshot?, p1: String?) {
            try {
                val newIdea = p0!!.getValue(Idea::class.java)!!
                val adapter = (main_list.adapter as IdeasAdapter)
                if (adapter.ideas.any { it.id == newIdea.id }) return
                adapter.ideas.add(newIdea)
                updateAdapter()
                if (lastId == "" || lastId == null) {
                    lastId = p0.key; lastNum = newIdea.counts.overall
                }
            } catch(e : Exception) {}
        }

        override fun onChildRemoved(p0: DataSnapshot?) {
            (main_list.adapter as IdeasAdapter).ideas.remove(p0!!.getValue(Idea::class.java)!!)
            updateAdapter()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 1) {
            if(resultCode != RESULT_OK) finish()
            else loadIdeas()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Bootstrap the view
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try { database.setPersistenceEnabled(true) } catch (e: Exception) {}

        // Authenticate user
        if(auth.currentUser == null) signIn()
        else loadIdeas()

        // Update titlebar information
        titlebar.actionButton.setImageResource(R.drawable.ic_search_black_24dp)
        titlebar.navButton.setImageResource(R.drawable.ic_menu_black_24dp)
        titlebar.txtLogo.text = getString(R.string.app_name)
        titlebar.actionButton.setOnClickListener({ searchbox.visibility = View.VISIBLE; main_list.visibility = View.GONE })
        titlebar.navButton.setOnClickListener({ drawerLayout.openDrawer(Gravity.START) })
        addButton.setOnClickListener({ startActivity(Intent(this, AddActivity::class.java)) })

        //Setup searchbar
        searchbar.txtLogo.text = getString(R.string.search)
        searchbar.actionButton.setOnClickListener({
            val type = when(searchType.selectedItemPosition) {
                0 -> 3
                else -> searchType.selectedItemPosition - 1
            }
            val sort = when(searchSort.selectedItemPosition) {
                0 -> 0
                1 -> 5
                else -> searchSort.selectedItemPosition - 1
            }
            updateFilter(Filter( searchTitle.text.toString(), searchTags.text.toString(), type, sort))
            searchbox.visibility = View.GONE
            main_list.visibility = View.VISIBLE
        })
        searchbar.navButton.setOnClickListener({ searchbox.visibility = View.GONE; main_list.visibility = View.VISIBLE })

        searchType.adapter = ArrayAdapter.createFromResource(this, R.array.search_types, R.layout.support_simple_spinner_dropdown_item)
        searchSort.adapter = ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, Helpers.ratingTitles[3].toMutableList())
        searchType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) { }
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                searchSort.requestLayout()
                val newSortAdapter = searchSort.adapter as ArrayAdapter<String>
                newSortAdapter.clear()
                if(p2 > 0) newSortAdapter.addAll(Helpers.ratingTitles[3] + Helpers.ratingTitles[p2 - 1])
                else newSortAdapter.addAll(Helpers.ratingTitles[3])
            }

        }

        // Setup navigation drawer
        navigation.setNavigationItemSelectedListener (Helpers.nav(this, R.id.home){drawerLayout.closeDrawer(Gravity.START)})

        // Setup main recycler view
        main_list.layoutManager = LinearLayoutManager(this)
        main_list.adapter = IdeasAdapter()
        main_list.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val manager = main_list.layoutManager as LinearLayoutManager
                 if ((manager.childCount + manager.findFirstVisibleItemPosition()) >= manager.itemCount - 2) {
                    loadIdeas()
                }
            }
        })


    }

    fun loadIdeas() {
        if(lastId == null) return
        var query : Query = database.getReference("ideas")

        if(filter.sort == 0) {
            query = when(lastId) {
                "" -> query.orderByKey().limitToLast(10)
                else -> query.orderByKey().endAt(lastId).limitToLast(10)
            }
            lastId = null
        }
        else {
            val sortBy = when(filter.sort) {
                1 -> "rating1"
                2 -> "rating2"
                3 -> "rating3"
                4 -> "rating4"
                else -> "overall"
            }
            query = when(lastId) {
                "" -> query.orderByChild("counts/$sortBy").endAt(lastNum).limitToLast(10)
                else -> query.orderByChild("counts/$sortBy").endAt(lastNum, lastId).limitToLast(10)
            }
        }

        query.addChildEventListener(listener)
    }

    fun updateAdapter() {
        val adapter = (main_list.adapter as IdeasAdapter)
        if(filter.sort != 0) {
            adapter.ideas.sortBy {
                when (filter.sort) {
                    1 -> -it.counts.rating1.toDouble()
                    2 -> -it.counts.rating2.toDouble()
                    3 -> -it.counts.rating3.toDouble()
                    4 -> -it.counts.rating4.toDouble()
                    else -> -it.counts.overall
                }
            }
        }
        adapter.filtered = adapter.ideas.filter { predicate(it) }.toMutableList()
        main_list.adapter.notifyDataSetChanged()
    }

    private fun signIn() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(listOf(
                        AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build(),
                        AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()
                )).setIsSmartLockEnabled(false).setLogo(R.drawable.logo)
                .setAllowNewEmailAccounts(true).setTheme(R.style.AppTheme).build(),
                1)
    }

    private fun updateFilter(newFilter: Filter) {
        filter.title = newFilter.title
        filter.tags = newFilter.tags
        filter.type = newFilter.type
        if(newFilter.sort != filter.sort) {
            val adapter = main_list.adapter as IdeasAdapter
            adapter.ideas = mutableListOf()
            lastId = ""
            lastNum = 10.0
            filter = newFilter
            loadIdeas()
        }
        else updateAdapter()
    }

    private fun predicate(idea: Idea) : Boolean {
        if(idea.title.toLowerCase().contains(filter.title.toLowerCase())
                && (idea.type == filter.type || filter.type == 3)) {
            val tags = idea.tags.toLowerCase().split(",")
            return filter.tags.trim() == "" || filter.tags.toLowerCase().split(",").all { tags.contains(it) }
        }
        return false
    }

    inner class IdeasAdapter : RecyclerView.Adapter<IdeasAdapter.IdeasHolder>() {
        var ideas = mutableListOf<Idea>()
        var filtered = mutableListOf<Idea>()

        override fun onBindViewHolder(holder: IdeasHolder?, position: Int) {
            val idea = filtered[position]
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
                viewIntent.putExtra("com.firebrick.Prideas.IDEA", filtered[position])
                startActivity(viewIntent)
            })
        }

        override fun getItemCount(): Int {
            if(filtered.size == 0) nothingHere.visibility = View.VISIBLE
            else nothingHere.visibility = View.GONE
            return filtered.size
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): IdeasHolder {
            val newView = LayoutInflater.from(parent!!.context).inflate(R.layout.main_item, parent, false)
            return IdeasHolder(newView, parent.context)
        }

        inner class IdeasHolder(var view: View, var context: Context) : RecyclerView.ViewHolder(view)
    }

}
