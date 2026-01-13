package com.ethan.cameradetection.ui.tips.page

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ethan.cameradetection.R
import com.ethan.cameradetection.theme.Black
import com.ethan.cameradetection.theme.White
import com.ethan.cameradetection.theme.White10
import com.ethan.cameradetection.theme.White60
import com.ethan.cameradetection.utils.findBaseActivityVBind

@Composable
fun TipsPage() {
    val context = LocalContext.current
    val showText1 = remember { mutableStateOf(false) }
    val showText2 = remember { mutableStateOf(false) }
    val showText3 = remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(color = Black).statusBarsPadding().navigationBarsPadding().padding(bottom = 24.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 12.dp)) {
            Image(
                painter = painterResource(R.drawable.svg_icon_back),
                modifier = Modifier.align(Alignment.CenterStart).clickable { context.findBaseActivityVBind()?.finish() },
                contentDescription = null
            )
            Text("Tips", color = White, fontSize = 18.sp, fontWeight = FontWeight.W500, modifier = Modifier.align(Alignment.Center))
        }

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .background(color = White10, shape = RoundedCornerShape(20.dp))
                    .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                        Text("Anti-sneak shooting while living", color = White, fontSize = 14.sp, fontWeight = FontWeight.W600)
                        Spacer(Modifier.weight(1f))
                        Image(painter = painterResource(if (showText1.value) R.drawable.svg_icon_down else R.drawable.svg_icon_next), modifier = Modifier.clickable { showText1.value = !showText1.value }, contentDescription = null)
                    }
                    if (showText1.value) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("1. ", color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                            Text(
                                text = "Pay attention to check the power socket holes, bath mirrors (especially used in combination with one-way glass), smoke sensors, lamp holders, ceilings, keyholes, etc. when using rental houses, and pay special attention to whether there is suspicious surroundings in the room. These may be the power and signal cables of the pinhole camera.",
                                color = White60,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("2. ", color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                            Text(
                                text = "To use the furniture attached to the rental house, pleasecheck the air-conditioning hole, telephone (may be monitored) and the top of the closet.",
                                color = White60,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("3. ", color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                            Text(
                                text = "When staying in hotels, guesthouses, guest houses,because the hotel is fully equipped, there are many places where 'pinhole cameras' can be installed, including airconditioners, lampshades, ceilings, vanity mirrors, smoke sensors, vases, sockets, and TVs. Racks, etc., pay particular attention to the equipment and furnishings facing the bed.",
                                color = White60,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .animateContentSize()
                    .background(color = White10, shape = RoundedCornerShape(20.dp))
                    .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                        Text("Anti-sneak shooting in public places", color = White, fontSize = 14.sp, fontWeight = FontWeight.W600)
                        Spacer(Modifier.weight(1f))
                        Image(painter = painterResource(if (showText2.value) R.drawable.svg_icon_down else R.drawable.svg_icon_next), modifier = Modifier.clickable { showText2.value = !showText2.value }, contentDescription = null)
                    }
                    if (showText2.value) {
                        // todo 文案记得修改
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("1. ", color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                            Text(
                                text = "Pay attention to check the power socket holes, bath mirrors (especially used in combination with one-way glass), smoke sensors, lamp holders, ceilings, keyholes, etc. when using rental houses, and pay special attention to whether there is suspicious surroundings in the room. These may be the power and signal cables of the pinhole camera.",
                                color = White60,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("2. ", color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                            Text(
                                text = "To use the furniture attached to the rental house, pleasecheck the air-conditioning hole, telephone (may be monitored) and the top of the closet.",
                                color = White60,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("3. ", color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                            Text(
                                text = "When staying in hotels, guesthouses, guest houses,because the hotel is fully equipped, there are many places where 'pinhole cameras' can be installed, including airconditioners, lampshades, ceilings, vanity mirrors, smoke sensors, vases, sockets, and TVs. Racks, etc., pay particular attention to the equipment and furnishings facing the bed.",
                                color = White60,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
                    .animateContentSize()
                    .background(color = White10, shape = RoundedCornerShape(20.dp))
                    .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                        Text("Strategy Self-prevention", color = White, fontSize = 14.sp, fontWeight = FontWeight.W600)
                        Spacer(Modifier.weight(1f))
                        Image(painter = painterResource(if (showText3.value) R.drawable.svg_icon_down else R.drawable.svg_icon_next), modifier = Modifier.clickable { showText3.value = !showText3.value }, contentDescription = null)
                    }
                    // todo 文案记得修改
                    if (showText3.value) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("1. ", color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                            Text(
                                text = "Pay attention to check the power socket holes, bath mirrors (especially used in combination with one-way glass), smoke sensors, lamp holders, ceilings, keyholes, etc. when using rental houses, and pay special attention to whether there is suspicious surroundings in the room. These may be the power and signal cables of the pinhole camera.",
                                color = White60,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("2. ", color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                            Text(
                                text = "To use the furniture attached to the rental house, pleasecheck the air-conditioning hole, telephone (may be monitored) and the top of the closet.",
                                color = White60,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("3. ", color = White60, fontSize = 12.sp, fontWeight = FontWeight.W400)
                            Text(
                                text = "When staying in hotels, guesthouses, guest houses,because the hotel is fully equipped, there are many places where 'pinhole cameras' can be installed, including airconditioners, lampshades, ceilings, vanity mirrors, smoke sensors, vases, sockets, and TVs. Racks, etc., pay particular attention to the equipment and furnishings facing the bed.",
                                color = White60,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.W400
                            )
                        }
                    }
                }
            }
        }

    }
}