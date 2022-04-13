package com.daon.tinder_part3_05.util

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.daon.tinder_part3_05.*
import com.daon.tinder_part3_05.model.DBKey.Companion.DIS_LIKE
import com.daon.tinder_part3_05.model.DBKey.Companion.LIKE
import com.daon.tinder_part3_05.model.DBKey.Companion.LIKED_BY
import com.daon.tinder_part3_05.model.DBKey.Companion.NAME
import com.daon.tinder_part3_05.model.DBKey.Companion.USERS
import com.daon.tinder_part3_05.model.DBKey.Companion.USER_ID
import com.daon.tinder_part3_05.R
import com.daon.tinder_part3_05.adapter.CardItemAdapter
import com.daon.tinder_part3_05.model.CardItem
import com.daon.tinder_part3_05.model.DBKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction

class LikeActivity : AppCompatActivity() {

    private val auth: FirebaseAuth = Firebase.auth
    private lateinit var userDB: DatabaseReference
    private val cardItems = mutableListOf<CardItem>()
    private val adapter: CardItemAdapter = CardItemAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_like)

        userDB = Firebase.database.reference.child(DBKey.USERS)
        val currentUserDB = userDB.child(getCurrentUserId())
        currentUserDB.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // 내가 데이터를 수정하거나, 다른 사람이 user Id 에 해당하는 정보를 변경했을 때
                if (snapshot.child(DBKey.NAME).value == null) {
                    // 아직 이름이 설정되어 있지 않은 경우
                    // TODO 사용자로부터 팝업으로 이름을 입력 받는다.
                    showNameInputPopup()
                    return
                }

                // TODO user 정보 갱신 필요
                getUnSelectedUsers()
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        initCardStackView()
        initLikeButton()
        initDisLikeButton()
        initSignOutButton()
        initMatchedListButton()
    }

    private fun initCardStackView() {
        val stackView = findViewById<RecyclerView>(R.id.cardStackView)
        stackView.layoutManager = LinearLayoutManager(this)
        stackView.adapter = adapter
    }

    private fun initLikeButton() {
        findViewById<Button>(R.id.likeButton).setOnClickListener {
            if (cardItems.size == 0) return@setOnClickListener

            val card = cardItems[cardItems.size - 1]
            cardItems.removeLast()

            userDB.child(card.userId)
                .child(DBKey.LIKED_BY)
                .child(DBKey.LIKE)
                .child(getCurrentUserId())
                .setValue(true) // 실제로 값을 저장하는 부분

            // TODO 매칭이 된 시점을 봐야한다.
            saveMatchIfOtherUserLikedMe(card.userId)

            Toast.makeText(this, "${card.name}님을 Like 하셨습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initDisLikeButton() {
        findViewById<Button>(R.id.dislikeButton).setOnClickListener {
            if (cardItems.size == 0) return@setOnClickListener

            val card = cardItems[cardItems.size - 1]
            cardItems.removeLast()

            userDB.child(card.userId)
                .child(DBKey.LIKED_BY)
                .child(DBKey.DIS_LIKE)
                .child(getCurrentUserId())
                .setValue(true) // 실제로 값을 저장하는 부분

            Toast.makeText(this, "${card.name}님을 Dislike 하셨습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initSignOutButton () {
        val signOutButton = findViewById<Button>(R.id.signOutButton)
        signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this@LikeActivity, MainActivity::class.java))
            finish()
        }
    }

    private fun initMatchedListButton () {
        val matchedListButton = findViewById<Button>(R.id.matchListButton)
        matchedListButton.setOnClickListener {
            startActivity(Intent(this@LikeActivity, MatchedUserActivity::class.java))
        }
    }

    private fun saveMatchIfOtherUserLikedMe (otherUserID : String) {
        val otherUserDB = userDB.child(getCurrentUserId()).child(DBKey.LIKED_BY).child(DBKey.LIKE).child(otherUserID)
        otherUserDB.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.value == true) {
                    // 상대방이 이미 날 좋아한 상태
                    userDB.child(getCurrentUserId())
                        .child(DBKey.LIKED_BY)
                        .child(DBKey.MATCH)
                        .child(otherUserID)
                        .setValue(true)

                    userDB.child(otherUserID)
                        .child(DBKey.LIKED_BY)
                        .child(DBKey.MATCH)
                        .child(getCurrentUserId())
                        .setValue(true)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun getUnSelectedUsers() {
        userDB.addChildEventListener(object : ChildEventListener {

            // userDB 에서 일어나는 모든 event 를 수신한다.
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                if (snapshot.child(DBKey.USER_ID).value != getCurrentUserId()
                    && snapshot.child(DBKey.LIKED_BY).child(DBKey.LIKE).hasChild(getCurrentUserId()).not()
                    && snapshot.child(DBKey.LIKED_BY).child(DBKey.DIS_LIKE).hasChild(getCurrentUserId()).not()
                ) {
                    // 내가 아닌 user and 나를 좋아요/싫어요 하지 않은 user 인 경우에만

                    val userId = snapshot.child(DBKey.USER_ID).value.toString()
                    val name = snapshot.child(DBKey.NAME).value ?: "undefined"

                    cardItems.add(CardItem(userId, name.toString()))
                    adapter.submitList(cardItems)
                    adapter.notifyDataSetChanged()
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                cardItems.find { it.userId == snapshot.key }?.let {
                    it.name = snapshot.child(DBKey.NAME).value.toString()
                }
                adapter.submitList(cardItems)
                adapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {/* 데이터 제거 */
            }

            override fun onChildMoved(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {/* 순서 변경 */
            }

            override fun onCancelled(error: DatabaseError) {/* 변경 작업이 취소 */
            }
        })
    }

    private fun showNameInputPopup() {
        val editText = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("이름을 입력해주세요.")
            .setView(editText)
            .setPositiveButton("저장") { _, _ ->
                if (editText.text.isEmpty()) {
                    showNameInputPopup()
                } else {
                    saveUserName(editText.text.toString())
                }
            }
            .setCancelable(false) // 바깥을 터치하거나, 백버튼으로 꺼지는 것을 방지
            .show()
    }

    private fun saveUserName(name: String) {
        val userId = getCurrentUserId()
        val currentUserDB = userDB.child(userId)
        val user = mutableMapOf<String, Any>()
        user[DBKey.NAME] = name
        currentUserDB.updateChildren(user)
            .addOnCompleteListener {
                Toast.makeText(this@LikeActivity, "이름이 저장되었습니다.", Toast.LENGTH_SHORT).show()
            }

        // TODO user 정보를 가져온다.
        getUnSelectedUsers()
    }

    private fun getCurrentUserId(): String {
        if (auth.currentUser == null) {
            Toast.makeText(this, "로그인이 되어있지 않습니다.", Toast.LENGTH_SHORT).show()
            finish()
        }

        return auth.currentUser?.uid.orEmpty()
    }
}