package com.topdon.tc001.sensors.shimmer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.topdon.tc001.sensors.shimmer.model.ConnectionQuality
import com.topdon.tc001.sensors.shimmer.model.ShimmerDeviceInfo

class ShimmerDeviceAdapter(
    private val onDeviceSelected: (ShimmerDeviceInfo) -> Unit
) : RecyclerView.Adapter<ShimmerDeviceAdapter.DeviceViewHolder>() {

    private var devices = listOf<ShimmerDeviceInfo>()
    private var selectedDeviceMAC: String? = null

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val deviceNameText: TextView = itemView.findViewById(R.id.deviceNameText)
        private val deviceMacText: TextView = itemView.findViewById(R.id.deviceMacText)
        private val deviceTypeText: TextView = itemView.findViewById(R.id.deviceTypeText)
        private val signalStrengthText: TextView = itemView.findViewById(R.id.signalStrengthText)
        private val connectionStatusText: TextView =
            itemView.findViewById(R.id.connectionStatusText)
        private val priorityText: TextView = itemView.findViewById(R.id.priorityText)

        fun bind(device: ShimmerDeviceInfo) {

            deviceNameText.text = device.name
            deviceMacText.text = device.macAddress
            deviceTypeText.text = device.deviceType

            val quality = ConnectionQuality.fromRSSI(device.rssi)
            signalStrengthText.text = "${device.rssi} dBm (${quality.displayName})"
            signalStrengthText.setTextColor(Color.parseColor(quality.color))

            connectionStatusText.text = device.getDetailedStatus()

            priorityText.text = "Priority: ${device.priority}"

            val isSelected = device.macAddress == selectedDeviceMAC
            itemView.setBackgroundColor(
                if (isSelected) Color.parseColor("#E3F2FD") else Color.TRANSPARENT
            )

            itemView.setOnClickListener {
                selectedDeviceMAC = device.macAddress
                notifyDataSetChanged()
                onDeviceSelected(device)
            }

            itemView.alpha = if (device.isReadyForConnection()) 1.0f else 0.6f
            itemView.isClickable = device.isReadyForConnection()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shimmer_device_detailed, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    fun updateDevices(newDevices: List<ShimmerDeviceInfo>) {
        devices = newDevices
        notifyDataSetChanged()
    }

    fun clearDevices() {
        devices = emptyList()
        selectedDeviceMAC = null
        notifyDataSetChanged()
    }

    fun getSelectedDevice(): ShimmerDeviceInfo? {
        return devices.find { it.macAddress == selectedDeviceMAC }
    }
}
