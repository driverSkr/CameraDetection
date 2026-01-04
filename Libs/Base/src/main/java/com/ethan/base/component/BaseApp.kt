package com.ethan.base.component

import android.app.Application
import android.os.Build
import android.os.Looper
import android.os.Process
import android.util.Log
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

abstract class BaseApp : Application() {

    companion object {
        const val GC_CALL_DELAY: Long = 15000
    }

    // 记录内存触顶率的队列
    private val proportionQueue: Queue<Float> = LinkedList<Float>()

    // 添加内存触顶率到队列中
    private fun addProportion(proportion: Float) { // 如果队列已满，移除最早的记录
        if (proportionQueue.size >= 10) {
            proportionQueue.poll()
        }
        proportionQueue.offer(proportion)
    }

    // 打印最近10次内存触顶率的图片形式
    private fun printMemoryProportions() {
        val TAG = "MemoryProportions"
        val scale = 50    // 图片打印的最大长度
        val sb = StringBuilder()
        for (proportion in proportionQueue) {
            val length = (scale * (proportion - 0F) / (1F - 0F)).toInt()
            val bar = "=".repeat(length)
            sb.append(String.format("|| %3.1f%% |%s\n", proportion * 100, bar))
        }
        Log.e(TAG, "===最近10次内存触顶率===\n$sb=====================")
    }

    private var mGcRunning: Boolean = false

    private val mExec = Executors.newSingleThreadScheduledExecutor { r ->
        object : Thread(r) {
            override fun run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST)
                super.run()
            }
        }
    }

    override fun onCreate() {
        super.onCreate() // 试运行:每隔固定时间,在消息队列闲时执行GC,降低内存峰值
        mExec.scheduleWithFixedDelay(Runnable {
            if (mGcRunning) return@Runnable

            val javaMax = Runtime.getRuntime().maxMemory()
            val javaTotal = Runtime.getRuntime().totalMemory();
            val javaUsed = javaTotal - Runtime.getRuntime().freeMemory();
            val proportion = javaUsed / javaMax.toFloat()
            addProportion(proportion)
            printMemoryProportions()

            mGcRunning = true
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Looper.getMainLooper().queue.addIdleHandler {
                    Runtime.getRuntime().gc()
                    mGcRunning = false
                    return@addIdleHandler false
                }
            }
        }, GC_CALL_DELAY, GC_CALL_DELAY, TimeUnit.MILLISECONDS)

        initLibs()
    }

    abstract fun initLibs()
}