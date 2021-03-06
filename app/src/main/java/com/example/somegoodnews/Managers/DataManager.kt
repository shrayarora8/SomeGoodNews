package com.example.somegoodnews.Managers

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.somegoodnews.Listeners.OnUpdateLikes
import com.example.somegoodnews.Listeners.OnUpdateListListener
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

class DataManager {
    companion object {
        private const val TAG = "DATAMANAGER"
    }
    var articles: MutableList<NewsArticle> = mutableListOf()
    var likedArticlePos: MutableList<String> = mutableListOf()
    var likedArticles: MutableList<NewsArticle> = mutableListOf()
    var userArticles: MutableList<String> = mutableListOf()
    private lateinit var database:FirebaseDatabase
    var onUpdateListListener: OnUpdateListListener? = null
    var onUpdateLikes:OnUpdateLikes? = null

    fun fetchData() {
        database = Firebase.database
        val myRef = database.getReference("articles")

        // Read from the database
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue<ArrayList<NewsArticle>>()
                value?.let {
                    // Drop 1st cause it's always null for some reason
                    articles = value.toMutableList()
                }
                onUpdateListListener?.onUpdateList()
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.i(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    fun fetchLikedData(user: String?): MutableList<NewsArticle>? {
        if(user == null) {
            Log.i(TAG, "There is no user logged in")
            likedArticles =  mutableListOf()
            likedArticlePos =  mutableListOf()
            return null
        } else {
            database = Firebase.database
            val myRef = database.getReference("users")
                .child(user.replace(".", ""))
                .child("likedArticles")
            // Read from the database
            myRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    val value = dataSnapshot.getValue<ArrayList<String>>()
                    val liked = mutableListOf<NewsArticle>()
                    value?.let { pos ->
                        // array of positions of articles in the main article list
                        likedArticlePos  = pos.toMutableList()
                        if(articles.size > 0) {
                            pos.forEach {
                                liked.add(articles[it.toInt()])
                            }
                        }
                        likedArticles = liked
                    }
                    onUpdateLikes?.updateLikesList()
                    onUpdateListListener?.onUpdateList()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Failed to read value
                    Log.i(TAG, "Failed to get liked articles.", error.toException())
                }
            })
            return likedArticles
        }
    }

    fun likeArticle(newsArticle: NewsArticle, pos:Int, user: String, context: Context) {
        if(user.isNotEmpty()) {
            database = Firebase.database
            fetchLikedData(user)
            // position of the article in main news list
            val list = likedArticlePos
            val posStr = (pos).toString()
            if(list.contains(posStr)) {
                list.remove(posStr)
                val text = "\"" +newsArticle.headline + "\" has been removed from bookmarks"
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            } else {
                list.add(posStr)
                val text = "\"" +newsArticle.headline + "\" has been added to bookmarks"
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            }
            database.getReference("users")
                .child(user.replace(".", ""))
                .child("likedArticles").setValue(list)
            onUpdateListListener?.onUpdateList()
        }
    }

    fun addArticle(newsArticle: NewsArticle, user:String) {
        database = Firebase.database
        // add to main articles
        articles.add(newsArticle)
        database.getReference("articles").setValue(articles)
        onUpdateListListener?.onUpdateList()

        // Get current list of articles for user
        val userArticlesRef = database.getReference("users")
            .child(user.replace(".", ""))
            .child("newsArticles")

        userArticlesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val value = dataSnapshot.getValue<ArrayList<String>>()
                value?.let {
                    userArticles = value.toMutableList()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.i(TAG, "Failed to get user articles.", error.toException())
            }
        })
        // Add the new article
        userArticles.add((articles.size - 1).toString())
        userArticlesRef.setValue(userArticles)

    }
}