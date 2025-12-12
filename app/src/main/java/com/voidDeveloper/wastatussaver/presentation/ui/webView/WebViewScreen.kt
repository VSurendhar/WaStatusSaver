package com.voidDeveloper.wastatussaver.presentation.ui.webView

import android.webkit.WebChromeClient
import android.webkit.WebView
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.utils.extentions.findActivity
import com.voidDeveloper.wastatussaver.data.utils.extentions.isInternetAvailable
import com.voidDeveloper.wastatussaver.presentation.ui.main.MainActivity

@Composable
fun WebViewScreen(url: String? = "https://www.google.com") {

    val webView =
        (LocalContext.current.findActivity() as MainActivity).webViewManager.getWebViewInstance()

    val context = LocalContext.current
    var internetAvailable by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        internetAvailable = context.isInternetAvailable()
    }
    Scaffold(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding(),
        topBar = {
            PrivacyPolicyTopBar(onRefreshPressed = {
                internetAvailable = context.isInternetAvailable()
            })
        }) { innerPadding ->
        if (internetAvailable) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(),
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    )
                }
                AndroidView(modifier = Modifier.padding(innerPadding), factory = {
                    webView.apply {
                        settings.domStorageEnabled = true
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress / 100f
                                isLoading = newProgress < 100
                            }
                        }
                        loadUrl(url ?: "https://www.google.com")
                    }
                })
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
                        fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 22.sp
                    ), text = "No Internet Connection", modifier = Modifier.padding(vertical = 5.dp)
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
fun PrivacyPolicyTopBar(onRefreshPressed: () -> Unit) {
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
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    contentDescription = "Refresh",
                )
            }
        }
    }


}


