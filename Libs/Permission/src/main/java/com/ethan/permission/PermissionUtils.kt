package com.ethan.permission

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.blankj.utilcode.util.PermissionUtils
import java.util.*

/**
 * 1.建议放在BaseActivity里面
 * 2.提供一个PermissionUtils类变量让子类继承
 * 3.在BaseActivity onCreate里面init
 *
 */
class PermissionUtils {


    private var launcherOne: ActivityResultLauncher<Intent>? = null
    private var launcherTow: ActivityResultLauncher<Array<String>>? = null
    private var launcherThree: ActivityResultLauncher<Intent>? = null
    private var launcherFour: ActivityResultLauncher<Intent>? = null
    private var launcherFive: ActivityResultLauncher<Intent>? = null
    private var blockOne: ((granted: Boolean, isShowRationale: Boolean) -> Unit)? = null
    private var blockTow: ((granted: Boolean) -> Unit)? = null
    private var blockThree: ((granted: Boolean, intent: Intent?) -> Unit)? = null

    // 这方法请放在BaseActivity onCreate或当前Activity onCreate里面加载,不然无法使用
    // 因为部分Android 系统不在onResume前register会崩溃
    fun init(activity: ComponentActivity) {
        activity.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                launcherOne = null
                launcherTow = null
                launcherThree = null
                launcherFour = null
                launcherFive = null
                blockOne = null
                blockTow = null
                blockThree = null
            }
        })
        launcherOne = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    blockOne?.invoke(true, true)
                } else {
                    blockOne?.invoke(false, true)
                }
            }
        }
        launcherTow = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
            var count = 0
            granted.entries.forEach {
                if (it.value) {
                    count++
                } else {
                    // 未同意授权
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!activity.shouldShowRequestPermissionRationale(it.key)) {
                            // 用户拒绝权限并且系统不再弹出请求权限的弹窗
                            // 这时需要我们自己处理，比如自定义弹窗告知用户为何必须要申请这个权限
                            // 返回值：isShowRationale为false时拒绝并不再询问弹窗，正常拒绝为true
                            blockOne?.invoke(false, false)
                            return@registerForActivityResult
                        }
                    }
                }
            }
            if (count == granted.entries.size) {
                blockOne?.invoke(true, true)
            } else {
                blockOne?.invoke(false, true)
            }
        }

        launcherThree = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (canShowNotification(activity)) {
                blockTow?.invoke(true)
            } else {
                blockTow?.invoke(false)
            }
        }

        launcherFour = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                blockTow?.invoke(true)
            } else {
                blockTow?.invoke(false)
            }
        }

        launcherFive = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (it.resultCode == Activity.RESULT_OK) {
                    blockThree?.invoke(true, it.data)
                } else {
                    blockThree?.invoke(false, it.data)
                }
            }
        }
    }

    /**
     * 判断是否有读写权限，如果没有则申请
     * 若Android 11则申请所有访问权限
     * 返回值:granted 为true同意权限,否则反之
     * 返回值：isShowRationale为false时安卓10或以下拒绝并不再询问弹窗，正常拒绝为true
     */

    fun checkPermission(context: Context, block: (granted: Boolean, isShowRationale: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return block(true, true)
        }
        blockOne = block
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            // permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        }
        val needPermission = ArrayList<String>()
        for (s in permissions) {
            if (context.checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) {
                needPermission.add(s)
            }
        }
        val isA14 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        if (isA14) {
            if (context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED) {
                if (needPermission.contains(Manifest.permission.READ_MEDIA_IMAGES)) {
                    needPermission.remove(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
        }
        return if (needPermission.isEmpty()) {
            block(true, true)
        } else {
            launcherTow!!.launch(needPermission.toTypedArray())
        }
    }


    // 跳转到开启通知权限系统页面
    /**
     * granted:为是否开启权限
     */
    fun startNotificationPermission(context: Context, block: (granted: Boolean) -> Unit) {
        val notificationSettings = "android.settings.APP_NOTIFICATION_SETTINGS"
        val appPACKAGE = "app_package"
        val appUID = "app_uid"
        blockTow = block
        try {
            val localIntent = Intent()
            // 这种方案适用于 API 26, 即8.0（含8.0）以上可以用
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                localIntent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                localIntent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // 这种方案适用于 API21——25，即 5.0——7.1 之间的版本可以使用
                localIntent.action = notificationSettings
                localIntent.putExtra(appPACKAGE, context.packageName)
                localIntent.putExtra(appUID, context.applicationInfo.uid)
            }
            if (!canShowNotification(context)) {
                launcherThree?.launch(localIntent)
            }
        } catch (e: Exception) {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            if (!canShowNotification(context)) {
                launcherThree?.launch(intent)
            }
        }

    }


    /**
     * Android 11获取Android/media目录权限
     * granted返回值:申请结果
     * intent:授权的Uri
     *
     */
    fun getMediaPermissionAndroidR(block: (granted: Boolean, intent: Intent?) -> Unit) {
        blockThree = block
        val dataPath = "content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fmedia"
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION // 永久权限
                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        val uri = Uri.parse(dataPath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }
        launcherFive?.launch(intent)
    }

    /**
     * Android 11获取Android/data目录权限
     * granted返回值:申请结果
     * intent:授权的Uri
     *
     */
    fun getDataPermissionAndroidR(block: (granted: Boolean, intent: Intent?) -> Unit) {
        blockThree = block
        // 注意Android 13 不能授权整个Android/data文件夹，只能单独授权里面的文件夹
        val dataPath = "content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata"
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.flags = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION // 永久权限
                or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
        val uri = Uri.parse(dataPath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }
        launcherFive?.launch(intent)
    }


    // 获取位置权限
    fun requestLocationPermission(context: Context, block: (granted: Boolean, isShowRationale: Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            block(true, true)
            return
        }
        blockOne = block
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            block(true, true)
            return
        }
        val permissionList = arrayOf(permission)
        launcherTow?.launch(permissionList)
    }

    // 获取录音权限
    fun requestAudioPermission(activity: ComponentActivity, block: (granted: Boolean, isShowRationale: Boolean) -> Unit) {
        blockOne = block
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            block(true, true)
            return
        }
        val permission = Manifest.permission.RECORD_AUDIO
        if (activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            block(true, true)
            return
        }
        val permissionList = arrayOf(permission)
        launcherTow?.launch(permissionList)
    }

    // 获取日历读取权限
    fun requestCalenderPermission(activity: Context, block: (granted: Boolean, isShowRationale: Boolean) -> Unit) {
        blockOne = block
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            block(true, true)
            return
        }
        val permissionList = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        val needPermission: MutableList<String> = ArrayList()
        for (s in permissionList) {
            if (activity.checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) {
                needPermission.add(s)
            }
        }
        return if (needPermission.isEmpty()) {
            block(true, true)
        } else {
            launcherTow!!.launch(needPermission.toTypedArray())
        }
    }

    // 如果这个App不是默认的Sms App，则修改成默认的SMS APP
    //  因为从Android 4.4开始，只有默认的SMS APP才能对SMS数据库进行处理(部分手机查找短信也需要，而插入是必须要)
    fun setDefaultSms(context: Context, block: (granted: Boolean) -> Unit) {
        blockTow = block
        if (Build.VERSION.SDK_INT >= 29) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            // 当前应用已经是默认SMS应用
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                block(true)
                return
            }
            // 当前应用已经是默认应用
            if (!roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                val intent0 = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                launcherFour?.launch(intent0)
                return
            } else {
                block(true)
                return
            }
        } else {
            val s = Telephony.Sms.getDefaultSmsPackage(context)
            if (s != context.packageName) {
                val intent1 = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                intent1.putExtra("package", context.packageName)
                launcherFour?.launch(intent1)
            }
        }
    }

    // 获取自定义权限
    // 比如单独放入Manifest.permission.READ_CALENDAR 或 多个权限进一个Array<String>
    fun requestCustomPermission(activity: Context, permissionList: Array<String>, block: (granted: Boolean, isShowRationale: Boolean) -> Unit) {
        blockOne = block
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            block(true, true)
            return
        }
        val needPermission: MutableList<String> = ArrayList()
        for (s in permissionList) {
            if (activity.checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) {
                needPermission.add(s)
            }
        }
        return if (needPermission.isEmpty()) {
            block(true, true)
        } else {
            launcherTow!!.launch(needPermission.toTypedArray())
        }
    }


    companion object {
        // Android 11 保存Uri权限
        fun saveSafUriPermissionForAndroidR(activity: Activity?, uriTree: Uri?, key: String) {
            if (uriTree == null) {
                return
            }
            activity?.contentResolver?.takePersistableUriPermission(uriTree, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val sp = activity?.getSharedPreferences("{$key}DirPermission", Context.MODE_PRIVATE)
            val editor = sp?.edit()
            editor?.putString(key, uriTree.toString())
            editor?.apply()
        }


        // 获取Android 11保存的Uri
        fun getSafUriPermissionForAndroidR(context: Context, key: String): String? {
            val sp = context.getSharedPreferences("{$key}DirPermission", Context.MODE_PRIVATE)
            return sp.getString(key, "")
        }

        // 判断是否有读取sd卡存储权限（包括安卓11所有文件访问权限）
        fun hasStoragePermission(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return Environment.isExternalStorageManager()
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) && hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            return true
        }

        // 判断是否有通知权限
        fun canShowNotification(context: Context?): Boolean {
            return NotificationManagerCompat.from(context!!).areNotificationsEnabled()
        }

        // 判断是否拥有某权限
        private fun hasPermission(context: Context, permission: String?): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return true
            }
            if (context.checkSelfPermission(permission!!) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            return true
        }

        // 判断是否拥有某组权限
        fun hasPermissions(context: Context, list: Array<String>): Boolean {
            for (str in list) {
                if (!hasPermission(context, str)) {
                    return false
                }
            }
            return true
        }

        // 检测是不是默认sms应用
        fun checkSms(context: Context): Boolean {
            if (Build.VERSION.SDK_INT >= 29) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    return true
                }
                val roleManager = context.getSystemService(RoleManager::class.java)
                // 当前应用已经是默认SMS应用
                if (!roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    return true
                }
                // 当前应用已经是默认应用
                return roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            } else {
                val s = Telephony.Sms.getDefaultSmsPackage(context)
                if (s != context.packageName) {
                    return false
                }
                return true
            }
        }

        val STORAGE_PERMISSION_LIST: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val STORAGE_IMAGE_PERMISSION_LIST: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val STORAGE_VIDEO_PERMISSION_LIST: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        /**
         * 判断用户是否只给了部分权限,返回三种状态
         *  0  : 授予完全权限
         *  1  : 授予部分媒体权限
         *  2  : 拒绝权限
         * -1  : 不是A14哦
         * @param type 0 图片 1 视频
         */
        fun isA14SelectSomeMedia(type: Int? = 0): Int {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                return -1
            }
            val hasImages = PermissionUtils.isGranted(Manifest.permission.READ_MEDIA_IMAGES)
            val hasVideos = PermissionUtils.isGranted(Manifest.permission.READ_MEDIA_VIDEO)
            val hasSpecialFlag = PermissionUtils.isGranted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            val result = when (type) {
                0 -> {
                    !hasImages
                }

                1 -> {
                    !hasVideos
                }

                else -> {
                    !hasImages
                }
            }
            return if (result) {
                if (hasSpecialFlag) {
                    1
                } else {
                    2
                }
            } else {
                0
            }

        }

    }

}