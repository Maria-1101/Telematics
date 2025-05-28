package com.example.ntele

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.textfield.TextInputEditText
import com.hbb20.CountryCodePicker
import org.json.JSONObject

class RegistrationPage : AppCompatActivity() {

    private lateinit var phoneNumberEditText: TextInputEditText
    private lateinit var sendOtpButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration_page)

        val ccp = findViewById<CountryCodePicker>(R.id.countryCodePicker)
        val loginLink = findViewById<TextView>(R.id.new_user_register)
        phoneNumberEditText = findViewById(R.id.mobile_num_edittext)
        sendOtpButton = findViewById(R.id.send_otp_btn)

        loginLink.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        sendOtpButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString().trim()

            if (!phoneNumber.matches(Regex("^[0-9]{10}$"))) {
                phoneNumberEditText.error = "Enter valid 10-digit number"
                return@setOnClickListener
            }

            val fullPhoneNumber = "+${ccp.selectedCountryCode}$phoneNumber"
            sendOtpToBackend(fullPhoneNumber)
        }
    }

    private fun sendOtpToBackend(phoneNumber: String) {
        val url = "https://telematics-zdbu.onrender.com/send-otp" // Replace with your actual backend URL

        val jsonBody = JSONObject().apply {
            put("phone", phoneNumber)
        }

        val requestBody = jsonBody.toString()

        val request = object : StringRequest(
            Request.Method.POST, url,
            Response.Listener { response ->
                Toast.makeText(this, "OTP Sent!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, OTPVerificationPage::class.java)
                intent.putExtra("fullPhoneNumber", phoneNumber)
                startActivity(intent)
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Failed to send OTP: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("OTP", "Error: ${error.message}")
            }
        ) {
            override fun getBody(): ByteArray = requestBody.toByteArray(Charsets.UTF_8)
            override fun getBodyContentType(): String = "application/json"
        }

        Volley.newRequestQueue(this).add(request)
    }
}