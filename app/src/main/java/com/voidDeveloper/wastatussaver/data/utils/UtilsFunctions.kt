package com.voidDeveloper.wastatussaver.data.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun openAppInPlayStore(context: Context, packageName: String?) {
    try {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()
            )
        )
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$packageName".toUri()
            )
        )
    }
}

@Composable
fun Floating(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    durationMillis: Int = 5000,
    amplitudeX: Float = 6f,
    amplitudeY: Float = 12f,
    rotationAmplitude: Float = 1.5f,
    scaleAmplitude: Float = 0.03f,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "floating_animation")

    // Primary animation for X movement (sine wave)
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_progress"
    )

    // Secondary animation for Y movement (cosine wave, different phase)
    val secondaryProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((durationMillis * 1.3f).toInt()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_secondary"
    )

    // Tertiary animation for rotation and scale
    val tertiaryProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween((durationMillis * 0.8f).toInt()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating_tertiary"
    )

    // Calculate floating transformations
    val angleRadians = animationProgress * 2 * PI.toFloat()
    val secondaryAngle = secondaryProgress * 2 * PI.toFloat()
    val tertiaryAngle = tertiaryProgress * 2 * PI.toFloat()

    // Create figure-8 like movement pattern
    val translationX = amplitudeX * sin(angleRadians) * cos(secondaryAngle * 0.5f)
    val translationY = amplitudeY * cos(angleRadians * 0.7f) * sin(secondaryAngle * 0.8f)

    // Gentle rotation
    val rotation = rotationAmplitude * sin(tertiaryAngle * 0.6f)

    // Subtle scale breathing effect
    val scale = 1f + (scaleAmplitude * sin(tertiaryAngle * 0.4f))

    Box(
        modifier = modifier
            .graphicsLayer {
                this.translationX = translationX
                this.translationY = translationY
                this.rotationZ = rotation
                this.scaleX = scale
                this.scaleY = scale
            }
    ) {
        content()
    }
}

@Composable
fun LifecycleAwareEventCallBacks(
    onPause: (() -> Unit)? = null,
    onDestroy: (() -> Unit)? = null,
    onResume: (() -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    onPause?.invoke()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    onDestroy?.invoke()
                }

                Lifecycle.Event.ON_RESUME -> {
                    onResume?.invoke()
                }

                else -> {
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

fun launchSafPicker(newUri: Uri?, launchPermission: (Intent) -> Unit) {
    val newTreeUri = DocumentsContract.buildDocumentUriUsingTree(
        newUri, DocumentsContract.getTreeDocumentId(newUri)
    )
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        putExtra(DocumentsContract.EXTRA_INITIAL_URI, newTreeUri)
    }
    launchPermission(intent)
}

fun createColoredString(
    baseString: String,
    color: Color,
): AnnotatedString {

    val annotatedText = buildAnnotatedString {
        var currentIndex = 0
        val regex = "\\*\\*(.*?)\\*\\*".toRegex()
        regex.findAll(baseString).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1

            if (start > currentIndex) {
                append(baseString.substring(currentIndex, start))
            }

            var word = matchResult.groups[1]?.value ?: ""
            withStyle(style = SpanStyle(color = color)) {
                if (word.isNotEmpty()) word = "$word "
                append(word)
            }

            currentIndex = end + 1
        }

        if (currentIndex < baseString.length) {
            append(baseString.substring(currentIndex))
        }
    }

    return annotatedText
}

fun Bitmap.compressBitmapQuality(quality: Int = 50): Bitmap? {
    return try {
        val stream = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val byteArray = stream.toByteArray()
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getMillisFromNow(hour: Int): Long {
    return System.currentTimeMillis() + TimeUnit.HOURS.toMillis(hour.toLong())
}

fun formatTime(millis: Long): String {
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))
}

fun escapeForMarkdownV2(text: String): String {
    val charsToEscape = listOf(
        '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'
    )
    var escaped = text
    charsToEscape.forEach { char ->
        escaped = escaped.replace(char.toString(), "\\$char")
    }
    return escaped
}