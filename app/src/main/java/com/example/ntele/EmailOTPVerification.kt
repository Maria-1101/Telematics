package com.example.ntele

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class EmailOTPVerification : AppCompatActivity() {

    private lateinit var emailIdToVerify: TextView
    private lateinit var otpFields: List<EditText>
    private lateinit var timerText: TextView
    private lateinit var resendOTP: TextView
    private lateinit var verifyButton: Button

    private lateinit var userEmail: String
    private var countDownTimer: CountDownTimer? = null
    private val BASE_URL = "https://telematics-zdbu.onrender.com"

    // Retain user input fields
    private lateinit var fullName: String
    private lateinit var phoneNumber: String
    private lateinit var dob: String
    private lateinit var state: String
    private lateinit var city: String
    private lateinit var postalCodeStr: String
    private lateinit var gender: String
    private lateinit var source: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_otpverification)

        emailIdToVerify = findViewById(R.id.email_id_to_veify)
        timerText = findViewById(R.id.new_otp_request_timing)
        resendOTP = findViewById(R.id.resend_otp)
        verifyButton = findViewById(R.id.verify_button)

        otpFields = listOf(
            findViewById(R.id.otp_1),
            findViewById(R.id.otp_2),
            findViewById(R.id.otp_3),
            findViewById(R.id.otp_4),
            findViewById(R.id.otp_5),
            findViewById(R.id.otp_6)
        )

        // Get data from intent
        userEmail = intent.getStringExtra("email") ?: ""
        fullName = intent.getStringExtra("name") ?: ""
        dob = intent.getStringExtra("dob") ?: ""
        state = intent.getStringExtra("state") ?: ""
        city = intent.getStringExtra("city") ?: ""
        postalCodeStr = intent.getStringExtra("postalCode") ?: ""
        gender = intent.getStringExtra("gender") ?: ""
        phoneNumber = intent.getStringExtra("verifiedPhoneNumber") ?: ""
        source = intent.getStringExtra("source") ?: "registration"

        emailIdToVerify.text = userEmail

        setupOtpInputs()
        startCountdown()
        sendEmailOtp(userEmail)

        resendOTP.setOnClickListener {
            sendEmailOtp(userEmail)
            startCountdown()
        }

        verifyButton.setOnClickListener {
            val otp = otpFields.joinToString("") { it.text.toString() }
            if (otp.length == 6) {
                verifyEmailOtp(userEmail, otp)
            } else {
                Toast.makeText(this, "Enter all 6 digits", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupOtpInputs() {
        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (!s.isNullOrEmpty() && i < otpFields.size - 1) {
                        otpFields[i + 1].requestFocus()
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun startCountdown() {
        resendOTP.isEnabled = false
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerText.text = "Resend OTP in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                resendOTP.isEnabled = true
                timerText.text = "Didn't get code?"
            }
        }.start()
    }

    private fun sendEmailOtp(email: String) {
        val client = OkHttpClient()
        val json = JSONObject().put("email", email).toString()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL/send-email-otp")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EmailOTPVerification, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EmailOTPVerification, "OTP sent to your email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@EmailOTPVerification, "Error sending OTP", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun verifyEmailOtp(email: String, otp: String) {
        val client = OkHttpClient()
        val json = JSONObject()
            .put("email", email)
            .put("otp", otp)
            .toString()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$BASE_URL/verify-email-otp")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EmailOTPVerification, "Verification failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EmailOTPVerification, "Email verified!", Toast.LENGTH_SHORT).show()

                        if(source == "registration") {
                            val intent = Intent(this@EmailOTPVerification, UserDetails::class.java)
                            intent.putExtra("verifiedEmail", email)
                            intent.putExtra("name", fullName)
                            intent.putExtra("dob", dob)
                            intent.putExtra("state", state)
                            intent.putExtra("city", city)
                            intent.putExtra("postalCode", postalCodeStr)
                            intent.putExtra("gender", gender)
                            intent.putExtra("verifiedPhoneNumber", phoneNumber)
                            intent.putExtra("isEmailVerified", true)
                            startActivity(intent)
                        }else if(source == "login") {
                            val intent = Intent (this@EmailOTPVerification, BottomNavigationDrawer::class.java)
                            intent.putExtra("name", fullName)
                            Log.d(" name Email","" + fullName)
                            startActivity(intent)
                        }
                        finish()
                    } else {
                        Toast.makeText(this@EmailOTPVerification, "Invalid OTP", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
