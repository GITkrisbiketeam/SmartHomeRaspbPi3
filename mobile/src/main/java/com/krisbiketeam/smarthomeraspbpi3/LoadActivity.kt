package com.krisbiketeam.smarthomeraspbpi3

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.krisbiketeam.smarthomeraspbpi3.ui.HomeActivity
import timber.log.Timber

private const val PERMISSION_REQUEST_ID = 999

class LoadActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        Timber.d("PlayServicesAvailable status: $status")
        if (status != ConnectionResult.SUCCESS) {
            Timber.d("Missing or outdated Play Services Version")
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
                    .addOnCompleteListener {
                        Timber.d("Play Services updated task: ${it.isSuccessful}")
                        if (it.isSuccessful) {
                            requestPermissions()
                        } else {
                            Toast.makeText(this, "You need to install or update Play Services!", Toast.LENGTH_SHORT)
                                    .show()
                        }
                    }
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_ID -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callHomeActivity()
                } else {
                    showWarning()
                }
            }
        }
    }

    private fun requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            callHomeActivity()
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

    private fun callHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}
