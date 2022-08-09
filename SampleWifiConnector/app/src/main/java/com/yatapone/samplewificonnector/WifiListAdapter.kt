package com.yatapone.samplewificonnector

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yatapone.samplewificonnector.databinding.WifiListCellBinding

class WifiListAdapter(private val clickListener: (wifi: Wifi) -> Unit) : ListAdapter<Wifi, WifiListAdapter.WifiListViewHolder>(DiffCallback()) {
    companion object {
        private const val TAG = "WifiListAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiListViewHolder {
        val binding = WifiListCellBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WifiListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WifiListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WifiListViewHolder(private val binding: WifiListCellBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(wifi: Wifi) {
            binding.ssid.text = wifi.ssid
            val waveLevelText = wifi.waveLevel.toString() + " dBm"
            binding.waveLevel.text = waveLevelText
            binding.securityType.text = wifi.securityType

            binding.wifiListCell.setOnClickListener { clickListener(wifi) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Wifi>() {
        override fun areItemsTheSame(oldItem: Wifi, newItem: Wifi): Boolean {
            // Log.d("DiffCallback", "areItemsTheSame: oldItem=$oldItem, newItem=$newItem")
            return oldItem.ssid == newItem.ssid
        }

        override fun areContentsTheSame(oldItem: Wifi, newItem: Wifi): Boolean {
            // Log.d("DiffCallback", "areContentsTheSame: oldItem=$oldItem, newItem=$newItem")
            return oldItem == newItem
        }
    }

}



