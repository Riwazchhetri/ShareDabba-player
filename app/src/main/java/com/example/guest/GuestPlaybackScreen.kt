package com.example.guest

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.core.config.EnvironmentConfig
import com.example.network.NetworkClient
import kotlinx.coroutines.launch
import java.net.URI

private const val API_ROOT = "https://sharedabba-pwa-api.onrender.com"
private val PageBackground = Color(0xFF090A12)
private val CardBackground = Color(0xFF151827)
private val Accent = Color(0xFF9B7BFF)
private val MutedText = Color(0xFFADB3C8)

@Composable
fun GuestPlaybackScreen(initialLink: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        if (playbackUrl != null) {
            GuestVideoSurface(url = playbackUrl!!, title = videoTitle, onBack = { playbackUrl = null })
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 42.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Accent) {
                        Text(
                            "SD",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text("ShareDabba", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("PLAYER", color = MutedText, fontSize = 10.sp, letterSpacing = 2.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(shape = CircleShape, color = Color(0xFF20263A)) {
                        Text("●", color = Color(0xFF64DDAA), modifier = Modifier.padding(10.dp), fontSize = 10.sp)
                    }
                }

                Spacer(Modifier.weight(1f))
                Text(
                    "Your videos,\none tap away.",
                    color = Color.White,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Paste a ShareDabba link below to start watching.",
                    color = MutedText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(Modifier.fillMaxWidth().padding(18.dp)) {
                        Text("OPEN A VIDEO", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = link,
                            onValueChange = { link = it; error = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Paste video link") },
                            placeholder = { Text("https://sharedabba.com/v/…") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val slug = extractShareSlug(link)
                                if (slug == null) {
                                    error = "Please enter a valid ShareDabba video link."
                                } else {
                                    loading = true
                                    error = null
                                    scope.launch {
                                        try {
                                            val response = api.getGuestPlayback(slug)
                                            videoTitle = response.file.name
                                            creator = response.file.creator.orEmpty()
                                            playbackUrl = if (response.playbackUrl.startsWith("http")) {
                                                response.playbackUrl
                                            } else {
                                                API_ROOT + response.playbackUrl
                                            }
                                        } catch (e: retrofit2.HttpException) {
                                            error = when (e.code()) {
                                                404, 410 -> "This video is unavailable or has expired."
                                                else -> "We couldn't load this video. Please try again."
                                            }
                                        } catch (_: Exception) {
                                            error = "Connection problem. Check your internet and try again."
                                        } finally {
                                            loading = false
                                        }
                                    }
                                }
                            },
                            enabled = !loading && link.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Play video  ›", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!error.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(error!!, color = Color(0xFFFF8585), fontSize = 13.sp)
                        }
                        if (creator.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Text("Uploaded by $creator", color = MutedText, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("No account needed • ShareDabba Player", color = MutedText, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun extractShareSlug(raw: String): String? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    if (!value.contains("://") && value.matches(Regex("[A-Za-z0-9_-]{3,128}"))) return value

    val uri = try { URI(value) } catch (_: Exception) { return null }
    if (uri.userInfo != null || uri.port != -1) return null

    val host = uri.host?.lowercase() ?: return null
    val segments = uri.path.orEmpty().split('/').filter { it.isNotBlank() }
    val slug = when {
        uri.scheme == "https" && host in setOf("sharedabba.com", "www.sharedabba.com") -> {
            if (segments.size < 2 || segments.first().lowercase() !in setOf("v", "watch", "video")) return null
            segments[1]
        }
        uri.scheme == "https" && host == "sharedabba-pwa-api.onrender.com" -> {
            if (segments.size != 2 || segments.first().lowercase() != "s") return null
            segments[1]
        }
        uri.scheme == "sharedabba" && host in setOf("v", "watch", "video") -> segments.firstOrNull()
        else -> return null
    }
    return slug?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{3,128}")) }
}

@Composable
private fun GuestVideoSurface(url: String, title: String, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("‹  Back", color = Color.White, modifier = Modifier.clickable(onClick = onBack).padding(10.dp))
            Text(title, color = Color.White, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
        }
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
    }
}
