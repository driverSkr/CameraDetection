package com.ethan.cameradetection.model

data class WifiInfoModel(
    var deviceType: Int,     // 1:路由器、2：摄像头、3：电脑、4：手机、5：打印机
    var name: String,
    var ip: String,
    var mac: String,
    var isSafe: Boolean
)
