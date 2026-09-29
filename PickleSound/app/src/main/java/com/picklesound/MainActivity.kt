package com.picklesound

import android.content.Context
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

class MainActivity : ComponentActivity() {

    private lateinit var sound: SoundPlayer
    private lateinit var detector: HitDetector
    private lateinit var vibrator: Vibrator

    // 화면 상태
    private val running = mutableStateOf(false)
    private val hits = mutableIntStateOf(0)
    private val level = mutableFloatStateOf(0f)
    private val sensitivity = mutableIntStateOf(6)
    private val soundIndex = mutableIntStateOf(0)
    private val vibrate = mutableStateOf(true)

    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensitivity.intValue = prefs.getInt("sensitivity", 6)
        soundIndex.intValue = prefs.getInt("sound", 0)
        vibrate.value = prefs.getBoolean("vibrate", true)

        sound = SoundPlayer(this)
        vibrator = getSystemService(Vibrator::class.java)
        detector = HitDetector(
            context = this,
            onHit = { onHitDetected() },
            onLevel = { peak -> runOnUiThread { level.floatValue = peak } },
        )
        detector.threshold = HitDetector.thresholdFor(sensitivity.intValue)

        setContent { PickleApp() }
    }

    /** 센서 스레드에서 호출됨: 소리는 바로 재생하고 화면 갱신만 메인 스레드로 넘긴다. */
    private fun onHitDetected() {
        feedback()
        runOnUiThread { hits.intValue += 1 }
    }

    private fun feedback() {
        sound.play(soundIndex.intValue)
        if (vibrate.value) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun setRunning(on: Boolean) {
        if (on == running.value) return
        running.value = on
        if (on) {
            detector.start()
            // 경기 중 화면이 꺼지거나 워치페이스로 돌아가면 감지가 멈추므로 화면을 켜둔다
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            detector.stop()
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            level.floatValue = 0f
        }
    }

    override fun onStop() {
        super.onStop()
        setRunning(false)      // 앱을 벗어나면 감지 중지 (배터리 보호)
    }

    override fun onDestroy() {
        super.onDestroy()
        detector.release()
        sound.release()
    }

    private fun saveSettings() {
        prefs.edit()
            .putInt("sensitivity", sensitivity.intValue)
            .putInt("sound", soundIndex.intValue)
            .putBoolean("vibrate", vibrate.value)
            .apply()
    }

    @Composable
    private fun PickleApp() {
        val listState = rememberScalingLazyListState()
        val isRunning = running.value
        val threshold = HitDetector.thresholdFor(sensitivity.intValue)

        MaterialTheme {
            Scaffold(
                timeText = { TimeText() },
                positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
            ) {
                ScalingLazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    state = listState,
                ) {
                    item {
                        Text(
                            text = "🏓 ${hits.intValue}",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) Color(0xFFF5D90A) else Color.White,
                            textAlign = TextAlign.Center,
                        )
                    }
                    item {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (!detector.isAvailable) return@Chip
                                setRunning(!isRunning)
                            },
                            label = {
                                Text(
                                    when {
                                        !detector.isAvailable -> "센서 없음"
                                        isRunning -> "정지"
                                        else -> "시작"
                                    }
                                )
                            },
                            secondaryLabel = {
                                Text(if (isRunning) "타구 감지 중" else "손목에 차고 시작하세요")
                            },
                            colors = if (isRunning) ChipDefaults.secondaryChipColors()
                            else ChipDefaults.primaryChipColors(),
                        )
                    }
                    item {
                        // 실시간 충격 세기: 기준값을 넘으면 소리가 난다 → 감도 조절에 참고
                        Text(
                            text = "충격 ${level.floatValue.toInt()} / 기준 ${threshold.toInt()}",
                            fontSize = 12.sp,
                            color = if (level.floatValue >= threshold) Color(0xFFF5D90A) else Color.LightGray,
                            textAlign = TextAlign.Center,
                        )
                    }
                    item {
                        Text(
                            text = "감도 ${sensitivity.intValue}",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    item {
                        InlineSlider(
                            value = sensitivity.intValue,
                            onValueChange = {
                                sensitivity.intValue = it
                                detector.threshold = HitDetector.thresholdFor(it)
                                saveSettings()
                            },
                            valueProgression = HitDetector.MIN_SENSITIVITY..HitDetector.MAX_SENSITIVITY,
                            decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "감도 낮추기") },
                            increaseIcon = { Icon(InlineSliderDefaults.Increase, "감도 높이기") },
                            segmented = true,
                        )
                    }
                    item {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                soundIndex.intValue = (soundIndex.intValue + 1) % sound.sounds.size
                                sound.play(soundIndex.intValue)
                                saveSettings()
                            },
                            label = { Text("효과음: ${sound.sounds[soundIndex.intValue].name}") },
                            secondaryLabel = { Text("눌러서 바꾸기") },
                            colors = ChipDefaults.secondaryChipColors(),
                        )
                    }
                    item {
                        ToggleChip(
                            modifier = Modifier.fillMaxWidth(),
                            checked = vibrate.value,
                            onCheckedChange = {
                                vibrate.value = it
                                saveSettings()
                            },
                            label = { Text("진동도 함께") },
                            toggleControl = {
                                Icon(
                                    imageVector = ToggleChipDefaults.switchIcon(vibrate.value),
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                    item {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { feedback() },
                            label = { Text("소리 테스트") },
                            colors = ChipDefaults.secondaryChipColors(),
                        )
                    }
                    item {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { hits.intValue = 0 },
                            label = { Text("횟수 초기화") },
                            colors = ChipDefaults.secondaryChipColors(),
                        )
                    }
                }
            }
        }
    }
}
