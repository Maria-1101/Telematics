package com.example.ntele

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hbb20.CountryCodePicker
import org.json.JSONObject

class LoginPage : AppCompatActivity() {
    private lateinit var mobileNumLoginButton: AppCompatButton
    private lateinit var emailLoginButton: AppCompatButton
    private lateinit var mobileNumLayout: LinearLayout
    private lateinit var emailLayout: LinearLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var ccp: CountryCodePicker
    private lateinit var phoneEditText: TextInputEditText
    private lateinit var sendOtpButton: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        // Initialize views
        ccp = findViewById(R.id.countryCodePicker)
        phoneEditText = findViewById(R.id.editTextPhone)
        sendOtpButton = findViewById(R.id.send_otp_btn)
        mobileNumLayout = findViewById(R.id.mobile_num_login_layout)
        emailLoginButton = findViewById(R.id.email_login_btn)
        mobileNumLoginButton = findViewById(R.id.mobile_num_login_btn)
        emailLayout = findViewById(R.id.email_login_layout)
        emailEditText = findViewById(R.id.email_input)

        // Default view: Mobile number login
        mobileNumLayout.visibility = LinearLayout.VISIBLE
        emailLayout.visibility = LinearLayout.GONE
        mobileNumLoginButton.visibility = AppCompatButton.GONE
        emailLoginButton.visibility = AppCompatButton.VISIBLE

        emailLoginButton.setOnClickListener {
            mobileNumLayout.visibility = LinearLayout.GONE
            mobileNumLoginButton.visibility = AppCompatButton.VISIBLE
            emailLayout.visibility = LinearLayout.VISIBLE
            emailLoginButton.visibility = AppCompatButton.GONE
        }

        mobileNumLoginButton.setOnClickListener {
            mobileNumLayout.visibility = LinearLayout.VISIBLE
            mobileNumLoginButton.visibility = AppCompatButton.GONE
            emailLayout.visibility = LinearLayout.GONE
            emailLoginButton.visibility = AppCompatButton.VISIBLE
        }

        sendOtpButton.setOnClickListener {
            if (mobileNumLayout.visibility == LinearLayout.VISIBLE) {
                val phone = phoneEditText.text.toString().trim()
                if (phone.isEmpty()) {
                    phoneEditText.error = "Please enter phone number"
                    return@setOnClickListener
                }
                val fullPhone = "+${ccp.selectedCountryCode}$phone"

                Log.d("Full Number", "Full phone number: $fullPhone")

                checkIfUserExists(fullPhone, isEmail = false)
            } else {
                val email = emailEditText.text.toString().trim()
                if (email.isEmpty()) {
                    emailEditText.error = "Please enter email"
                    return@setOnClickListener
                }
                checkIfUserExists(email, isEmail = true)
            }
        }

        val registrationLink = findViewById<TextView>(R.id.new_user_register)
        registrationLink.setOnClickListener {
            val i = Intent(this, RegistrationPage::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }
    }
    private fun checkIfUserExists(identifier: String, isEmail: Boolean) {
        val dbRef = FirebaseDatabase.getInstance("https://telematics-a0e1f-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("RegistrationDetails")
        val query = if (isEmail) {
            dbRef.orderByChild("email").equalTo(identifier)
        } else {
            dbRef.orderByChild("phone").equalTo(identifier)
        }

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val name = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"

                    if (isEmail) {
                        sendEmailOtp(identifier, name)
                    } else {
                        sendMobileOtp(identifier, name)
                    }
                } else {
                    Toast.makeText(this@LoginPage, "User not registered", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginPage, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMobileOtp(phoneNumber: String, name: String) {
        val url = "https://telematics-zdbu.onrender.com/send-otp"

        val jsonBody = JSONObject().apply {
            put("phoneNumber", phoneNumber)
        }

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                Toast.makeText(this, "OTP Sent!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, OTPVerificationPage::class.java)
                intent.putExtra("phoneNumber", phoneNumber)
                intent.putExtra("name", name)
                intent.putExtra("source","login")
                startActivity(intent)
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Failed to send OTP: ${error.message}", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getBody(): ByteArray = jsonBody.toString().toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json"
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun sendEmailOtp(email: String, name: String) {
        val url = "https://telematics-zdbu.onrender.com/send-email-otp"  // Replace with your actual URL
        val queue = Volley.newRequestQueue(this)

        val json = JSONObject().apply {
            put("email", email)
        }

        val request = object : JsonObjectRequest(
            Method.POST, url, json,
            Response.Listener { response ->
                val success = response.optBoolean("success", false)
                if (success) {
                    val intent = Intent(this, EmailOTPVerification::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("name", name)
                    Log.d("name Registration","" + name)
                    intent.putExtra("source","login")
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Failed to send Email OTP", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return hashMapOf("Content-Type" to "application/json")
            }
        }

        queue.add(request)
    }
}