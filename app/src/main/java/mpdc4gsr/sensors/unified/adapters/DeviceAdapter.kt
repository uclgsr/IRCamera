package mpdc4gsr.sensors.unified.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import mpdc4gsr.sensors.unified.model.DeviceInfo

class DeviceAdapter(
    private val onDeviceClick: (DeviceInfo) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<DeviceInfo>()
    private var selectedPosition = RecyclerView.NO_POSITION

    fun updateDevices(newDevices: List<DeviceInfo>) {
        devices.clear()
        devices.addAll(DeviceInfo.sortByPriority(newDevices))
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun getSelectedDevice(): DeviceInfo? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            devices[selectedPosition]
        } else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shimmer_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceNameText: TextView = itemView.findViewById(R.id.deviceNameText)
        private val deviceAddressText: TextView = itemView.findViewById(R.id.deviceAddressText)
        private val connectionStatusText: TextView =
            itemView.findViewById(R.id.connectionStatusText)
        private val signalStrengthText: TextView = itemView.findViewById(R.id.signalStrengthText)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val previousPosition = selectedPosition
                    selectedPosition = position

                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)

                    onDeviceClick(devices[position])
                }
            }
        }

        fun bind(device: DeviceInfo, isSelected: Boolean) {
            deviceNameText.text = device.displayName
            deviceAddressText.text = device.address
            connectionStatusText.text = device.statusSummary
            signalStrengthText.text = "${device.rssi} dBm"

            itemView.setBackgroundColor(
                if (isSelected) Color.LTGRAY else Color.TRANSPARENT
            )

            signalStrengthText.setTextColor(
                when (device.signalStrength) {
                    DeviceInfo.SignalStrength.EXCELLENT -> Color.GREEN
                    DeviceInfo.SignalStrength.GOOD -> Color.BLUE
                    DeviceInfo.SignalStrength.FAIR -> Color.YELLOW
                    DeviceInfo.SignalStrength.POOR -> 0xFFFF8800.toInt()
                    DeviceInfo.SignalStrength.VERY_POOR -> Color.RED
                }
            )

            if (device.isGSRPlusDevice) {
                deviceNameText.setTextColor(Color.BLUE)
                deviceNameText.text = "${device.name} ★"
            } else {
                deviceNameText.setTextColor(Color.BLACK)
            }

            if (device.isRecommended) {
                connectionStatusText.text = "${device.statusSummary} • Recommended"
                connectionStatusText.setTextColor(Color.GREEN)
            } else {
                connectionStatusText.setTextColor(Color.GRAY)
            }
        }
    }
}
