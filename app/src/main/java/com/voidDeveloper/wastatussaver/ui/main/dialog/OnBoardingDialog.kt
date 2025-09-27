package com.voidDeveloper.wastatussaver.ui.main.dialog

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.utils.Floating
import com.voidDeveloper.wastatussaver.ui.theme.WaStatusSaverTheme
import kotlinx.coroutines.launch

@Composable
fun OnBoardingDialog(onDialogDismissed: () -> Unit) {
    var openDialog by remember { mutableStateOf(true) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    if (openDialog) {
        Dialog(
            onDismissRequest = { openDialog = false },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Skip",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            openDialog = false
                        },
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalPager(
                    state = pagerState, modifier = Modifier.wrapContentSize()
                ) { page ->
                    when (page) {
                        0 -> {
                            OnBoardingBody(
                                imageRes = R.drawable.ic_onboardingimage2,
                                title = "View in WhatsApp",
                                bodyText = "Watch photo or video statuses directly in WhatsApp. Once you view them, our app will detect them automatically."
                            )
                        }

                        1 -> {
                            OnBoardingBody(
                                imageRes = R.drawable.ic_onboardingimage1,
                                title = "Find Them in the App",
                                bodyText = "Every status you’ve viewed will instantly appear here. All photos and videos are listed in one place for quick access."
                            )
                        }

                        2 -> {
                            OnBoardingBody(
                                imageRes = R.drawable.ic_onboardingimage3,
                                title = "Save What You Like",
                                bodyText = "Tap the save button to download any status to your gallery. Keep it forever, even after it disappears from WhatsApp."
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    repeat(pagerState.pageCount) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                        )
                    }
                }

                Text(
                    text = "Next",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (pagerState.currentPage < 2) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                openDialog = false
                            }
                        },
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.primary
                )

            }
        }
    } else {
        onDialogDismissed()
    }
}

@Composable
fun OnBoardingBody(@DrawableRes imageRes: Int, title: String, bodyText: String) {
    Column(
        modifier = Modifier
            .height(280.dp)
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Floating(modifier = Modifier.padding(horizontal = 16.dp)) {
            Image(
                modifier = Modifier
                    .size(130.dp)
                    .padding(horizontal = 12.dp),
                painter = painterResource(imageRes),
                contentDescription = "OnBoarding Image"
            )
        }
        Text(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
            text = title,
            color = Color.Black,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            modifier = Modifier.padding(horizontal = 12.dp),
            text = bodyText,
            color = Color.Black,
            lineHeight = 22.sp,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnBoardingScreenPreview() {
    WaStatusSaverTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            OnBoardingDialog {}
        }
    }
}
