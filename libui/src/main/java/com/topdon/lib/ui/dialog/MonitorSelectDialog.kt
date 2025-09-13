package com.topdon.lib.ui.dialog

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.topdon.lib.core.R
import com.topdon.lib.core.utils.ScreenUtil
import com.topdon.lib.ui.databinding.DialogMonitorSelectBinding

/**
 * tip窗
 * create by fylder on 2018/6/15
 **/
/**
 * MonitorSelectDialog(context: class
 */
/**
 * Monitor select dialog for thermal imaging user interaction.
 * Provides specialized input and configuration interfaces.
 */
/**
 * MonitorSelectDialog displays modal dialog interface for user interaction.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
class MonitorSelectDialog(context: Context) : Dialog(context, R.style.InfoDialog) {
    /**
     * Builder(private class
     */
/**
 * Builder dialog for thermal imaging user interaction.
 * Provides specialized input and configuration interfaces.
 */
/**
 * Builder manages camera operations and image capture functionality.
 *
 * @author IRCamera Development Team
 * @since 1.0
 */
    class Builder(private val context: Context) {
        /**
         * 是否处于第 1 步.
         */
        private var isFirstStep = true

        /**
         * currentselected的监控type 1-point 2-line 3-area.
         */
        private var monitorType = 0

        private var positiveClickListener: ((select: Int) -> Unit)? = null

        fun setPositiveListener(listener: ((select: Int) -> Unit)?): Builder {
            this.positiveClickListener = listener
            return this
        }

    /**
     * Creates and configures a new  instance.
     */
        fun create(): MonitorSelectDialog {
            val dialog = MonitorSelectDialog(context)
            dialog.setCanceledOnTouchOutside(false)

            val binding = DialogMonitorSelectBinding.inflate(LayoutInflater.from(context))
            dialog.setContentView(binding.root)

            val lp = dialog.window!!.attributes
            lp.width = (ScreenUtil.getScreenWidth(context) * if (ScreenUtil.isPortrait(context)) 0.85 else 0.35).toInt() 
            dialog.window!!.attributes = lp

            binding.btnConfirmOrBack.setOnClickListener {
                if (isFirstStep) { // 步骤1->步骤2 逻辑为“Confirm”
                    if (monitorType == 0) { 
                        return@setOnClickListener
                    }
                    isFirstStep = false
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.clFirstStep.visibility = View.INVISIBLE
                    binding.clSecondStep.visibility = View.VISIBLE
                    binding.tvTitle.text = context.getString(R.string.select_monitor_type_step2)
                    binding.btnConfirmOrBack.text = context.getString(R.string.select_monitor_return)
                } else { // 步骤2->步骤1 逻辑为“Return”
                    isFirstStep = true
                    binding.btnCancel.visibility = View.GONE
                    binding.clFirstStep.visibility = View.VISIBLE
                    binding.clSecondStep.visibility = View.GONE
                    binding.tvTitle.text = context.getString(R.string.select_monitor_type_step1)
                    binding.btnConfirmOrBack.text = context.getString(R.string.app_confirm)
                }
            }

            binding.btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            binding.btnSelectLocation.setOnClickListener {
                dialog.dismiss()
                positiveClickListener?.invoke(monitorType)
            }

            binding.tvPoint.setOnClickListener {
                updateUI(binding, 1)
            }
            binding.tvLine.setOnClickListener {
                updateUI(binding, 2)
            }
            binding.tvRect.setOnClickListener {
                updateUI(binding, 3)
            }
            return dialog
        }

    /**
     * Updates the ui with new data.
     */
        private fun updateUI(
            binding: DialogMonitorSelectBinding,
            index: Int,
        ) {
            binding.tvPoint.isSelected = index == 1
            binding.tvLine.isSelected = index == 2
            binding.tvRect.isSelected = index == 3
            monitorType = index
        }
    }
}
