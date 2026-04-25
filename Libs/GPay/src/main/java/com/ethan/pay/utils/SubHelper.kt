package com.ethan.pay.utils

object SubHelper {

    /**
     * 订阅商品id
     */
    private const val product_id_sub = "hidden.spycamera.premium"

    /**
     * 订阅商品plan_id
     */
    private const val plan_id_week = "spycamera-weekly"
    private const val plan_id_month = "spycamera-monthly"
    private const val plan_id_year = "spycamera-yearly"

    /**
     * todo 请注意，如果添加了lifetime套餐，请向该list添加！！！，不然无法识别lifetime权益,以及积分包添加！！！！
     */
    val listLifeGoodsList = listOf<String>()

    fun getProductId(): String {
        return product_id_sub
    }

    fun getWeekPlanId(): String {
        return plan_id_week
    }

    fun getMonthPlanId(): String {
        return plan_id_month
    }

    fun getYearPlanId(): String {
        return plan_id_year
    }

    fun getWeekSkuId(): String {
        return product_id_sub
    }

    fun getMonthSkuId(): String {
        return product_id_sub
    }

    fun getYearSkuId(): String {
        return product_id_sub
    }
}