package com.topdon.tc001.gsr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R


class GSRDeviceAdapter(
    private val devices: MutableList<GSRDeviceInfo>,
    private val onDeviceClick: (GSRDeviceInfo) -> Unit,
) : RecyclerView.Adapter<GSRDeviceAdapter.DeviceViewHolder>() {
    companion object {
        private const val TAG = "GSRDeviceAdapter"
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): DeviceViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_gsr_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: DeviceViewHolder,
        position: Int,
    ) {
        val device = devices[position]
        holder.bind(device, onDeviceClick)
    }

    override fun getItemCount(): Int = devices.size


    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceNameText: TextView = itemView.findViewById(R.id.deviceNameText)
        private val deviceAddressText: TextView = itemView.findViewById(R.id.deviceAddressText)
        private val connectionStatusText: TextView = itemView.findViewById(R.id.connectionStatusText)
        private val signalStrengthText: TextView = itemView.findViewById(R.id.signalStrengthText)
        private val batteryLevelText: TextView = itemView.findViewById(R.id.batteryLevelText)
        private val firmwareVersionText: TextView = itemView.findViewById(R.id.firmwareVersionText)

        private val deviceIcon: ImageView = itemView.findViewById(R.id.deviceIcon)
        private val connectionStatusIcon: ImageView = itemView.findViewById(R.id.connectionStatusIcon)
        private val batteryProgressBar: ProgressBar = itemView.findViewById(R.id.batteryProgressBar)
        private val signalStrengthProgressBar: ProgressBar = itemView.findViewById(R.id.signalStrengthProgressBar)

        fun bind(
            device: GSRDeviceInfo,
            onDeviceClick: (GSRDeviceInfo) -> Unit,
        ) {
            // Basic device information
            deviceNameText.text = device.name
            deviceAddressText.text = device.address
            firmwareVersionText.text = "Firmware: ${device.firmwareVersion}"

            // Connection status
            connectionStatusText.text = if (device.isConnected) "Connected" else "Available"
            connectionStatusText.setTextColor(
                if (device.isConnected) {
                    itemView.context.getColor(android.R.color.holo_green_dark)
                } else {
                    itemView.context.getColor(android.R.color.holo_orange_dark)
                },
            )

            // Connection status icon
            connectionStatusIcon.setImageResource(
                if (device.isConnected) {
                    R.drawable.ic_bluetooth_connected
                } else {
                    R.drawable.ic_bluetooth_searching
                },
            )

            // Signal strength
            val signalStrengthPercent = calculateSignalStrengthPercent(device.rssi)
            signalStrengthText.text = "Signal: $signalStrengthPercent%"
            signalStrengthProgressBar.progress = signalStrengthPercent

            // Battery level
            batteryLevelText.text = "Battery: ${device.batteryLevel}%"
            batteryProgressBar.progress = device.batteryLevel

            // Device icon - use Shimmer GSR specific icon
            deviceIcon.setImageResource(R.drawable.ic_device_type_shimmer_gsr)

            // Click handler
            itemView.setOnClickListener {
                onDeviceClick(device)
            }

            // Enable/disable based on connection status
            itemView.alpha = if (device.isConnected) 1.0f else 0.8f
        }


        private fun calculateSignalStrengthPercent(rssi: Int): Int {
            return when {
                rssi >= -30 -> 100 // Excellent signal
                rssi >= -50 -> 80 // Good signal
                rssi >= -70 -> 60 // Fair signal
                rssi >= -85 -> 40 // Weak signal
                else -> 20 // Very weak signal
            }.coerceIn(0, 100)
        }
    }
}
