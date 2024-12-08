package com.example.nearchat

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth

import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private val PERMISSIONS_REQUEST_CODE = 100
    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.INTERNET,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login) // XML 레이아웃 파일 이름 수정

        // 뷰 초기화
        val titleTextView: TextView = findViewById(R.id.titleTextView)
        val subtitleTextView: TextView = findViewById(R.id.subtitleTextView)
        val googleLoginButton: Button = findViewById(R.id.googleLoginButton)
//        val facebookLoginButton: Button = findViewById(R.id.facebookLoginButton)  페이스북 로그인 추후 추가
//        val naverLoginButton: Button = findViewById(R.id.naverLoginButton) 네이버 로그인 추후 추가
        val sharedPreferences: SharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)


        val das = "911433686322-ou70edbrc4ki6rv450tnj0ube7occ6l4.apps.googleusercontent.com"
        // FirebaseAuth 인스턴스 초기화
        auth = FirebaseAuth.getInstance()

        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(das) // Firebase Console에서 Web Client ID로 대체
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()


        // 구글 로그인 버튼 클릭 리스너
        googleLoginButton.setOnClickListener {
            // 구글 로그인 로직 추가
            // 예: GoogleSignInClient을 사용하여 로그인 시작
            // 로그인 성공 후 회원가입 페이지로 이동
            startGoogleOneTapSignIn()
             // 이 함수는 아래에서 정의합니다.
        }

        /*
        // 페이스북 로그인 버튼 클릭 리스너
        facebookLoginButton.setOnClickListener {
            // 페이스북 로그인 로직 추가
            // 로그인 성공 후 회원가입 페이지로 이동
            navigateToSignup()
        }

        // 네이버 로그인 버튼 클릭 리스너
        naverLoginButton.setOnClickListener {
            // 네이버 로그인 로직 추가
            // 로그인 성공 후 회원가입 페이지로 이동
            navigateToSignup()
        }
        */

        if (isFirstLaunch) {
            // 처음 앱을 실행하는 경우에만 권한 요청
            requestPermissionsIfNeeded()
            // 첫 실행 여부를 false로 설정
            sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
        }


    }




    // 회원가입 페이지로 이동하는 함수
    private fun navigateToSignup() {
        //val intent = Intent(this, MainActivity::class.java) // 메인 액티비티 이동, 개발자용(회원가입 과정 생략)
        val intent = Intent(this, Signup::class.java) // SignupActivity로 이동
        startActivity(intent)
        finish() // 현재 액티비티 종료
    }

    private fun startGoogleOneTapSignIn() {
        Log.d("MainActivity", "startGoogleOneTapSignIn")


        oneTapClient.beginSignIn(signInRequest)
            .addOnSuccessListener(this) { result ->
                try {
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender, REQ_ONE_TAP,
                        null, 0, 0, 0)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("MainActivity", "Couldn't start One Tap UI: ${e.localizedMessage}")
                }
            }
            .addOnFailureListener(this) { e ->
                // No Google Accounts found. Just continue presenting the signed-out UI.
                Log.d("MainActivity", e.localizedMessage)
            }
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_ONE_TAP) {
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: Exception) {
                Log.d("MainActivity", "No user logged 2")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "user logged in")
                    navigateToSignup()
                    // 이후 필요한 액티비티나 화면으로 전환
                } else {
                    Log.d("MainActivity", "No user logged 3")
                }
            }

    }

    companion object {
        private const val REQ_ONE_TAP = 2
    }


    private fun requestPermissionsIfNeeded() {
        val permissionsToRequest = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // 권한이 허용됨
                    println("${permissions[i]} granted")
                } else {
                    // 권한이 거부됨
                    println("${permissions[i]} denied")
                }
            }
        }
    }

}