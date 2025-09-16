package com.topdon.tc001.gsr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.topdon.tc001.R
import com.topdon.tc001.sensors.unified.model.DeviceInfo

/**
 * RecyclerView Adapter for Shimmer Device List
 * 
 * Displays discovered Shimmer3 GSR+ devices with:
 * - Device name and MAC address
 * - Signal strength (RSSI) indicator
 * - Device type and capabilities
 * - Connection status
 * 
 * Uses DiffUtil for efficient updates instead of notifyDataSetChanged()
 */
class ShimmerDeviceAdapter(
    private val onDeviceClick: (DeviceInfo) -> Unit
) : RecyclerView.Adapter<ShimmerDeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<DeviceInfo>()
    private var selectedDevice: DeviceInfo? = null

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceName: TextView = itemView.findViewById(R.id.textDeviceName)
        val deviceAddress: TextView = itemView.findViewById(R.id.textDeviceAddress)
        val deviceType: TextView = itemView.findViewById(R.id.textDeviceType)
        val signalStrength: TextView = itemView.findViewById(R.id.textSignalStrength)
        val signalIcon: ImageView = itemView.findViewById(R.id.imageSignalStrength)
        val gsrCapableIcon: ImageView = itemView.findViewById(R.id.imageGsrCapable)
        val root: View = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shimmer_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        
        // Set device information
        holder.deviceName.text = device.name.ifEmpty { "Unknown Shimmer" }
        holder.deviceAddress.text = device.address
        holder.deviceType.text = device.deviceType
        
        // Set signal strength
        val rssiText = "${device.rssi} dBm"
        holder.signalStrength.text = rssiText
        
        // Set signal strength icon based on RSSI value
        val signalIconRes = when {
            device.rssi >= -50 -> R.drawable.ic_bluetooth_disabled // Excellent - reusing available icon
            device.rssi >= -60 -> R.drawable.ic_bluetooth_disabled // Good 
            device.rssi >= -70 -> R.drawable.ic_bluetooth_disabled // Fair
            device.rssi >= -80 -> R.drawable.ic_bluetooth_disabled // Poor
            else -> R.drawable.ic_bluetooth_disabled // Very poor
        }
        
        try {
            holder.signalIcon.setImageResource(signalIconRes)
        } catch (e: Exception) {
            // Fallback if signal strength icons don't exist
            holder.signalIcon.visibility = View.GONE
        }
        
        // Set GSR capability indicator
        if (device.isGSRCapable) {
            try {
                holder.gsrCapableIcon.setImageResource(R.drawable.ic_device_type_shimmer_gsr)
                holder.gsrCapableIcon.visibility = View.VISIBLE
            } catch (e: Exception) {
                // Fallback if icon doesn't exist
                holder.gsrCapableIcon.visibility = View.GONE
            }
        } else {
            holder.gsrCapableIcon.visibility = View.GONE
        }
        
        // Highlight selected device
        if (device == selectedDevice) {
            try {
                holder.root.setBackgroundResource(R.drawable.item_background_selected)
            } catch (e: Exception) {
                // Fallback highlighting without drawable
                holder.root.alpha = 0.8f
            }
        } else {
            try {
                holder.root.setBackgroundResource(R.drawable.item_background_default)
            } catch (e: Exception) {
                // Fallback default appearance
                holder.root.alpha = 1.0f
            }
        }
        
        // Set click listener
        holder.root.setOnClickListener {
            selectDevice(device, position)
            onDeviceClick(device)
        }
    }

    override fun getItemCount(): Int = devices.size

    /**
     * Efficiently update devices using DiffUtil instead of notifyDataSetChanged()
     */
    fun updateDevices(newDevices: List<DeviceInfo>) {
        val diffCallback = DeviceInfoDiffCallback(devices, newDevices)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        devices.clear()
        devices.addAll(newDevices)
        diffResult.dispatchUpdatesTo(this)
    }
    
    /**
     * Clear all devices efficiently
     */
    fun clearDevices() {
        val oldSize = devices.size
        devices.clear()
        selectedDevice = null
        notifyItemRangeRemoved(0, oldSize)
    }
    
    /**
     * Select a device and update only the affected items
     */
    fun selectDevice(device: DeviceInfo, position: Int) {
        val previousSelection = selectedDevice
        selectedDevice = device
        
        // Refresh only the previously selected item and the new selection
        if (previousSelection != null) {
            val previousIndex = devices.indexOfFirst { it == previousSelection }
            if (previousIndex >= 0) {
                notifyItemChanged(previousIndex)
            }
        }
        notifyItemChanged(position)
    }
    
    fun getDeviceByAddress(address: String): DeviceInfo? {
        return devices.find { it.address == address }
    }
    
    fun getSelectedDevice(): DeviceInfo? = selectedDevice
    
    /**
     * DiffUtil callback for efficient list updates
     */
    private class DeviceInfoDiffCallback(
        private val oldList: List<DeviceInfo>,
        private val newList: List<DeviceInfo>
    ) : DiffUtil.Callback() {
        
        override fun getOldListSize(): Int = oldList.size
        
        override fun getNewListSize(): Int = newList.size
        
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].address == newList[newItemPosition].address
        }
        
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            
            return oldItem.name == newItem.name &&
                    oldItem.rssi == newItem.rssi &&
                    oldItem.deviceType == newItem.deviceType &&
                    oldItem.isGSRCapable == newItem.isGSRCapable
        }
    }
}