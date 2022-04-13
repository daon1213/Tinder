package com.daon.tinder_part3_05.util

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daon.tinder_part3_05.model.CardItem
import com.daon.tinder_part3_05.adapter.MatchedUserAdapter
import com.daon.tinder_part3_05.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MatchedUserActivity : AppCompatActivity() {

    private val auth: FirebaseAuth = Firebase.auth
    private lateinit var userDB: DatabaseReference
    private val adapter = MatchedUserAdapter()
    // 원래는 matchedUser 를 위한 model 를 새로 만드는 것이 맞다ㅏ.
    private val cardItems = mutableListOf<CardItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)

        userDB = Firebase.database.reference.child("Users")
        initMatchedUserRecyclerView()
        getMatchUsers()
    }

    private fun initMatchedUserRecyclerView () {
        val recyclerView : RecyclerView = findViewById(R.id.matchedUserRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun getMatchUsers () {
        val matchedDB = userDB.child(getCurrentUserId()).child("likedBy").child("match")
        matchedDB.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.key?.isNotEmpty() == true) {
                    // match 가 없는 user 가 있을 수 있기 때문에
                    getUserByKey(snapshot.key.orEmpty())
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getUserByKey (userId : String) {
        userDB.child(userId).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                cardItems.add(CardItem(userId, snapshot.child("name").value.toString()))
                adapter.submitList(cardItems)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getCurrentUserId(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        return auth.currentUser?.uid.orEmpty()
    }
}