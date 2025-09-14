package com.topdon.tc001.gsr

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.csl.irCamera.databinding.ItemGsrDataRowBinding


class GSRDataRowAdapter(
    private val dataRows: List<GSRDataViewActivity.GSRDataRow>,
) : RecyclerView.Adapter<GSRDataRowAdapter.ViewHolder>() {
    class ViewHolder(private val binding: ItemGsrDataRowBinding) : RecyclerView.ViewHolder(binding.root) {
    val rowNumber = binding.rowNumber
    val timestamp = binding.timestamp
    val gsrValue = binding.gsrValue
    val resistance = binding.resistance
    val conductance = binding.conductance
    }

    override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
    ): ViewHolder {
        val binding =
            ItemGsrDataRowBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int,
    ) {
    val dataRow = dataRows[position]

    holder.rowNumber.text = dataRow.rowNumber.toString()
    holder.timestamp.text = dataRow.timestamp
    holder.gsrValue.text = "%.3f μS".format(dataRow.gsrValue)
    holder.resistance.text = "%.1f kΩ".format(dataRow.resistance / 1000)
    holder.conductance.text = "%.6f S".format(dataRow.conductance)
    }

    override fun getItemCount() = minOf(dataRows.size, 100) // Limit to first 100 rows for performance
}
