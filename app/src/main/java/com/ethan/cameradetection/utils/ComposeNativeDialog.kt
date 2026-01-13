package com.ethan.cameradetection.utils


import android.view.LayoutInflater
import androidx.fragment.app.FragmentActivity
import com.blankj.utilcode.util.ScreenUtils
import com.ethan.base.dialog.BaseDialog
import com.ethan.cameradetection.R
import com.ethan.cameradetection.databinding.DialogComposeContainerBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


object ComposeNativeDialog {

    fun composeBaseDialog(activity: FragmentActivity, dismiss: () -> Unit = {}): Pair<DialogComposeContainerBinding, BaseDialog> {
        val binding = DialogComposeContainerBinding.inflate(LayoutInflater.from(activity))
        val dialog = BaseDialog.Builder(activity).setWidth((ScreenUtils.getAppScreenWidth() * 0.8F).toInt())
            .setOnDismissListener { dismiss.invoke() }.setView(binding.root).create()
        return binding to dialog
    }

    fun composeBottomDialog(activity: FragmentActivity, dismiss: () -> Unit = {}): Pair<DialogComposeContainerBinding, BottomSheetDialog> {
        val binding = DialogComposeContainerBinding.inflate(LayoutInflater.from(activity))
        val sheetDialog = BottomSheetDialog(activity, R.style.ComposeBottomSheetDialog).apply {
            setOnDismissListener { dismiss.invoke() }
            setDismissWithAnimation(true) // 确保启用关闭动画
            window?.setWindowAnimations(R.style.BottomSheetAnimation)
            behavior.apply {
                isFitToContents = true // 弹窗高度会根据内容自适应
                skipCollapsed = true // 关闭时直接完全收起，跳过中间状态
                state = BottomSheetBehavior.STATE_EXPANDED // 初始状态为完全展开
            }
            window?.setBackgroundDrawableResource(android.R.color.transparent)
        }
        sheetDialog.setContentView(binding.root)
        return binding to sheetDialog
    }
}