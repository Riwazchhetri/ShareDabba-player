package com.example.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.core.config.EnvironmentConfig
import com.example.network.NetworkClient
import kotlinx.coroutines.launch
import java.net.URI

private const val API_ROOT = "https://sharedabba-pwa-api.onrender.com"

@Composable
fun GuestPlaybackScreen(initialLink: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val config = remember { EnvironmentConfig() }
    val api = remember { NetworkClient.createApiService(config) }
    var link by remember { mutableStateOf(initialLink.orEmpty()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var playbackUrl by remember { mutableStateOf<String?>(null) }
    var videoTitle by remember { mutableStateOf("") }
    var creator by remember { mutableStateOf("") }

    LaunchedEffect(initialLink) {
        if (!initialLink.isNullOrBlank()) link = initialLink
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF101014))) {
        if (playbackUrl != null) {
            GuestVideoSurface(url = playbackUrl!!, title = videoTitle, onBack = { playbackUrl = null })
        } else {
            Column(
                modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("ShareDabba Player", color = Color.White, fontSize = 27.sp)
                Spacer(Modifier.height(8.dp))
                Text("Paste a ShareDabba video link to play", color = Color(0xFFB8B8C5), fontSize = 14.sp)
                Spacer(Modifier.height(26.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ShareDabba link") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        val slug = extractShareSlug(link)
                        if (slug == null) {
                            error = "This ShareDabba link is not valid."
                        } else {
                            loading = true
                            error = null
                            scope.launch {
                                try {
                                    val response = api.getGuestPlayback(slug)
                                    videoTitle = response.file.name
                                    creator = response.file.creator.orEmpty()
                                    playbackUrl = API_ROOT + response.playbackUrl
                                } catch (e: retrofit2.HttpException) {
                                    error = when (e.code()) {
                                        404, 410 -> "This video is currently unavailable."
                                        else -> "Could not load this video. Please try again."
                                    }
                                } catch (_: Exception) {
                                    error = "Could not connect. Check your internet and try again."
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    },
                    enabled = !loading && link.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) CircularProgressIndicator() else Text("Play Video")
                }
                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(error!!, color = Color(0xFFFF8A8A), fontSize = 14.sp)
                }
                if (creator.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Uploaded by $creator", color = Color(0xFFB8B8C5), fontSize = 12.sp)
                }
            }
        }
    }
}

private fun extractShareSlug(raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    val uri = try {
        if (!value.contains("://") && value.matches(Regex("[A-Za-z0-9_-]{3,128}"))) {
            return value
        }
        URI(value)
    } catch (_: Exception) { return null }
    val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
    val validHost = (uri.scheme == "https" && uri.host?.lowercase() in setOf("sharedabba.com", "www.sharedabba.com")) ||
        (uri.scheme == "sharedabba" && uri.host?.lowercase() in setOf("v", "watch", "video"))
    if (!validHost) return null
    val slug = if (uri.scheme == "sharedabba") {
        if (uri.host?.lowercase() !in setOf("v", "watch", "video")) return null
        segments.firstOrNull()
    } else {
        if (segments.size < 2 || segments.first().lowercase() !in setOf("v", "watch", "video")) return null
        segments[1]
    }
    return slug?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{3,128}")) }
}

@Composable
private fun GuestVideoSurface(url: String, title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {}
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Text(
            "‹  Back",
            color = Color.White,
            modifier = Modifier.padding(16.dp).clickable(onClick = onBack)
        )
        Text(title, color = Color.White, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
