package com.spyfinder.hiddencamera.detectorapp.event

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.ethan.firebaseAnalytics.analytics.FirebaseEvent
import com.ethan.firebaseAnalytics.attribution.OnReferrerCallback
import com.ethan.firebaseAnalytics.attribution.Referrer
import com.ethan.firebaseAnalytics.attribution.ReferrerSeeker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Event {
    private const val TAG = "Event"

    // 基础功能埋点名称，统一维护避免各页面散落魔法字符串。
    const val PAGE_VIEW = "page_view"
    const val TAB_CLICK = "tab_click"
    const val FEATURE_CLICK = "feature_click"
    const val WIFI_SCAN_START = "wifi_scan_start"
    const val WIFI_SCAN_CANCEL = "wifi_scan_cancel"
    const val WIFI_SCAN_COMPLETE = "wifi_scan_complete"
    const val WIFI_RESULT_CLICK = "wifi_result_click"
    const val WIFI_HISTORY_CLICK = "wifi_history_click"
    const val MAGNETIC_DETECT_START = "magnetic_detect_start"
    const val MAGNETIC_DETECT_STOP = "magnetic_detect_stop"
    const val CAMERA_SCANNER_CLICK = "camera_scanner_click"
    const val CAMERA_SCANNER_OPEN = "camera_scanner_open"
    const val SUBSCRIBE_GATE_SHOW = "subscribe_gate_show"
    const val SUBSCRIBE_ENTRY_CLICK = "subscribe_entry_click"
    const val SUBSCRIBE_PRODUCT_QUERY = "subscribe_product_query"
    const val SUBSCRIBE_PRODUCT_SELECT = "subscribe_product_select"
    const val SUBSCRIBE_CONTINUE_CLICK = "subscribe_continue_click"
    const val PURCHASE_BEGIN = "purchase_begin"
    const val PURCHASE_SUCCESS = "purchase_success"
    const val PURCHASE_OWNED = "purchase_owned"
    const val PURCHASE_FAILED = "purchase_failed"
    const val PURCHASE_CANCEL = "purchase_cancel"
    const val PURCHASE_DISCONNECT = "purchase_disconnect"

    // 常用参数名，保持 Firebase 控台维度命名稳定。
    const val PARAM_PAGE = "page"
    const val PARAM_TAB = "tab"
    const val PARAM_FEATURE = "feature"
    const val PARAM_SOURCE = "source"
    const val PARAM_ITEM = "item"
    const val PARAM_SUBSCRIBED = "subscribed"
    const val PARAM_PLAN_ID = "plan_id"
    const val PARAM_GOODS_ID = "goods_id"
    const val PARAM_SKU = "sku"
    const val PARAM_OFFER_ID = "offer_id"
    const val PARAM_PRICE = "price"
    const val PARAM_CURRENCY = "currency"
    const val PARAM_PRODUCT_COUNT = "product_count"
    const val PARAM_SUSPICIOUS_COUNT = "suspicious_count"
    const val PARAM_TRUSTED_COUNT = "trusted_count"
    const val PARAM_TOTAL_COUNT = "total_count"
    const val PARAM_REASON = "reason"
    const val PARAM_ORDER_COUNT = "order_count"
    const val PARAM_PROGRESS = "progress"
    const val PARAM_GAUGE = "gauge"

    fun start(context: Context) {
        ReferrerSeeker.find(context, object : OnReferrerCallback {
            override fun onSuccess(referrer: Referrer) {
                runCatching {
                    FirebaseEvent.open(context, referrer)
                }.onFailure {
                    Log.e(TAG, "上报启动归因成功回调失败", it)
                }
            }

            override fun onFailure(msg: String?) {
                runCatching {
                    FirebaseEvent.open(context, null)
                }.onFailure {
                    Log.e(TAG, "上报启动归因失败回调失败: $msg", it)
                }
            }
        })
    }

    fun event(context: Context, event: String, value: String) {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                FirebaseEvent.event(context, event, value)
            }.onFailure {
                Log.e(TAG, "埋点上报失败 event=$event value=$value", it)
            }
        }
    }

    fun event(context: Context, event: String, bundle: Bundle) {
        CoroutineScope(Dispatchers.Default).launch {
            runCatching {
                val bundle2 = Bundle(bundle)
                FirebaseEvent.event(context, event, bundle2)

                val logText = bundle.keySet().joinToString(",") { key -> "$key:${bundle.get(key)}" }
                Log.d(TAG, "埋点上报 event=$event params=[$logText]")
            }.onFailure {
                Log.e(TAG, "埋点上报失败 event=$event", it)
            }
        }
    }

    fun event(context: Context, event: String, vararg params: Pair<String, Any?>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            // Firebase 参数类型有限，这里集中做类型转换和空值保护。
            when (value) {
                null -> bundle.putString(key, "")
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
        event(context, event, bundle)
    }
}
