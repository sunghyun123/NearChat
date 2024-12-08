package com.example.nearchat

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.location.Location


data class ChatMessage(
    val message: String,      // 채팅 메시지 내용
    val isMyMessage: Boolean, // 내가 보낸 메시지 여부
    val senderName: String    // 보내는 사람의 이름 (익명1, 익명2 등)
)

@Suppress("UNREACHABLE_CODE")
class ChatFragment : Fragment() {

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatList: MutableList<ChatMessage>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var currentUserId: String
    private lateinit var db: FirebaseFirestore
    private val sharedViewModel: SharedViewModel by activityViewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentDistance: Int = 50

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        val messageEditText = view.findViewById<EditText>(R.id.messageEditText)
        val sendButton = view.findViewById<Button>(R.id.sendButton)
        val setlocation = view.findViewById<LinearLayout>(R.id.setlocation)
        val chatRecyclerView = view.findViewById<RecyclerView>(R.id.chatRecyclerView)

        val distanceTextView: TextView = view.findViewById(R.id.textViewLabel)

        // ViewModel의 거리 값 관찰
        sharedViewModel.distance.observe(viewLifecycleOwner) { distance ->
            distanceTextView.text = "$distance m"
        }


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        getCurrentLocation()





        firebaseAuth = FirebaseAuth.getInstance()
        currentUserId = firebaseAuth.currentUser?.uid ?: "guest_${System.currentTimeMillis()}"
        db = FirebaseFirestore.getInstance()

        setlocation.setOnClickListener {
            showDistanceSettingDialog()
        }

