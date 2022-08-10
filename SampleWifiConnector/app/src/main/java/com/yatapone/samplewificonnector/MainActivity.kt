package com.yatapone.samplewificonnector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.*
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED
import android.net.wifi.WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.recyclerview.widget.LinearLayoutManager
import com.yatapone.samplewificonnector.databinding.ActivityMainBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 999
        private const val SECURITY_TYPE_WPA3 = "WPA3"
        private const val SECURITY_TYPE_WPA2 = "WPA2"
        private const val SECURITY_TYPE_WPA = "WPA"
        private const val SECURITY_TYPE_NA = "N/A"
    }

    private var permissionRejected: Boolean = false
    private var isDialogDisplayed: Boolean = false
    private lateinit var binding: ActivityMainBinding
    private lateinit var wifiListAdapter: WifiListAdapter
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var wifiManager: WifiManager
    private lateinit var wifiScanReceiver: BroadcastReceiver
    private lateinit var suggestionPostConnectionReceiver: BroadcastReceiver
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wifiListAdapter = WifiListAdapter { wifi -> onClickWifi(wifi) }
        binding.recyclerView.adapter = wifiListAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                Log.d(TAG, "onReceive: success=$success")
                // success or failure processing

                refreshWifiList()
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)
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

        refreshWifiList()

        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network : Network) {
                Log.d(TAG, "onAvailable: The default network is now: $network")
            }

            override fun onLost(network : Network) {
                Log.d(TAG, "onLost: The application no longer has a default network. The last default network was $network")
            }

            override fun onCapabilitiesChanged(network : Network, networkCapabilities : NetworkCapabilities) {
                Log.d(TAG, "onCapabilitiesChanged: The default network changed capabilities: $networkCapabilities")
            }

            override fun onLinkPropertiesChanged(network : Network, linkProperties : LinkProperties) {
                Log.d(TAG, "onLinkPropertiesChanged: The default network changed link properties: $linkProperties")
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: requestCode=$requestCode, permissions=${permissions[0]}, grantResults=${grantResults[0]}")
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            permissionRejected = true
        }
    }

    private fun refreshWifiList() {
        Log.d(TAG, "refreshWifiList: ")
        // refresh update time
        val localDateTime = LocalDateTime.now()
        val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd(E) HH:mm:ss")
        binding.statusText.text = dateTimeFormat.format(localDateTime)

        // refresh wifi list
        val wifiList: ArrayList<Wifi> = ArrayList()
        val scanResults: List<ScanResult> = wifiManager.scanResults
        scanResults.forEach {
            Log.d(TAG, "refreshWifiList: scanResults=$it")
            val ssid = it.SSID
            val waveLevel = it.level
            val securityType = when {
                it.capabilities.contains(SECURITY_TYPE_WPA3) -> SECURITY_TYPE_WPA3
                it.capabilities.contains(SECURITY_TYPE_WPA2) -> SECURITY_TYPE_WPA2
                it.capabilities.contains(SECURITY_TYPE_WPA) -> SECURITY_TYPE_WPA
                else -> SECURITY_TYPE_NA
            }

            // Comment out the following if you need to manage stealth wifi.
            if (ssid == null || ssid == "") { return@forEach }

            wifiList.add(Wifi(ssid, waveLevel, securityType))
        }
        wifiListAdapter.submitList(wifiList.sortedByDescending { it.waveLevel })
    }

    private fun onClickWifi(wifi: Wifi) {
        Log.d(TAG, "onClickWifi: wifi=$wifi")

        val editText = AppCompatEditText(this)
        AlertDialog.Builder(this)
            .setTitle("Connect to ${wifi.ssid}")
            .setMessage("input passphrase.")
            .setView(editText)
            .setPositiveButton("connect") { dialog, _ ->
                val pass = editText.text.toString()

                if (pass.isNotEmpty() && !pass.matches("^[A-Za-z0-9]+$".toRegex())) {
                    Log.d(TAG, "onClickWifi: input error")
                    Toast.makeText(this, "Please enter pass correctly (A-Za-z0-9).", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@setPositiveButton
                }

                if (binding.radioSuggestion.isChecked) {
                    connectByWifiNetworkSuggestion(wifi, pass)
                } else {
                    connectByWifiNetworkSpecifier(wifi, pass)
                }
                dialog.dismiss()
            }
            .setNegativeButton("cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun connectByWifiNetworkSuggestion(wifi: Wifi, pass: String) {
        Log.d(TAG, "connectByWifiNetworkSuggestion: wifi=$wifi, pass=$pass")
        val suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(wifi.ssid)
        when (wifi.securityType) {
            SECURITY_TYPE_WPA3 -> suggestion.setWpa3Passphrase(pass)
            SECURITY_TYPE_WPA2 -> suggestion.setWpa2Passphrase(pass)
            SECURITY_TYPE_WPA -> suggestion.setWpa2Passphrase(pass)
            SECURITY_TYPE_NA -> suggestion.setWpa2Passphrase(pass)
            else -> suggestion.setWpa2Passphrase(pass)
        }
        val suggestionsList = listOf(suggestion.build())
        val resultValue = wifiManager.addNetworkSuggestions(suggestionsList)
        val resultKey = when (resultValue) {
            STATUS_NETWORK_SUGGESTIONS_SUCCESS -> "STATUS_NETWORK_SUGGESTIONS_SUCCESS"
            STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL -> "STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL"
            STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED -> "STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED"
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE"
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP"
            STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID -> "STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID"
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_NOT_ALLOWED"
            STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID -> "STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_INVALID"
            else -> ""
        }
        Log.d(TAG, "connectByWifiNetworkSuggestion: result: $resultValue: $resultKey")
        Toast.makeText(this, "result: $resultValue: $resultKey", Toast.LENGTH_SHORT).show()


        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)
        suggestionPostConnectionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return
                }
                Log.d(TAG, "connectByWifiNetworkSuggestion: onReceive: ")
                // do post connect processing here
            }
        }
        registerReceiver(suggestionPostConnectionReceiver, intentFilter)
    }

    private fun connectByWifiNetworkSpecifier(wifi: Wifi, pass: String) {
        Log.d(TAG, "connectByWifiNetworkSpecifier: wifi=$wifi, pass=$pass")
        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(wifi.ssid)
        when (wifi.securityType) {
            SECURITY_TYPE_WPA3 -> specifier.setWpa3Passphrase(pass)
            SECURITY_TYPE_WPA2 -> specifier.setWpa2Passphrase(pass)
            SECURITY_TYPE_WPA -> specifier.setWpa2Passphrase(pass)
            SECURITY_TYPE_NA -> specifier.setWpa2Passphrase(pass)
            else -> specifier.setWpa2Passphrase(pass)
        }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier.build())
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "onAvailable: network=$network")
                // do success processing here..
            }
            override fun onUnavailable() {
                super.onUnavailable()
                Log.d(TAG, "onAvailable: ")
                // do failure processing here..
            }
        }
        connectivityManager.requestNetwork(request, networkCallback)
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: ")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        unregisterReceiver(wifiScanReceiver)

        try {
            unregisterReceiver(suggestionPostConnectionReceiver)
        } catch (e: Exception) {
            Log.d(TAG, "onDestroy: unregisterReceiver: e=$e")
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.d(TAG, "onDestroy: unregisterNetworkCallback: e=$e")
        }

        super.onDestroy()
    }
}