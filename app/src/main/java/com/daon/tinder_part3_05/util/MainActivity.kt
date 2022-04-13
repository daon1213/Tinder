package com.daon.tinder_part3_05.util

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.daon.tinder_part3_05.R
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


    }

    override fun onStart() {
        super.onStart()

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(Intent(this, LikeActivity::class.java))
        }
    }
}