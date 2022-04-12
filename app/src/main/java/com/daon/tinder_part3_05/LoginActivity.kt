package com.daon.tinder_part3_05

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.math.sign

class LoginActivity: AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val emailEditText: EditText by lazy { findViewById(R.id.emailEditText) }
    private val passwordEditText: EditText by lazy { findViewById(R.id.passwordEditText) }
    private val loginButton: Button by lazy { findViewById<Button>(R.id.loginButton) }
    private val signUpButton: Button by lazy { findViewById<Button>(R.id.signUpButton) }

    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = Firebase.auth // 자바에서의 Firebase.getInstance()와 동일
        callbackManager = CallbackManager.Factory.create()

        // option + command + m
        initLoginButton()
        initSignUpButton()
        initFaceBookLoginButton()
        initEmailAndPasswordEditText()

        setButtonEnabled(false)
    }

    private fun initLoginButton() {
        loginButton.setOnClickListener {
            val email = getInputEmail()
            val password = getInputPassword()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    // 로그인에 성공한 경우, LoginActivity 종료
                    if (task.isSuccessful) {
                        // MainActivity 로 이동
                        handleSuccessLogin()
                    } else {
                        Toast.makeText(
                            this,
                            "로그인에 실패하였습니다. 이메일 또는 비밀번호를 확인해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun initSignUpButton() {
        signUpButton.setOnClickListener {
            val email = getInputEmail()
            val password = getInputPassword()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // 회원가입에 성공한 경우
                        Toast.makeText(this, "회원가입이 되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    // 구글 이메일 로그인 최소 비밀번호 길이는 6자 이상
                    Toast.makeText(this, it.message.orEmpty(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun initFaceBookLoginButton() {
        val facebookLoginButton = findViewById<LoginButton>(R.id.facebookLoginButton)
        // facebook 으로부터 받아올 정보를 추가
        facebookLoginButton.setPermissions("email","public_profile")
        facebookLoginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult>  {
            override fun onSuccess(result: LoginResult) {
                // 로그인 성공
                val credential = FacebookAuthProvider.getCredential(result.accessToken.token)
                // 이메일과 다른게 해당 token 의 회원정보가 없다면 자동으로 회원가입을 하고, 로그인을 수행한다.
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this@LoginActivity) { task ->
                        if (task.isSuccessful) {
                            handleSuccessLogin()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@LoginActivity, "페이스북 로그인이 실패하였습니다.", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancel() {}

            override fun onError(error: FacebookException?) {
                Toast.makeText(this@LoginActivity, "페이스북 로그인이 실패하였습니다.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun initEmailAndPasswordEditText() {
        // 이메일과 비밀번호를 둘 다 입력하지 않을 경우, 로그인/회원가입 버튼 비 활성화
        emailEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotBlank() && passwordEditText.text.isNotBlank()
            setButtonEnabled(enable)
        }
        passwordEditText.addTextChangedListener {
            val enable = emailEditText.text.isNotBlank() && passwordEditText.text.isNotBlank()
            setButtonEnabled(enable)
        }
    }

    private fun setButtonEnabled(enable: Boolean) {
        loginButton.isEnabled = enable
        signUpButton.isEnabled = enable
    }

    private fun getInputEmail() = emailEditText.text.toString()
    private fun getInputPassword() = passwordEditText.text.toString()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleSuccessLogin () {
        if (auth.currentUser == null) { // 한번 더 검사
            Toast.makeText(this, "로그인에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }

        val userId = auth.currentUser?.uid.orEmpty()
        // json 형식으로 가지고 오며, reference 가 최상위 객체이다.
        // child(userId) 가 없으면 새로 생성
        val currentUserDB = Firebase.database.reference.child("Users").child(userId)
        val user = mutableMapOf<String, Any>()
        user["userId"] = userId
        currentUserDB.updateChildren(user)
            .addOnCompleteListener {
                finish()
            }
    }
}