        chatList = mutableListOf()
        chatAdapter = ChatAdapter(chatList)
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.scrollToPosition(chatList.size - 1)
        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageEditText.text.clear()
            }
        }

        // Firestore에서 모든 메시지 로드
        listenToMessages("globalChat", currentUserId) { chatMessage ->
            // 이미 내 메시지는 중복해서 추가하지 않음
            if (chatMessage.isMyMessage) {
                // 내가 보낸 메시지는 이미 보내졌기 때문에 추가하지 않음
                if (chatList.isNotEmpty() && chatList.last().message != chatMessage.message) {
                    chatList.add(chatMessage) // 수정된 타입에 맞게 추가
                    chatAdapter.notifyItemInserted(chatList.size - 1)
                    chatRecyclerView.scrollToPosition(chatList.size - 1)
                }
            } else {
                // 상대방의 메시지는 그대로 추가
                chatList.add(chatMessage)
                chatAdapter.notifyItemInserted(chatList.size - 1)
                chatRecyclerView.scrollToPosition(chatList.size - 1)
            }
        }
        return view


    }

    private fun sendMessage(message: String) {
        // Firestore에 내 메시지 전송
        val messageData = mapOf(
            "senderId" to currentUserId,
            "message" to message,
            "timestamp" to Timestamp.now()
        )

        db.collection("chatRooms").document("globalChat")
            .collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                Log.d("ChatFrag", "메시지 전송 성공!")
            }
            .addOnFailureListener { e ->
                Log.d("ChatFrag", "메시지 전송 실패: ${e.message}")
            }
    }

    private fun listenToMessages(chatRoomId: String, currentUserId: String, onMessageAdded: (ChatMessage) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("chatRooms").document(chatRoomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING) // 메시지를 시간 순으로 정렬
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ChatFrag", "메시지 수신 오류: ${e.message}")
                    return@addSnapshotListener
                }

                val uidList = mutableListOf<String>()
                // 메시지에서 senderId를 수집해서 uidList 생성
                snapshots?.documents?.forEach { doc ->
                    val senderId = doc.getString("senderId")
                    senderId?.let { uidList.add(it) }
                }


                for (doc in snapshots?.documentChanges ?: emptyList()) {
                    if (doc.type == DocumentChange.Type.ADDED) {
                        val message = doc.document.getString("message") ?: ""
                        val senderId = doc.document.getString("senderId") ?: ""
                        val isMyMessage = senderId == currentUserId

                        val senderName = if (!isMyMessage) {
                            generateAnonymousNameForUid(senderId, uidList)  // 상대방의 익명 이름 생성
                        } else {
                            "나"  // 내 메시지인 경우
                        }

                        val chatMessage = ChatMessage(message, isMyMessage, senderName)

                        onMessageAdded(chatMessage)
                    }
                }
            }
    }

    // 사용자의 uid 리스트에 따라 익명 이름을 생성
    private fun generateAnonymousNameForUid(uid: String, uidList: List<String>): String {
        val index = uidList.indexOf(uid)
        return if (index >= 0) {
            "익명${index + 1}"
        } else {
            "익명"  // 기본값 처리
        }
    }

    private fun showDistanceSettingDialog() {
        // 현재 값

        // SeekBar와 TextView로 구성된 팝업 레이아웃
        val dialogView = layoutInflater.inflate(R.layout.dialog_distance_setting, null)

        val seekBar: SeekBar = dialogView.findViewById(R.id.seekBar)
        val distanceTextView: TextView = dialogView.findViewById(R.id.distanceTextView)

        // SeekBar 초기화
        seekBar.progress = currentDistance
        distanceTextView.text = "$currentDistance m"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentDistance = progress
                distanceTextView.text = "$currentDistance m"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // AlertDialog 생성
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("거리 설정")
            .setView(dialogView)
            .setPositiveButton("닫기") { dialog, _ ->
                // 선택된 거리 값 처리
                distanceTextView.text = "$currentDistance m"
                // 값을 프래그먼트에 전달
                updateDistanceText(currentDistance)
                dialog.dismiss()
            }
            .setCancelable(true)

        builder.create().show()
    }

    private fun updateDistanceText(distance: Int) {
        // 거리가 선택되면 텍스트 뷰에 반영
        val distanceTextView: TextView = view?.findViewById(R.id.textViewLabel) ?: return
        distanceTextView.text = "$distance m"

        // ViewModel에 거리 값 업데이트
        sharedViewModel.setDistance(distance)

        // 새 거리 값으로 사용자 필터링 실행
        fetchUsersAndFilterMessages()
    }



    private fun fetchUsersAndFilterMessages() {
        db.collection("users").get()
            .addOnSuccessListener { documents ->
                val filteredUserIds = mutableListOf<String>()
                val maxDistance = sharedViewModel.distance.value ?: 50 // 거리 제한 (m)
                Log.d("ChatFragment", "범위: $maxDistance 미터")

                val latitude = 37.302063//currentLatitude
                val longitude = 127.924621//currentLongitude



                if (latitude == null || longitude == null) {
                    Log.e("ChatFragment", "현재 위치를 가져오지 못했습니다.")
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val location = document.get("location") as? Map<*, *> ?: continue
                    val userLatitude = location["latitude"] as? Double ?: continue
                    val userLongitude = location["longitude"] as? Double ?: continue
                    val uid = document.getString("uid") ?: continue


                    // 거리 계산
                    val distance = DistanceCalculator.calculateDistance(
                        latitude, longitude, userLatitude, userLongitude
                    )

                    Log.d("ChatFragment", "사용자거리: $distance 미터")

                    if (distance <= maxDistance) {
                        // 범위 내 사용자 추가
                        filteredUserIds.add(uid)
                    }
                }

                // 필터링된 사용자 ID 기반으로 메시지 필터링
                filterMessagesByUserIds(filteredUserIds)
            }
            .addOnFailureListener { e ->
                Log.e("ChatFragment", "사용자 데이터 가져오기 실패: ${e.message}")
            }
    }


    private fun filterMessagesByUserIds(filteredUserIds: List<String>) {
        db.collection("chatRooms").document("globalChat")
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ChatFragment", "메시지 수신 오류: ${e.message}")
                    return@addSnapshotListener
                }

                chatList.clear() // 기존 메시지 초기화

                for (doc in snapshots?.documents ?: emptyList()) {
                    val message = doc.getString("message") ?: continue
                    val senderId = doc.getString("senderId") ?: continue

                    if (filteredUserIds.contains(senderId)) {
                        val isMyMessage = senderId == firebaseAuth.currentUser?.uid
                        val senderName = if (isMyMessage) "나" else "익명"
                        chatList.add(ChatMessage(message, isMyMessage, senderName))
                    }
                }

                // RecyclerView 업데이트
                chatAdapter.notifyDataSetChanged()
            }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 위치 권한 요청
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                currentLatitude = it.latitude
                currentLongitude = it.longitude

//                Log.d("ChatFragment", "현재 위치: $currentLatitude, $currentLongitude")

                // 위치 정보를 기반으로 작업 실행
                fetchUsersAndFilterMessages()
            } ?: run {
                Log.w("ChatFragment", "위치를 가져올 수 없습니다.")
            }
        }.addOnFailureListener { e ->
            Log.e("ChatFragment", "위치 가져오기 실패: ${e.message}")
        }
    }
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }



}