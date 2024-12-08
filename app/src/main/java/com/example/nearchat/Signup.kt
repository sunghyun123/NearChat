package com.example.nearchat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class Signup : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var birthdayEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var signupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup1) // XML 레이아웃 파일 이름 수정

        // 뷰 초기화
        nameEditText = findViewById(R.id.nameEditText)
        birthdayEditText = findViewById(R.id.birthdayEditText)
        addressEditText = findViewById(R.id.addressEditText)
        signupButton = findViewById(R.id.signupButton)

        val database = FirebaseDatabase.getInstance().reference
        val firebaseAuth = FirebaseAuth.getInstance()

        // 회원가입 버튼 클릭 리스너
        signupButton.setOnClickListener {
            // 입력 값 가져오기
            val name = nameEditText.text.toString().trim()
            val birthday = birthdayEditText.text.toString().trim()
            val address = addressEditText.text.toString().trim()


            //사용자 고유 키 값인 uid 가져오기 from 파이어베이스..
            val userId = firebaseAuth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}"

            val db = FirebaseFirestore.getInstance()

            // 입력값 유효성 검사
            if (name.isEmpty() || birthday.isEmpty() || address.isEmpty()) {
                Toast.makeText(this, "모든 필드를 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val userInfo = mapOf(
                "uid" to userId,
                "name" to name,
                "birthday" to birthday,
                "address" to address
            )

            db.collection("users").document(userId)
                .set(userInfo)
                .addOnSuccessListener {
                    println("회원정보 저장 성공!")
                    Log.d("Signup", "회원정보 저장 성공!")
                }
                .addOnFailureListener { e ->
                    println("회원정보 저장 실패: ${e.message}")
                    Log.d("Signup", "회원정보 저장 실패: ${e.message}")
                }

            //database.child("users").child(userId).setValue(userData)


            // 회원가입 완료 후 메인 화면으로 이동
            Toast.makeText(this, "회원가입 완료!", Toast.LENGTH_SHORT).show()
            finish() // 현재 액티비티 종료
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
