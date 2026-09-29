package com.picklesound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** 짧은 효과음을 지연 없이 재생하기 위해 SoundPool에 미리 올려둔다. */
class SoundPlayer(context: Context) {

    data class Sound(val name: String, val resId: Int)

    val sounds = listOf(
        Sound("팡!", R.raw.pop),
        Sound("딱!", R.raw.knock),
        Sound("뿅!", R.raw.blip),
    )

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: List<Int> = sounds.map { pool.load(context, it.resId, 1) }

    fun play(index: Int, volume: Float = 1f) {
        val id = ids.getOrNull(index) ?: return
        val v = volume.coerceIn(0f, 1f)
        pool.play(id, v, v, 1, 0, 1f)
    }

    fun release() = pool.release()
}
