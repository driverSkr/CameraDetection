package com.ethan.cameradetection.ui.camera.page

import android.Manifest
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blankj.utilcode.util.PermissionUtils
import com.ethan.cameradetection.R
import com.ethan.cameradetection.theme.Black
import com.ethan.cameradetection.theme.Transparent
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.theme.White10
import com.ethan.cameradetection.theme.White60
import com.ethan.cameradetection.ui.camera.view.CameraPreview
import com.ethan.cameradetection.utils.findBaseActivityVBind

@Composable
fun CameraScannerPage() {
    val context = LocalContext.current
    val colors = listOf(
        Color(0xFFDD1313),
        Color(0xFF00C424),
        Color(0xFF1C73FF)
    )
    var currentFilterColorIndex by remember { mutableStateOf(0) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    val isShowDialogTip = remember { mutableStateOf(true) }

    // 请求摄像头权限
    LaunchedEffect(Unit) {
        PermissionUtils.permission(Manifest.permission.CAMERA)
            .callback { isAllGranted, granted, deniedForever, denied ->
                if (!isAllGranted) {
                    Toast.makeText(
                        context,
                        "The camera function cannot be used because the camera permission is not obtained.",
                        Toast.LENGTH_SHORT
                    ).show()
                    context.findBaseActivityVBind()?.finish()
                } else {
                    cameraPermissionGranted = true
                }
            }
            .request()
    }

    Column(modifier = Modifier.fillMaxSize().background(color = Black).statusBarsPadding().navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp)) {
            Image(painter = painterResource(R.drawable.svg_icon_back), contentDescription = null, modifier = Modifier.align(Alignment.CenterStart).clickable{
                context.findBaseActivityVBind()?.finish()
            })
            Text("Scanner", color = White, fontSize = 18.sp, fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center))
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            if (cameraPermissionGranted) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    lensFacing = CameraSelector.LENS_FACING_BACK
                )
            }

            // 滤镜层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors[currentFilterColorIndex].copy(alpha = 0.3f))
            )

            // 顶部遮罩
            Box(modifier = Modifier.fillMaxWidth().height(140.dp).background(brush = Brush.verticalGradient(colorStops = arrayOf(0f to Black, 1f to Transparent))))
            // 底部遮罩
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(140.dp).background(brush = Brush.verticalGradient(colorStops = arrayOf(0f to Transparent, 1f to Black))))

            if (isShowDialogTip.value) {
                Row(modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(color = White10, shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(painter = painterResource(R.drawable.svg_icon_warning_gray), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Please aim the lens at the detection position, and then press this button to start detection",
                        color = White60,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W400, modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Image(painter = painterResource(R.drawable.svg_icon_close), contentDescription = null, modifier = Modifier.clickable{ isShowDialogTip.value = false })
                }
            }

            Image(painter = painterResource(R.drawable.svg_icon_retry), contentDescription = null, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 8.dp).clickable{
                currentFilterColorIndex = 0
            })

            Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 50.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                colors.forEachIndexed { index, color ->
                    Box(modifier = Modifier
                        .size(58.dp)
                        .border(width = 2.dp, color = if (currentFilterColorIndex == index) White else Transparent, shape = RoundedCornerShape(999.dp))
                        .padding(5.dp)
                        .background(color = color, shape = RoundedCornerShape(999.dp))
                        .clickable{ currentFilterColorIndex = index }
                    )
                }
            }
        }
    }
}