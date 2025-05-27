package com.example.ntele

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class OTPVerificationPage : AppCompatActivity() {

    private lateinit var verifyButton: Button
    private lateinit var verifyButtonClickable: Button
    private lateinit var otpBoxes: List<EditText>
    private lateinit var mobileNumToVerify: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otpverification_page)

        verifyButton = findViewById(R.id.verify_button)
        verifyButtonClickable = findViewById(R.id.verify_button_clickable)
        mobileNumToVerify = findViewById(R.id.mobile_num_to_verify)

        // Get full phone number from intent
        val fullPhoneNumber = intent.getStringExtra("fullPhoneNumber") ?: return

        mobileNumToVerify.text = fullPhoneNumber

        otpBoxes = listOf(
            findViewById(R.id.otp_1),
            findViewById(R.id.otp_2),
            findViewById(R.id.otp_3),
            findViewById(R.id.otp_4),
            findViewById(R.id.otp_5),
            findViewById(R.id.otp_6)
        )

        setupOtpWatcher()

        verifyButton.setOnClickListener {
            val otp = otpBoxes.joinToString("") { it.text.toString().trim() }
            if (otp.length == 6) {
                verifyOtpWithBackend(fullPhoneNumber, otp)
            } else {
                Toast.makeText(this, "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }

        verifyButtonClickable.setOnClickListener {
            val otp = otpBoxes.joinToString("") { it.text.toString().trim() }
            if (otp.length == 6) {
                verifyOtpWithBackend(fullPhoneNumber, otp)
            } else {
                Toast.makeText(this, "Enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupOtpWatcher() {
        otpBoxes.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (editText.text.toString().length == 1 && index < otpBoxes.size - 1) {
                        otpBoxes[index + 1].requestFocus()
                    }
                    val otp = otpBoxes.joinToString("") { it.text.toString().trim() }
                    verifyButtonClickable.visibility = if (otp.length == 6) View.VISIBLE else View.GONE
                    verifyButton.visibility = if (otp.length == 6) View.GONE else View.VISIBLE
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }

    private fun verifyOtpWithBackend(phoneNumber: String, otp: String) {
        val url = "https://telematics-zdbu.onrender.com/verify-otp" // Replace with your backend endpoint

        val jsonBody = JSONObject().apply {
            put("phoneNumber", phoneNumber)
            put("code", otp)
        }

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                Toast.makeText(this, "OTP Verified!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, UserDetails::class.java) // Replace with actual next activity
                startActivity(intent)
                finish()
            },
            Response.ErrorListener { error ->
                val errorMsg = error.networkResponse?.data?.let { String(it) } ?: error.message
                Toast.makeText(this, "Verification failed: $errorMsg", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getBody(): ByteArray = jsonBody.toString().toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json"
        }

        Volley.newRequestQueue(this).add(request)
    }
}