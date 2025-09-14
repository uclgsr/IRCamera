package com.topdon.tc001.sensors.unified.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.R
import com.topdon.tc001.sensors.unified.model.PCControllerInfo

class PCControllerAdapter(
    private val onControllerClick: (PCControllerInfo) -> Unit
) : RecyclerView.Adapter<PCControllerAdapter.ControllerViewHolder>() {

    private val controllers = mutableListOf<PCControllerInfo>()
    private var selectedPosition = RecyclerView.NO_POSITION

    fun updateControllers(newControllers: List<PCControllerInfo>) {
        controllers.clear()
        controllers.addAll(PCControllerInfo.sortByPriority(newControllers))
        selectedPosition = RecyclerView.NO_POSITION
        notifyDataSetChanged()
    }

    fun getSelectedController(): PCControllerInfo? {
        return if (selectedPosition != RecyclerView.NO_POSITION) {
            controllers[selectedPosition]
        } else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ControllerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pc_controller, parent, false)
        return ControllerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ControllerViewHolder, position: Int) {
        holder.bind(controllers[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = controllers.size

    inner class ControllerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val controllerNameText: TextView = itemView.findViewById(R.id.controllerNameText)
        private val controllerAddressText: TextView =
            itemView.findViewById(R.id.controllerAddressText)
        private val controllerStatusText: TextView =
            itemView.findViewById(R.id.controllerStatusText)
        private val capabilitiesText: TextView = itemView.findViewById(R.id.capabilitiesText)
        private val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val previousPosition = selectedPosition
                    selectedPosition = position

                    notifyItemChanged(previousPosition)
                    notifyItemChanged(selectedPosition)

                    onControllerClick(controllers[position])
                }
            }
        }

        fun bind(controller: PCControllerInfo, isSelected: Boolean) {
            controllerNameText.text = controller.displayName
            controllerAddressText.text = controller.address
            controllerStatusText.text = controller.statusSummary

            val capabilities = buildString {
                if (controller.supportsGSR) append("GSR ")
                if (controller.supportsThermal) append("Thermal ")
                if (controller.supportsRGB) append("RGB ")
                if (controller.supportsSecure) append("TLS ")
            }
            capabilitiesText.text = capabilities.trim()

            itemView.setBackgroundColor(
                if (isSelected) Color.LTGRAY else Color.TRANSPARENT
            )

            val priorityColor = when {
                controller.connectionPriority >= 120 -> Color.GREEN  // High priority
                controller.connectionPriority >= 100 -> Color.BLUE   // Medium priority
                controller.connectionPriority >= 50 -> Color.YELLOW  // Low priority
                else -> Color.GRAY  // Very low priority
            }
            priorityIndicator.setBackgroundColor(priorityColor)

            if (controller.supportsGSR) {
                controllerNameText.setTextColor(Color.BLUE)
                controllerNameText.text = "${controller.displayName} ★"
            } else {
                controllerNameText.setTextColor(Color.BLACK)
            }

            if (controller.isRecentlyActive) {
                controllerStatusText.text = "${controller.statusSummary} • Active"
                controllerStatusText.setTextColor(Color.GREEN)
            } else {
                controllerStatusText.setTextColor(Color.GRAY)
            }

            if (controller.supportsSecure) {
                capabilitiesText.text = "${capabilities.trim()} 🔒"
            }
        }
    }
}
