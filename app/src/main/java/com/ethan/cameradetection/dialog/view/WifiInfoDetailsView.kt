package com.ethan.cameradetection.dialog.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.model.WifiInfoModel
import com.ethan.cameradetection.theme.Transparent
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.theme.White10
import com.ethan.cameradetection.theme.White60
import com.google.android.material.bottomsheet.BottomSheetDialog

@Composable
fun WifiInfoDetailsView(dialog: BottomSheetDialog, infoModel: WifiInfoModel) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(color = Color(0xFF161618), shape = RoundedCornerShape(48.dp))
        .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Box(modifier = Modifier
            .width(48.dp)
            .height(6.dp)
            .background(color = Color(0xFF5B5B5E), shape = RoundedCornerShape(4.dp))
            .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).background(color = White10, shape = RoundedCornerShape(19.dp))) {
                Image(painter = painterResource(R.drawable.svg_icon_wifi_info_router), modifier = Modifier.size(36.dp).align(Alignment.Center), contentDescription = null)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(infoModel.name, color = White, fontSize = 18.sp, fontWeight = FontWeight.W600)
            Spacer(modifier = Modifier.weight(1f))
            Image(painter = painterResource(R.drawable.svg_icon_close_30), contentDescription = null, modifier = Modifier.clickable{ dialog.dismiss() })
        }
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text("ID Address", color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterStart))
            Text(infoModel.ip, color = White, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterEnd))
        }
        Box(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text("MAC Address", color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterStart))
            Text(infoModel.mac, color = White, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterEnd))
        }
        Box(modifier = Modifier.fillMaxWidth().height(42.dp)) {
            Text("Device Model", color = White60, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterStart))
            Text("Unknown", color = White, fontSize = 14.sp, fontWeight = FontWeight.W400, modifier = Modifier.align(Alignment.CenterEnd))
        }
        Spacer(modifier = Modifier.height(20.dp))
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
            .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.horizontalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
        ) {
            Row(modifier = Modifier.align(Alignment.Center)) {
                Image(painter = painterResource(R.drawable.svg_icon_correct_white), contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Mark as safe", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
            }
        }
    }
}