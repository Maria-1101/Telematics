package com.example.ntele.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.ntele.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val apiKey = "20b2c7a2d61fa8e3c0f04cc4c3f5cbb3" // Mappls API key
    private lateinit var seatToggleBtn: SwitchMaterial
    private lateinit var vehicleToggleBtn: SwitchMaterial
    private lateinit var handleToggleBtn: SwitchMaterial
    private lateinit var immobilizationToggleBtn: SwitchMaterial
    private lateinit var sosButtonDefault: ImageButton
    private lateinit var sosButtonSelected: ImageButton
    private lateinit var sosFrame: FrameLayout
    private lateinit var sosText: TextView
    private lateinit var nameField: TextView

    private lateinit var batteryLevel: TextView
    private lateinit var dteProgress: TextView
    private lateinit var parkedLocation: TextView
    private lateinit var parkedTime: TextView
    private lateinit var mileageInput: TextView
    private lateinit var speedInput: TextView
    private lateinit var co2Input: TextView
    private lateinit var distanceInput: TextView

    private lateinit var databaseRef: DatabaseReference
    private var mapImageView: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        mapImageView = rootView.findViewById(R.id.map_image)
        seatToggleBtn = rootView.findViewById(R.id.seat_lock_toggle)
        vehicleToggleBtn = rootView.findViewById(R.id.vehicle_lock_toggle)
        handleToggleBtn =  rootView.findViewById(R.id.handle_lock_toggle)
        immobilizationToggleBtn =  rootView.findViewById(R.id.immobilization_toggle)
        sosButtonDefault = rootView.findViewById(R.id.sos_button_default)
        sosButtonSelected = rootView.findViewById(R.id.sos_button_selected)
        sosFrame = rootView.findViewById(R.id.sos_frame)
        sosText =  rootView.findViewById(R.id.sos_text)
        batteryLevel = rootView.findViewById(R.id.battery_level_detection_textview)
        dteProgress = rootView.findViewById(R.id.DTE_progress_textview)
        parkedLocation = rootView.findViewById(R.id.parked_place_textview)
        parkedTime = rootView.findViewById(R.id.parked_time_textview)
        mileageInput = rootView.findViewById(R.id.mileage_display_textview)
        speedInput = rootView.findViewById(R.id.speed_display_textview)
        co2Input = rootView.findViewById(R.id.co2_display_textview)
        distanceInput = rootView.findViewById(R.id.distance_display_textiew)
        nameField = rootView.findViewById(R.id.name_text)

        databaseRef = FirebaseDatabase.getInstance("https://telematics-a0e1f-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .reference

        val name = arguments?.getString("name")
        nameField.text = name ?: "User"
        Log.d("name","" + name)

        name?.let { loadDataFromFirebase(it) }
        
        sosButtonDefault.setOnClickListener {
            updateSosState(1, name)
        }

        sosButtonSelected.setOnClickListener {
            updateSosState(0, name)
        }

        immobilizationToggleBtn.isEnabled = false
        val overlay = rootView.findViewById<View>(R.id.immobilization_click_overlay)

        overlay.setOnClickListener {
            Toast.makeText(requireContext(), "Requires premium subscription", Toast.LENGTH_SHORT).show()

        }


        if (checkLocationPermission()) {
            fetchAndUpdateMap()
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }

        return rootView
    }

    private fun updateSosState(state: Int, name: String?) {
        if (name == null) return

        databaseRef.child("HomeFragment").child(name).child("sos").setValue(state)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (state == 1) {
                        // SOS activated UI changes
                        sosFrame.setBackgroundResource(R.drawable.selected_sos_bg)
                        sosButtonDefault.visibility = View.GONE
                        sosButtonSelected.visibility = View.VISIBLE
                        val boldTypeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_bold)
                        sosText.typeface = boldTypeface
                        Toast.makeText(requireContext(), "SOS Activated", Toast.LENGTH_SHORT).show()
                    } else {
                        // SOS deactivated UI changes
                        sosFrame.setBackgroundResource(R.drawable.sos_bg)
                        sosButtonDefault.visibility = View.VISIBLE
                        sosButtonSelected.visibility = View.GONE
                        val regularTypeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_regular)
                        sosText.typeface = regularTypeface
                        Toast.makeText(requireContext(), "SOS Deactivated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to update SOS state", Toast.LENGTH_SHORT).show()
                }
            }

    }

    private fun loadDataFromFirebase(userName: String) {
        val userRef = databaseRef.child("HomeFragment").child(userName)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Battery Level (e.g., "85 %")
                    batteryLevel.text = snapshot.child("battery_level").getValue(Int::class.java)?.let {
                        "$it %"
                    } ?: "-"

                    // DTE Progress (Distance to Empty) in km (e.g., "120 km")
                    dteProgress.text = snapshot.child("dte_progress").getValue(Int::class.java)?.let {
                        "DTE $it km"
                    } ?: "-"

                    // Mileage in km (e.g., "15 km")
                    mileageInput.text = snapshot.child("mileage").getValue(Double::class.java)?.let {
                        if (it % 1.0 == 0.0) {
                            "${it.toInt()} km"
                        } else {
                            String.format("%.2f km", it)
                        }
                    } ?: "-"

                    speedInput.text = snapshot.child("avg_speed").getValue(Double::class.java)?.let {
                        if (it % 1.0 == 0.0) {
                            "${it.toInt()} km/h"
                        } else {
                            String.format("%.2f km/h", it)
                        }
                    } ?: "-"

                    distanceInput.text = snapshot.child("total_distance").getValue(Double::class.java)?.let {
                        if (it % 1.0 == 0.0) {
                            "${it.toInt()} km"
                        } else {
                            String.format("%.2f km", it)
                        }
                    } ?: "-"

                    co2Input.text = snapshot.child("co2_saved").getValue(Double::class.java)?.let {
                        if (it % 1.0 == 0.0) {
                            "${it.toInt()} kg"
                        } else {
                            String.format("%.2f kg", it)
                        }
                    } ?: "-"

                    // Parked Place (string)
                    parkedLocation.text = snapshot.child("parked_place").getValue(String::class.java) ?: "-"

                    // Parked Time formatted as "Xh Ym"
                    val parkedTimeRaw = snapshot.child("parked_time").getValue(Double::class.java)
                    parkedTime.text = parkedTimeRaw?.let { formatParkedTime(it) } ?: "-"

                    // Toggles: seat_lock, vehicle_lock, handle_lock (stored as Int 1 or 0)
                    seatToggleBtn.isChecked = snapshot.child("seat_lock").getValue(Int::class.java) == 1
                    vehicleToggleBtn.isChecked = snapshot.child("vehicle_lock").getValue(Int::class.java) == 1
                    handleToggleBtn.isChecked = snapshot.child("handle_lock").getValue(Int::class.java) == 1

                    val sosState = snapshot.child("sos").getValue(Int::class.java) ?: 0
                    if (sosState == 1) {
                        sosFrame.setBackgroundResource(R.drawable.selected_sos_bg)
                        sosButtonDefault.visibility = View.GONE
                        sosButtonSelected.visibility = View.VISIBLE
                        val boldTypeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_bold)
                        sosText.typeface = boldTypeface
                    } else {
                        sosFrame.setBackgroundResource(R.drawable.sos_bg)
                        sosButtonDefault.visibility = View.VISIBLE
                        sosButtonSelected.visibility = View.GONE
                        val regularTypeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_regular)
                        sosText.typeface = regularTypeface
                    }

                } else {
                    // No data found
                    Toast.makeText(requireContext(), "No data found for $userName", Toast.LENGTH_SHORT).show()
                }
            }

            private fun formatParkedTime(timeInMinutes: Double): String {
                val totalMinutes = timeInMinutes.toInt()
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                return "${hours}h ${minutes}m"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Update Firebase on toggle changes
        seatToggleBtn.setOnCheckedChangeListener { _, isChecked ->
            userRef.child("seat_lock").setValue(if (isChecked) 1 else 0)
            Toast.makeText(requireContext(), if (isChecked) "Seat Lock Enabled" else "Seat Lock Disabled", Toast.LENGTH_SHORT).show()
        }

        vehicleToggleBtn.setOnCheckedChangeListener { _, isChecked ->
            userRef.child("vehicle_lock").setValue(if (isChecked) 1 else 0)
            Toast.makeText(requireContext(), if (isChecked) "Vehicle Lock Enabled" else "Vehicle Lock Disabled", Toast.LENGTH_SHORT).show()
        }

        handleToggleBtn.setOnCheckedChangeListener { _, isChecked ->
            userRef.child("handle_lock").setValue(if (isChecked) 1 else 0)
            Toast.makeText(requireContext(), if (isChecked) "Handle Lock Enabled" else "Handle Lock Disabled", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to convert parked time in minutes (String) to "Xh Ym" format
    private fun formatParkedTime(timeString: String): String {
        return try {
            val totalMinutes = timeString.toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            "${hours}h ${minutes}m"
        } catch (e: NumberFormatException) {
            timeString // fallback if not a valid number
        }
    }


    override fun onResume() {
        super.onResume()
        if (checkLocationPermission()) {
            fetchAndUpdateMap()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            fetchAndUpdateMap()
        }
    }

    private fun fetchAndUpdateMap() {
        getCurrentLocation { latitude, longitude ->
            updateMapImage(latitude, longitude)
        }
    }

    private fun getCurrentLocation(onLocationFetched: (Double, Double) -> Unit) {
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        onLocationFetched(it.latitude, it.longitude)
                    }
                }
                .addOnFailureListener {
                    // Handle failure
                }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateMapImage(latitude: Double, longitude: Double) {
        mapImageView?.post {
            val widthPx = mapImageView!!.width
            val heightPx = mapImageView!!.height

            if (widthPx == 0 || heightPx == 0) {
                // fallback if size not determined
                return@post
            }

            val mapUrl = "https://apis.mappls.com/advancedmaps/v1/$apiKey/still_image" +
                    "?center=$latitude,$longitude" +
                    "&zoom=15" +
                    "&size=${widthPx}x${heightPx}" +
                    "&pt=$latitude,$longitude,bluedot" +
                    "&format=png&scale=2"

            val cornerRadiusDp = 16
            val scale = resources.displayMetrics.density
            val cornerRadiusPx = (cornerRadiusDp * scale + 0.5f).toInt()

            Glide.with(this)
                .load(mapUrl)
                .apply(
                    RequestOptions()
                        .override(widthPx, heightPx)
                        .transform(RoundedCorners(cornerRadiusPx))
                )
                .into(mapImageView!!)
        }
    }
}
