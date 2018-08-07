package com.krisbiketeam.smarthomeraspbpi3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.krisbiketeam.data.storage.NotSecureStorage

private const val PERMISSION_REQUEST_ID = 999

class LoadActivity : AppCompatActivity() {

    private lateinit var secureStorage: NotSecureStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        secureStorage = NotSecureStorage(this)
        requestPermissions()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_ID -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadNextActivity()
                } else {
                    showWarning()
                }
            }
        }
    }

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            loadNextActivity()
        } else {
            showWarning()
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSION_REQUEST_ID)
        }
    }

    private fun showWarning() {
        Toast.makeText(this, "You need to provide permissions!", Toast.LENGTH_SHORT).show()
    }

    private fun loadNextActivity() {
        when {
            secureStorage.isAuthenticated() -> callActivity(MobileActivity::class.java)
            else -> callActivity(LoginActivity::class.java)
        }
    }

    private fun callActivity(clazz: Class<*>) {
        val intent = Intent(this, clazz)
        startActivity(intent)
        finish()
    }
}
