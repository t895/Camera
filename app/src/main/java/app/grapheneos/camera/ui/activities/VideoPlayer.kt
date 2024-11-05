package app.grapheneos.camera.ui.activities

import android.annotation.SuppressLint
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.MediaController
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import app.grapheneos.camera.databinding.VideoPlayerBinding
import app.grapheneos.camera.util.getParcelableExtra
import kotlin.concurrent.thread


class VideoPlayer : AppCompatActivity() {

    companion object {
        const val TAG = "VideoPlayer"
        const val IN_SECURE_MODE = "isInSecureMode"
        const val VIDEO_URI = "videoUri"
    }

    private lateinit var binding: VideoPlayerBinding

    @SuppressLint("WrongConstant")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = false
        windowInsetsController.isAppearanceLightNavigationBars = false
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())

        // This is a deprecated constant but Google uses it themselves in Photos and results in
        // much nicer UX. Suppress the warning until we get a decent API.
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH

        val intent = this.intent
        if (intent.getBooleanExtra(IN_SECURE_MODE, false)) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        binding = VideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.let {
            it.setDisplayShowTitleEnabled(false)
            it.setDisplayHomeAsUpEnabled(true)
        }

        val uri = getParcelableExtra<Uri>(intent, VIDEO_URI)!!

        val videoView = binding.videoPlayer

        val mediaController = object : MediaController(this) {
            override fun show() {
                super.show()
                supportActionBar?.show()
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }

            override fun hide() {
                super.hide()
                supportActionBar?.hide()
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        supportActionBar?.setBackgroundDrawable(null)

        thread {
            var hasAudio = true
            try {
                MediaMetadataRetriever().use {
                    it.setDataSource(this, uri)
                    hasAudio = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
                }
            } catch (e: Exception) {
                Log.d(TAG, "", e)
            }

            mainExecutor.execute {
                val lifecycleState = lifecycle.currentState

                if (lifecycleState == Lifecycle.State.DESTROYED) {
                    return@execute
                }

                val audioFocus = if (hasAudio) AudioManager.AUDIOFOCUS_GAIN else AudioManager.AUDIOFOCUS_NONE
                videoView.setAudioFocusRequest(audioFocus)

                videoView.setOnPreparedListener { _ ->
                    videoView.setMediaController(mediaController)

                    if (lifecycleState == Lifecycle.State.RESUMED) {
                        videoView.start()
                    }

                    supportActionBar?.show()
                    mediaController.show(0)
                }

                videoView.setVideoURI(uri)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        supportActionBar?.show()
    }
}
