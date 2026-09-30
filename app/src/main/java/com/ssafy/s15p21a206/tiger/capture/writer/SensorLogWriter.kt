package com.ssafy.s15p21a206.tiger.capture.writer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.ssafy.s15p21a206.tiger.episode.SessionBundle
import java.io.File

/**
 * 가속도계·자이로·회전 벡터 표본을 세션 bundle의 CSV에 적는다.
 *
 * 여는 일이 [open]과 [listen] 둘로 나뉜 것은 시점이 다르기 때문이다. 헤더는 수집을 시작하기 전에
 * 적어 두어야 도중에 실패해도 파일 모양이 온전하고, 수신은 카메라가 돌기 시작한 뒤에 연다.
 *
 * 닫는 일도 [stopListening]과 [close] 둘이다. 수신을 끊은 뒤에도 이미 전달된 표본이 남아 있어,
 * 기록 대상을 비우는 것은 나머지 정리가 끝난 뒤다.
 */
class SensorLogWriter(
    private val sensorManager: SensorManager,
) : SensorEventListener {
    private val files = mutableMapOf<Int, File>()

    /** 세 센서의 CSV 헤더를 적고 기록 대상을 잡는다. */
    fun open(bundle: SessionBundle) {
        bundle.accelerometer.writeText("${SessionBundle.ACCELEROMETER_HEADER}\n")
        bundle.gyroscope.writeText("${SessionBundle.GYROSCOPE_HEADER}\n")
        bundle.rotationVector.writeText("${SessionBundle.ROTATION_VECTOR_HEADER}\n")
        files[Sensor.TYPE_ACCELEROMETER] = bundle.accelerometer
        files[Sensor.TYPE_GYROSCOPE] = bundle.gyroscope
        files[Sensor.TYPE_ROTATION_VECTOR] = bundle.rotationVector
    }

    /** 표본 수신을 연다. */
    fun listen() {
        register(Sensor.TYPE_ACCELEROMETER)
        register(Sensor.TYPE_GYROSCOPE)
        register(Sensor.TYPE_ROTATION_VECTOR)
    }

    private fun register(type: Int) {
        sensorManager.getDefaultSensor(type)?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val target = files[event.sensor.type] ?: return
        val valueCount = if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) ROTATION_VECTOR_VALUES else MOTION_VALUES
        target.appendText("${sensorRow(event.timestamp, event.values, valueCount, event.accuracy)}\n")
    }

    override fun onAccuracyChanged(
        sensor: Sensor?,
        accuracy: Int,
    ) = Unit

    /** 표본 수신을 끊는다. */
    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    /** 기록 대상을 비운다. 이 뒤에 닿은 표본은 버려진다. */
    fun close() {
        files.clear()
    }
}

/**
 * `timestamp_ns`, 값 [valueCount]개, `accuracy` 순의 CSV 행.
 *
 * 기기가 값을 덜 주면 0으로 채워 열 수를 헤더에 맞춘다. 회전 벡터의 heading accuracy는 기기에 따라 오지 않는다.
 */
internal fun sensorRow(
    timestampNs: Long,
    values: FloatArray,
    valueCount: Int,
    accuracy: Int,
): String =
    buildList {
        add(timestampNs)
        repeat(valueCount) { add(values.getOrElse(it) { 0f }) }
        add(accuracy)
    }.joinToString(",")

/** [SessionBundle.ROTATION_VECTOR_HEADER]의 x, y, z, scalar_component, heading_accuracy_rad. */
private const val ROTATION_VECTOR_VALUES = 5

/** [SessionBundle.ACCELEROMETER_HEADER]·[SessionBundle.GYROSCOPE_HEADER]의 x, y, z. */
private const val MOTION_VALUES = 3
