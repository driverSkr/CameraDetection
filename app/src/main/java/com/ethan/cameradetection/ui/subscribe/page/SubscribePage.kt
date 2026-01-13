package com.ethan.cameradetection.ui.subscribe.page

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.theme.Black
import com.ethan.cameradetection.theme.Transparent
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.theme.White10
import com.ethan.cameradetection.ui.subscribe.view.SubscribeItemView
import com.ethan.cameradetection.utils.findBaseActivityVBind

@Composable
fun SubscribePage() {
    val context = LocalContext.current
    val productList = listOf(
        Triple("≈\$3.99/wk", "\$19.99", "Monthly"),
        Triple("≈\$2.30/wk", "\$6.99", "Weekly"),
        Triple("≈\$0.58/wk", "\$29.99", "Yearly"),
    )

    Box(modifier = Modifier.fillMaxSize().background(color = Black).navigationBarsPadding()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(painter = painterResource(R.mipmap.img_subscribe_demo_1), modifier = Modifier.fillMaxWidth(), contentDescription = null)
            Box(modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(200.dp)
                .background(brush = Brush.verticalGradient(colorStops = arrayOf(0f to Transparent, 1f to Black)))
            )
            Image(
                painter = painterResource(R.mipmap.img_position),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 74.dp, bottom = 44.dp)
                    .size(32.dp),
                contentDescription = null
            )
            Image(
                painter = painterResource(R.mipmap.img_position),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 23.dp, bottom = 199.dp)
                    .size(32.dp),
                contentDescription = null
            )
            Image(
                painter = painterResource(R.mipmap.img_position),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 41.dp, bottom = 144.dp)
                    .size(32.dp),
                contentDescription = null
            )
        }

        Image(
            painter = painterResource(R.drawable.svg_icon_close_30),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 15.dp, end = 16.dp)
                .clickable{ context.findBaseActivityVBind()?.finish() },
            contentDescription = null
        )

        Column(modifier = Modifier.fillMaxWidth().padding(top = 110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Advanced",
                style = TextStyle(
                    brush = Brush.horizontalGradient(colorStops = arrayOf(0f to Color(0xFF01C587), 1f to Color(0xFFBCF085))),
                    fontWeight = FontWeight.W700,
                    fontSize = 32.sp
                ),
                softWrap = false,
                maxLines = 1
            )
            Text(
                text = "Hidden Camera Finder",
                style = TextStyle(
                    brush = Brush.horizontalGradient(colorStops = arrayOf(0f to Color(0xFF01C587), 1f to Color(0xFFBCF085))),
                    fontWeight = FontWeight.W700,
                    fontSize = 32.sp
                ),
                softWrap = false,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(24.dp))
            Column {
                Row(modifier = Modifier) {
                    Image(painter = painterResource(R.drawable.svg_icon_correct), modifier = Modifier.padding(end = 4.dp), contentDescription = null)
                    Text("Unlock All Pro Features", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier) {
                    Image(painter = painterResource(R.drawable.svg_icon_correct), modifier = Modifier.padding(end = 4.dp), contentDescription = null)
                    Text("View Devices‘ Information", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier) {
                    Image(painter = painterResource(R.drawable.svg_icon_correct), modifier = Modifier.padding(end = 4.dp), contentDescription = null)
                    Text("No Ads Experience", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                productList.forEachIndexed { index, triple ->
                    SubscribeItemView(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text("Auto-Renewable. Cancel anytime.", color = Color(0xFF96939E), fontSize = 14.sp, fontWeight = FontWeight.W400)
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(color = Color(0xFF00C46F), shape = RoundedCornerShape(999.dp))
                .border(width = 1.dp, shape = RoundedCornerShape(999.dp), brush = Brush.verticalGradient(colorStops = arrayOf(0f to White10, 0.5f to Transparent, 1f to White10)))
            ) {
                Text("Continue", color = White, fontSize = 16.sp, fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Text("Terms of Use", color = Color(0xFF00C46F), fontSize = 12.sp, fontWeight = FontWeight.W400)
                Text(" and ", color = White, fontSize = 12.sp, fontWeight = FontWeight.W400)
                Text("Terms of Use", color = Color(0xFF00C46F), fontSize = 12.sp, fontWeight = FontWeight.W400)
            }
        }
    }
}