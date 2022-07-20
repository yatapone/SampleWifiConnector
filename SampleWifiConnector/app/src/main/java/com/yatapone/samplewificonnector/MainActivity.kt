package com.yatapone.samplewificonnector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.yatapone.samplewificonnector.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 999
    }

    private lateinit var binding: ActivityMainBinding
    private val wifiListAdapter = WifiListAdapter()
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiScanReceiver: BroadcastReceiver
    private var permissionRejected: Boolean = false
    private var isDialogDisplayed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerView.adapter = wifiListAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                Log.d(TAG, "onReceive: success=$success")
                if (success) {
                    scanSuccess()
                } else {
                    scanFailure()
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        applicationContext.registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onResume: permission.ACCESS_FINE_LOCATION is granted.")
        } else {
            Log.d(TAG, "onResume: permission.ACCESS_FINE_LOCATION is required.")
            if (!permissionRejected) {
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            } else if (!isDialogDisplayed) {
                isDialogDisplayed = true
                AlertDialog.Builder(this)
                    .setTitle("a permission is required")
                    .setMessage("ACCESS_FINE_LOCATION permission is required for scanning Wi-Fi list. Restart app and grant ACCESS_FINE_LOCATION permission.")
                    .setPositiveButton("OK") { _, _ ->
                        finish()
                    }
                    .show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: requestCode=$requestCode, permissions=${permissions[0]}, grantResults=${grantResults[0]}")
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            permissionRejected = true
        }
    }

    private fun scanSuccess() {
        Log.d(TAG, "scanSuccess: ")

        val wifiStatus = wifiManager.wifiState
        Log.d(TAG, "scanWifi: wifiState=$wifiStatus")
        if (wifiStatus == WifiManager.WIFI_STATE_ENABLED) {
            val wifiListNew: ArrayList<Wifi> = ArrayList()
            val scanResults: List<ScanResult> = wifiManager.scanResults
            scanResults.forEach {
                Log.d(TAG, "scanWifi: scanResults=$it")
                val ssid = it.SSID
                val waveLevel = it.level
                val securityType = when {
                    it.capabilities.contains("WEP") -> "WEP"
                    it.capabilities.contains("PSK") -> "PSK"
                    it.capabilities.contains("EAP") -> "EAP"
                    else -> "N/A"
                }
                if (ssid != null && ssid != "") {
                    wifiListNew.add(Wifi(ssid, waveLevel, securityType))
                }
            }
            wifiListAdapter.submitList(wifiListNew.sortedByDescending { it.waveLevel })
        } else {
            wifiListAdapter.submitList(emptyList())
        }
    }

    private fun scanFailure() {
        Log.d(TAG, "scanFailure: ")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        applicationContext.unregisterReceiver(wifiScanReceiver)
        super.onDestroy()
    }
}