package com.ethan.pay

import android.content.Context
import com.ethan.pay.impl.ClientController
import com.ethan.pay.impl.GPayImpl
import com.ethan.pay.impl.PurchaseType
import com.ethan.pay.lifetime.LifeTimeImpl
import com.ethan.pay.lifetime.LifeTimeSkuImpl
import com.ethan.pay.model.OrderInfo
import com.ethan.pay.onetime.OneTimeImpl
import com.ethan.pay.onetime.OneTimeSkuImpl
import com.ethan.pay.subs.SubscribeImpl
import com.ethan.pay.subs.SubscribeSkuImpl


object BillFactory {

    private var subProductImpl: GPayImpl? = null
    private var subSkuImpl: GPayImpl? = null
    private var lifeProductImpl: GPayImpl? = null
    private var lifeSkuImpl: GPayImpl? = null
    private var oneProductImpl: GPayImpl? = null
    private var oneSkuImpl: GPayImpl? = null

    suspend fun init(context: Context): Int {
        return ClientController.connect(context)
    }

    fun isSupport(): Boolean {
        return ClientController.isSupport()
    }

    fun getSubscribe(): GPayImpl {
        return if (ClientController.isSupport()) {
            if (subProductImpl == null) subProductImpl = SubscribeImpl()
            subProductImpl!!
        } else {
            if (subSkuImpl == null) subSkuImpl = SubscribeSkuImpl()
            subSkuImpl!!
        }
    }

    fun getLifeTime(): GPayImpl {
        return if (ClientController.isSupport()) {
            if (lifeProductImpl == null) lifeProductImpl = LifeTimeImpl()
            lifeProductImpl!!
        } else {
            if (lifeSkuImpl == null) lifeSkuImpl = LifeTimeSkuImpl()
            lifeSkuImpl!!
        }
    }

    fun getOneTime(): GPayImpl {
        return if (ClientController.isSupport()) {
            if (oneProductImpl == null) oneProductImpl = OneTimeImpl()
            oneProductImpl!!
        } else {
            if (oneSkuImpl == null) oneSkuImpl = OneTimeSkuImpl()
            oneSkuImpl!!
        }
    }



    /**
     * 不要在多个地方设置这个变量！！！！！！！！！！！！！！！！！！！
     */
    var globalSuccessCallBack: (orderList: MutableList<OrderInfo>, type: PurchaseType) -> Unit = { _, _ -> }
        set(value) {
            ClientController.globalSuccessCallBack = value
            field = value
        }
        get() {
            return ClientController.globalSuccessCallBack
        }
}