package com.voiddevelopers.wastatussaver.presentation.ui.webView

import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.voiddevelopers.wastatussaver.R
import com.voiddevelopers.wastatussaver.data.utils.extentions.isInternetAvailable
import com.voiddevelopers.wastatussaver.data.utils.extentions.singleClick
import com.voiddevelopers.wastatussaver.presentation.theme.WaStatusSaverTheme

@Composable
fun WebViewScreen(url: String? = "https://www.google.com", onBackClick: () -> Unit) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<WebViewViewModel>()
    val pageUrl = url ?: "https://www.google.com"
    var internetAvailable by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }

    // Created when the screen opens and destroyed when it closes (see onDispose below).
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowFileAccess = false
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                defaultTextEncodingName = "utf-8"
                blockNetworkLoads = false
            }
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress = newProgress / 100f
                    isLoading = newProgress < 100
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // Also called after an error; the ViewModel ignores it then,
                    // because the failure was already logged for this load.
                    viewModel.logLoadResult(success = true)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    // Only the page itself counts, not images or scripts inside it.
                    if (request?.isForMainFrame == true) {
                        viewModel.logLoadResult(
                            success = false,
                            errorCode = error?.errorCode,
                            errorDescription = error?.description?.toString()
                        )
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?
                ) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    if (request?.isForMainFrame == true) {
                        viewModel.logLoadResult(
                            success = false,
                            errorCode = errorResponse?.statusCode,
                            errorDescription = errorResponse?.reasonPhrase
                        )
                    }
                }
            }
        }
    }

    // The only place the page is loaded. Success / failure is reported by the WebView callbacks.
    fun loadPage() {
        viewModel.startNewLoad()
        isLoading = true
        webView.loadUrl(pageUrl)
    }

    LaunchedEffect(Unit) {
        internetAvailable = context.isInternetAvailable() // decides which UI is shown
        loadPage()
    }

    DisposableEffect(Unit) {
        onDispose {
            // Stops any load in progress (so nothing is logged after the user has left)
            // and frees the WebView.
            webView.stopLoading()
            webView.destroy()
        }
    }

    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding(),
        topBar = {
            PrivacyPolicyTopBar(
                onRefreshPressed = {
                    viewModel.onRefreshClick()
                    internetAvailable = context.isInternetAvailable() // decides which UI is shown
                    loadPage()
                },
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        if (internetAvailable) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                }
                AndroidView(modifier = Modifier.fillMaxSize(), factory = { webView })
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.padding(horizontal = 120.dp, vertical = 30.dp),
                    contentDescription = "No Internet Found",
                    painter = painterResource(R.drawable.ic_no_network),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 22.sp
                    ),
                    text = "No Internet Connection",
                    modifier = Modifier.padding(vertical = 5.dp)
                )
                Text(
                    text = "Please turn on your Wifi or Mobile Data to see the content.Tap the refresh icon above to try again later.",
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 30.dp)
                )
            }
        }
    }
}

@Composable
fun PrivacyPolicyTopBar(onRefreshPressed: () -> Unit, onBackClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .wrapContentHeight(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .padding(horizontal = 18.dp)
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier
                    .height(30.dp)
                    .padding(end = 12.dp)
                    .singleClick {
                        onBackClick()
                    }
            )
            Text(
                modifier = Modifier,
                text = "Privacy Policy",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(modifier = Modifier.size(50.dp), onClick = onRefreshPressed) {
                Icon(
                    tint = Color.White,
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }
    }
}

@Composable
@Preview
fun PreviewWebViewScreen() {
    WaStatusSaverTheme {
        WebViewScreen {}
    }
}
