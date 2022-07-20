package com.yatapone.samplewificonnector

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yatapone.samplewificonnector.databinding.WifiListCellBinding

class WifiListAdapter : ListAdapter<Wifi, WifiListAdapter.WifiListViewHolder>(DiffCallback()) {
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
            binding.waveLevel.text = wifi.waveLevel.toString()
            binding.securityType.text = wifi.securityType
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Wifi>() {
        override fun areItemsTheSame(oldItem: Wifi, newItem: Wifi): Boolean {
            //            Log.d("DiffCallback", "areItemsTheSame: oldItem=$oldItem, newItem=$newItem")
            return oldItem.ssid == newItem.ssid
        }

        override fun areContentsTheSame(oldItem: Wifi, newItem: Wifi): Boolean {
            //            Log.d("DiffCallback", "areContentsTheSame: oldItem=$oldItem, newItem=$newItem")
            return oldItem == newItem
        }
    }

}



