package mpdc4gsr.sensors.gsr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import mpdc4gsr.R
import mpdc4gsr.sensors.unified.model.DeviceInfo


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


        holder.deviceName.text = device.name.ifEmpty { "Unknown Shimmer" }
        holder.deviceAddress.text = device.address
        holder.deviceType.text = device.deviceType


        val rssiText = "${device.rssi} dBm"
        holder.signalStrength.text = rssiText


        val signalIconRes = when {
            device.rssi >= -50 -> R.drawable.ic_bluetooth_disabled
            device.rssi >= -60 -> R.drawable.ic_bluetooth_disabled
            device.rssi >= -70 -> R.drawable.ic_bluetooth_disabled
            device.rssi >= -80 -> R.drawable.ic_bluetooth_disabled
            else -> R.drawable.ic_bluetooth_disabled
        }

        try {
            holder.signalIcon.setImageResource(signalIconRes)
        } catch (e: Exception) {

            holder.signalIcon.visibility = View.GONE
        }


        if (device.isGSRCapable) {
            try {
                holder.gsrCapableIcon.setImageResource(R.drawable.ic_device_type_shimmer_gsr)
                holder.gsrCapableIcon.visibility = View.VISIBLE
            } catch (e: Exception) {

                holder.gsrCapableIcon.visibility = View.GONE
            }
        } else {
            holder.gsrCapableIcon.visibility = View.GONE
        }


        if (device == selectedDevice) {
            try {
                holder.root.setBackgroundResource(R.drawable.item_background_selected)
            } catch (e: Exception) {

                holder.root.alpha = 0.8f
            }
        } else {
            try {
                holder.root.setBackgroundResource(R.drawable.item_background_default)
            } catch (e: Exception) {

                holder.root.alpha = 1.0f
            }
        }


        holder.root.setOnClickListener {
            selectDevice(device, position)
            onDeviceClick(device)
        }
    }

    override fun getItemCount(): Int = devices.size


    fun updateDevices(newDevices: List<DeviceInfo>) {
        val diffCallback = DeviceInfoDiffCallback(devices, newDevices)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        devices.clear()
        devices.addAll(newDevices)
        diffResult.dispatchUpdatesTo(this)
    }


    fun clearDevices() {
        val oldSize = devices.size
        devices.clear()
        selectedDevice = null
        notifyItemRangeRemoved(0, oldSize)
    }


    fun selectDevice(device: DeviceInfo, position: Int) {
        val previousSelection = selectedDevice
        selectedDevice = device


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
