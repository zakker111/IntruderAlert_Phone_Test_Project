package com.example.intruderalert

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var emailInput: EditText
    private var photoUri: Uri? = null
    private var audioUri: Uri? = null
    private var lastLocationText = "Location not checked yet."
    private var recorder: MediaRecorder? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(IntruderAdminReceiver.PREFS, Context.MODE_PRIVATE)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val scroll = ScrollView(this)
        content.addView(TextView(this).apply { text = "Intruder Alert"; textSize = 30f })
        content.addView(TextView(this).apply {
            text = "Best-effort lock-screen event monitor and evidence helper. Android/OnePlus may not report every wrong PIN. Photo/audio capture is started by you while the app is open; Android can block background capture. This app never reads or stores your PIN."
            textSize = 15f
        })
        emailInput = EditText(this).apply {
            hint = "Alert email address (optional)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setSingleLine(true)
            setText(prefs.getString(IntruderAdminReceiver.KEY_EMAIL, "") ?: "")
        }
        content.addView(emailInput, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        content.addView(button("Save email address") {
            prefs.edit().putString(IntruderAdminReceiver.KEY_EMAIL, emailInput.text.toString().trim()).apply()
            toast("Email address saved on this phone")
        })
        content.addView(button("Enable lock-screen event test") { requestAdminActivation() })
        content.addView(button("Check location and map link") { checkLocation() })
        content.addView(button("Take photo") { takePhoto() })
        content.addView(button(if (isRecording) "Stop audio recording" else "Record short audio clip") {
            if (isRecording) stopAudioRecording() else startAudioRecording()
        })
        content.addView(button("Prepare email alert with available evidence") { composeAlertEmail() })
        content.addView(button("Refresh event log") { refreshStatus() })
        content.addView(button("Clear local event log") {
            prefs.edit().remove(IntruderAdminReceiver.KEY_EVENT_COUNT)
                .remove(IntruderAdminReceiver.KEY_LAST_EVENT).remove(IntruderAdminReceiver.KEY_LAST_SUCCESS)
                .remove(IntruderAdminReceiver.KEY_EVENT_LOG).apply()
            refreshStatus()
        })
        statusText = TextView(this).apply { textSize = 14f }
        content.addView(statusText, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        scroll.addView(content)
        setContentView(scroll)
        refreshStatus()
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun requestAdminActivation() {
        val component = ComponentName(this, IntruderAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Optional legacy failed-unlock callback test. Support varies by Android version and manufacturer.")
        }
        try { startActivity(intent) } catch (e: Exception) { toast("Could not open admin settings: ${e.message}") }
    }

    private fun checkLocation() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_LOCATION)
            return
        }
        try {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            val location: Location? = manager.getProviders(true).mapNotNull { provider ->
                try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            }.maxByOrNull { it.time }
            lastLocationText = if (location != null) {
                "Latitude: ${location.latitude}\nLongitude: ${location.longitude}\nAccuracy: ${location.accuracy} m\nMap: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else "No cached location available. Enable Location, then try again outdoors. Android may not have a recent location cached."
            getSharedPreferences(IntruderAdminReceiver.PREFS, MODE_PRIVATE).edit().putString(IntruderAdminReceiver.KEY_LOCATION, lastLocationText).apply()
            toast(if (location != null) "Location found" else "No cached location found")
        } catch (e: Exception) { lastLocationText = "Location unavailable: ${e.message}" }
        refreshStatus()
    }

    private fun takePhoto() {
        val file = File(cacheDir, "intruder-photo-${System.currentTimeMillis()}.jpg")
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try { startActivityForResult(intent, REQUEST_CAMERA) }
        catch (_: Exception) { toast("No compatible camera app available") }
    }

    @Suppress("DEPRECATION")
    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_AUDIO)
            return
        }
        try {
            val file = File(cacheDir, "intruder-audio-${System.currentTimeMillis()}.m4a")
            audioUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            isRecording = true
            toast("Recording started. Tap Stop audio recording when finished.")
            refreshStatus()
            // The user explicitly starts and stops recording while the app is open.
        } catch (e: Exception) {
            try { recorder?.release() } catch (_: Exception) {}
            recorder = null
            isRecording = false
            toast("Could not start recording: ${e.message}")
        }
    }

    private fun stopAudioRecording() {
        try {
            recorder?.stop()
            toast("Audio clip saved temporarily for this report")
        } catch (_: Exception) { audioUri = null; toast("Recording was too short or could not be saved") }
        finally {
            try { recorder?.release() } catch (_: Exception) {}
            recorder = null
            isRecording = false
            refreshStatus()
        }
    }

    @Deprecated("Uses activity result API for broad Android compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA) {
            if (resultCode == RESULT_OK) toast("Photo ready to attach to the alert")
            else { photoUri = null; toast("Photo cancelled") }
            refreshStatus()
        }
    }

    private fun composeAlertEmail() {
        if (isRecording) stopAudioRecording()
        val prefs = getSharedPreferences(IntruderAdminReceiver.PREFS, MODE_PRIVATE)
        val email = emailInput.text.toString().trim().ifBlank { prefs.getString(IntruderAdminReceiver.KEY_EMAIL, "") ?: "" }
        val lastEvent = prefs.getString(IntruderAdminReceiver.KEY_LAST_EVENT, "No failed-unlock callback recorded") ?: "No failed-unlock callback recorded"
        val savedLocation = prefs.getString(IntruderAdminReceiver.KEY_LOCATION, null)
        if (savedLocation != null && lastLocationText == "Location not checked yet.") lastLocationText = savedLocation
        val mapLine = if (lastLocationText.contains("https://maps.google.com")) lastLocationText.substringAfter("Map: ") else "No map link available; open the app and check location."
        val body = "Intruder Alert report\n\nTime: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n$lastEvent\n\nLocation:\n$lastLocationText\n\nMap: $mapLine\n\nImportant: Android/OnePlus may not report every failed unlock. Photo and audio evidence are included only if captured in the app. This report is shared through your email app; you must review and press Send."
        val attachments = arrayListOf<Uri>()
        photoUri?.let { attachments.add(it) }
        audioUri?.let { attachments.add(it) }
        val intent = if (attachments.isNotEmpty()) Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, attachments)
        } else Intent(Intent.ACTION_SEND).apply { type = "text/plain" }
        intent.putExtra(Intent.EXTRA_EMAIL, if (email.isNotBlank()) arrayOf(email) else emptyArray<String>())
        intent.putExtra(Intent.EXTRA_SUBJECT, "Intruder Alert - phone security report")
        intent.putExtra(Intent.EXTRA_TEXT, body)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try { startActivity(Intent.createChooser(intent, "Review and send alert email")) }
        catch (_: Exception) { toast("No compatible email or sharing app found") }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) checkLocation()
        else if (requestCode == REQUEST_LOCATION) toast("Location permission denied")
        if (requestCode == REQUEST_AUDIO && grantResults.any { it == PackageManager.PERMISSION_GRANTED }) startAudioRecording()
        else if (requestCode == REQUEST_AUDIO) toast("Microphone permission denied")
    }

    private fun refreshStatus() {
        val prefs = getSharedPreferences(IntruderAdminReceiver.PREFS, MODE_PRIVATE)
        val component = ComponentName(this, IntruderAdminReceiver::class.java)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val active = dpm.isAdminActive(component)
        val count = prefs.getInt(IntruderAdminReceiver.KEY_EVENT_COUNT, 0)
        val lastFailure = prefs.getString(IntruderAdminReceiver.KEY_LAST_EVENT, "No failure callback recorded.")
        val lastSuccess = prefs.getString(IntruderAdminReceiver.KEY_LAST_SUCCESS, "No success callback recorded.")
        val log = prefs.getString(IntruderAdminReceiver.KEY_EVENT_LOG, "No events logged yet.")
        statusText.text = "\nSTATUS\nDevice admin active: $active\nFailed-unlock callbacks logged: $count\nLast failure: $lastFailure\nLast success: $lastSuccess\n\nPhoto ready: ${photoUri != null}\nAudio ready: ${audioUri != null}\nAudio recording: $isRecording\n\nLocation:\n$lastLocationText\n\nEVENT LOG\n$log\n\nTEST\nActivate the optional receiver, lock the phone, enter a wrong PIN, unlock normally, then reopen this app and refresh. If the count stays at zero, your Android/OxygenOS version did not deliver the callback. Email sending always requires your review and confirmation."
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onDestroy() {
        if (isRecording) stopAudioRecording()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_LOCATION = 20
        private const val REQUEST_CAMERA = 21
        private const val REQUEST_AUDIO = 22
    }
}
