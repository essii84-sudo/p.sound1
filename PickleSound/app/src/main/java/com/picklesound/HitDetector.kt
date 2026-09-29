package com.picklesound

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 손목 가속도 센서로 "공이 패들에 맞는 순간"을 감지한다.
 *
 * 원리: 스윙 자체는 가속도가 수십 ms에 걸쳐 부드럽게 변하지만,
 * 공이 패들에 맞으면 충격이 손목까지 전달되면서 가속도가 몇 ms 만에 급변한다.
 * 그래서 가속도의 크기가 아니라 "변화 속도(저크, m/s³)"를 보고 임계값을 넘으면 타구로 판단한다.
 */
class HitDetector(
    context: Context,
    private val onHit: (strength: Float) -> Unit,
    private val onLevel: (peak: Float) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val thread = HandlerThread("hit-detector").apply { start() }
    private val handler = Handler(thread.looper)

    val isAvailable: Boolean get() = accelerometer != null

    /** 저크 임계값 (m/s³). 낮을수록 민감. */
    @Volatile var threshold: Float = thresholdFor(6)

    /** 한 번 감지한 뒤 다시 감지하지 않는 시간 (연속 울림 방지). */
    @Volatile var cooldownMs: Long = 300L

    private var hasPrev = false
    private var px = 0f
    private var py = 0f
    private var pz = 0f
    private var lastTs = 0L
    private var lastHitTs = 0L
    private var peak = 0f
    private var lastLevelTs = 0L

    fun start() {
        val sensor = accelerometer ?: return
        hasPrev = false
        lastHitTs = 0L
        peak = 0f
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, handler)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun release() {
        stop()
        thread.quitSafely()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val ts = event.timestamp

        if (!hasPrev) {
            px = x; py = y; pz = z; lastTs = ts; lastLevelTs = ts
            hasPrev = true
            return
        }

        val dt = ((ts - lastTs) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
        val dx = x - px
        val dy = y - py
        val dz = z - pz
        val jerk = sqrt(dx * dx + dy * dy + dz * dz) / dt

        px = x; py = y; pz = z; lastTs = ts

        if (jerk > peak) peak = jerk
        if (ts - lastLevelTs > 250_000_000L) {       // 0.25초마다 최근 최대값을 화면에 알림
            onLevel(peak)
            peak = 0f
            lastLevelTs = ts
        }

        if (jerk >= threshold && ts - lastHitTs > cooldownMs * 1_000_000L) {
            lastHitTs = ts
            onHit(jerk)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        const val MIN_SENSITIVITY = 1
        const val MAX_SENSITIVITY = 10

        /** 감도 1(둔감)~10(민감)을 임계값 5000~400 m/s³ 로 지수적으로 변환 */
        fun thresholdFor(sensitivity: Int): Float {
            val s = sensitivity.coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
            val t = (s - 1) / 9f
            return 5000f * (400f / 5000f).pow(t)
        }
    }
}
