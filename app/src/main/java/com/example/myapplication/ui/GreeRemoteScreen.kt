package com.example.myapplication.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.GreeRemoteViewModel
import com.example.myapplication.describe
import com.example.myapplication.fanLabel
import com.example.myapplication.ir.GreeCodec
import com.example.myapplication.ir.VerticalSwing
import com.example.myapplication.modeLabel
import com.example.myapplication.swingSummary

private val PageBg = Color(0xFFF2F2F2)
private val CardWhite = Color.White
private val TextMain = Color(0xFF1C1C1C)
private val TextMuted = Color(0xFF9A9A9A)
private val AccentBlue = Color(0xFF2B7CFF)
private val PowerOff = Color(0xFFE53935)
private val ChipIdle = Color(0xFFE8E8E8)

@Composable
fun GreeRemoteScreen(
    vm: GreeRemoteViewModel,
    onBack: () -> Unit = {},
) {
    val s = vm.state
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    fun tap(action: () -> Unit) {
        RemoteFeedback.play(context, vm.clickSound)
        action()
    }
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PageBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { tap(onBack) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = TextMain)
                }
                Text("格力空调", fontSize = 20.sp, fontWeight = FontWeight.Medium, color = TextMain)
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "更多", tint = TextMain)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("按键音") },
                            onClick = {
                                showMenu = false
                                showSoundPicker = true
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = s.tempC.toString(),
                fontSize = 88.sp,
                fontWeight = FontWeight.Light,
                color = TextMain,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                lineHeight = 88.sp,
            )
            Text(
                "目标温度 (℃)",
                color = TextMuted,
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFE9E9E9))
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatCell(modeLabel(s.mode), "模式", onClick = {
                    tap { vm.setMode((s.mode + 1) % 5) }
                })
                StatCell(fanLabel(s.fan), "风速", onClick = { tap(vm::cycleFan) })
                StatCell(swingSummary(s.verticalSwing), "风向", onClick = { tap(vm::openSwingPicker) })
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PillButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.PowerSettingsNew,
                    label = "开关",
                    iconTint = if (s.power) AccentBlue else PowerOff,
                    onClick = { tap(vm::togglePower) },
                )
                PillButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.SwapHoriz,
                    label = "扫风",
                    iconTint = if (s.verticalSwing != VerticalSwing.Off) AccentBlue else TextMain,
                    onClick = { tap(vm::openSwingPicker) },
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardWhite)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "－",
                    fontSize = 28.sp,
                    color = TextMain,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { tap { vm.changeTemp(-1) } }
                        .padding(16.dp),
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("温度", fontSize = 18.sp, color = TextMain)
                }
                Text(
                    "＋",
                    fontSize = 28.sp,
                    color = TextMain,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { tap { vm.changeTemp(1) } }
                        .padding(16.dp),
                )
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(CardWhite)
                    .padding(vertical = 16.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ModeChip("A", null, "自动", s.mode == GreeCodec.MODE_AUTO) { tap { vm.setMode(GreeCodec.MODE_AUTO) } }
                ModeChip(null, Icons.Outlined.AcUnit, "制冷", s.mode == GreeCodec.MODE_COOL) { tap { vm.setMode(GreeCodec.MODE_COOL) } }
                ModeChip(null, Icons.Outlined.WbSunny, "制热", s.mode == GreeCodec.MODE_HEAT) { tap { vm.setMode(GreeCodec.MODE_HEAT) } }
                ModeChip(null, Icons.Outlined.WaterDrop, "除湿", s.mode == GreeCodec.MODE_DRY) { tap { vm.setMode(GreeCodec.MODE_DRY) } }
                ModeChip(null, Icons.Outlined.Air, "送风", s.mode == GreeCodec.MODE_FAN) { tap { vm.setMode(GreeCodec.MODE_FAN) } }
            }

            Spacer(Modifier.height(16.dp))
            Text(describe(s), color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(24.dp))
        }

        if (showSoundPicker) {
            Dialog(onDismissRequest = { showSoundPicker = false }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardWhite)
                        .padding(vertical = 12.dp),
                ) {
                    ClickSound.entries.forEach { sound ->
                        val selected = sound == vm.clickSound
                        Text(
                            text = sound.label,
                            color = if (selected) AccentBlue else TextMain,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    vm.selectClickSound(sound)
                                    RemoteFeedback.play(context, sound)
                                    showSoundPicker = false
                                }
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                }
            }
        }

        if (vm.showSwingPicker) {
            Dialog(onDismissRequest = vm::dismissSwingPicker) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(CardWhite)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DirectionChip("上", s.verticalSwing == VerticalSwing.FixUp, Modifier.weight(1f)) {
                        tap { vm.lockVane(0) }
                    }
                    DirectionChip("中", s.verticalSwing == VerticalSwing.FixMid, Modifier.weight(1f)) {
                        tap { vm.lockVane(1) }
                    }
                    DirectionChip("下", s.verticalSwing == VerticalSwing.FixDown, Modifier.weight(1f)) {
                        tap { vm.lockVane(2) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCell(value: String, caption: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
        Spacer(Modifier.height(4.dp))
        Text(caption, fontSize = 13.sp, color = TextMuted)
    }
}

@Composable
private fun PillButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(CardWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = iconTint)
        Text(label, fontSize = 18.sp, color = TextMain, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun ModeChip(
    letter: String?,
    icon: ImageVector?,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (selected) AccentBlue else ChipIdle)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (letter != null) {
                Text(letter, color = if (selected) Color.White else TextMain, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            } else if (icon != null) {
                Icon(icon, contentDescription = caption, tint = if (selected) Color.White else TextMain)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(caption, fontSize = 12.sp, color = TextMuted)
    }
}

@Composable
private fun DirectionChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AccentBlue else ChipIdle)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Color.White else TextMain, fontSize = 14.sp)
    }
}
