package com.voidDeveloper.wastatussaver.presentation.ui.main.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.utils.LifecycleAwarePause
import com.voidDeveloper.wastatussaver.data.utils.createColoredString
import com.voidDeveloper.wastatussaver.data.utils.extentions.valueOrDefault
import com.voidDeveloper.wastatussaver.data.utils.extentions.valueOrEmptyString
import com.voidDeveloper.wastatussaver.data.utils.launchSafPicker
import com.voidDeveloper.wastatussaver.data.utils.openAppInPlayStore
import com.voidDeveloper.wastatussaver.domain.model.AudioFile
import com.voidDeveloper.wastatussaver.domain.model.ImageFile
import com.voidDeveloper.wastatussaver.domain.model.MediaFile
import com.voidDeveloper.wastatussaver.domain.model.UnknownFile
import com.voidDeveloper.wastatussaver.domain.model.VideoFile
import com.voidDeveloper.wastatussaver.navigation.Screens
import com.voidDeveloper.wastatussaver.presentation.theme.WaStatusSaverTheme
import com.voidDeveloper.wastatussaver.presentation.theme.gray
import com.voidDeveloper.wastatussaver.presentation.theme.light_gray
import com.voidDeveloper.wastatussaver.presentation.ui.main.dialog.AppNotInstalledDialog
import com.voidDeveloper.wastatussaver.presentation.ui.main.dialog.AutoSaveDialog
import com.voidDeveloper.wastatussaver.presentation.ui.main.dialog.NotificationPermissionDialog
import com.voidDeveloper.wastatussaver.presentation.ui.main.dialog.NotificationPermissionSettingsDialog
import com.voidDeveloper.wastatussaver.presentation.ui.main.dialog.OnBoardingDialog
import com.voidDeveloper.wastatussaver.presentation.ui.main.dialog.SAFAccessPermissionDialog
import com.voidDeveloper.wastatussaver.presentation.ui.player.PlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun MainScreen(
    uiState: StateFlow<UiState?>,
    onEvent: (Event) -> Unit,
    navigate: (String) -> Unit,
    infoState: Flow<String?>,
) {

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val titleItems = listOf(Title.Whatsapp, Title.WhatsappBusiness)
    val pagerState = rememberPagerState(pageCount = { 3 })
    val fileTypeData = listOf(FileType.IMAGES, FileType.VIDEOS, FileType.AUDIO)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scope.launch {
            infoState.collect { info ->
                if (info != null) {
                    Toast.makeText(context, info, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LifecycleAwarePause(onResume = {
        onEvent(Event.RefreshUiState)
    })

    val state by uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state?.mediaFiles) {
        Log.i("State TAG", "MainScreen: MediaFiles changed ${state?.mediaFiles?.map { it.id }}")
    }

    val launcher: ActivityResultLauncher<Intent> =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data
            if (uri != null && uri == state?.title?.uri) {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                onEvent(Event.ChangeSAFAccessPermission(true))
            }
        }

    ModalNavigationDrawer(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding(),
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                navigate = {
                    scope.launch {
                        navigate(it)
                        drawerState.close()
                    }
                }, closeDrawer = {
                    scope.launch {
                        drawerState.close()
                    }
                }, showOnBoardingUI = {
                    scope.launch {
                        onEvent(
                            Event.ChangeShowOnBoardingUiStatus(
                                true
                            )
                        )
                        drawerState.close()
                    }
                }, showAutoSaveDialog = {
                    scope.launch {
                        drawerState.close()
                        onEvent(Event.ShowAutoSaveDialog)
                    }
                }, enableAutoSave = state?.hasSafAccessPermission == true
            )
        },
        content = {
            Scaffold(
                modifier = Modifier.fillMaxSize(), topBar = {
                    MainTopBar(
                        openNavigationDrawer = { scope.launch { drawerState.open() } },
                        selectedTitle = state?.title,
                        items = titleItems,
                        onTitleChanged = { onEvent(Event.ChangeTitle(it)) },
                        pagerState = pagerState,
                        scope = scope,
                        fileTypeData = fileTypeData,
                        onRefreshBtnPressed = {
                            onEvent(Event.RefreshUiState)
                        })
                }) { innerPadding ->
                if (state != null) {
                    MainBody(
                        modifier = Modifier.padding(innerPadding),
                        pagerState = pagerState,
                        onEvent = onEvent,
                        launchSafPermission = { launcher.launch(it) },
                        uiState = state!!
                    )
                }
            }
        })
}

@Composable
fun MainTopBar(
    openNavigationDrawer: () -> Unit,
    selectedTitle: Title?,
    items: List<Title>,
    onTitleChanged: (Title) -> Unit,
    pagerState: PagerState,
    scope: CoroutineScope,
    fileTypeData: List<FileType>,
    onRefreshBtnPressed: () -> Unit,
) {

    var expanded by remember { mutableStateOf(false) }

    val rotationDegree by animateFloatAsState(
        if (expanded) 180f else 0f, animationSpec = tween(
            durationMillis = 400
        )
    )

    val targetSize = when (selectedTitle) {
        Title.Whatsapp -> 30.dp
        Title.WhatsappBusiness -> 60.dp
        null -> {
            30.dp
        }
    }

    val animatedSize by animateDpAsState(
        targetValue = targetSize, animationSpec = tween(durationMillis = 500)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .wrapContentHeight(), color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { openNavigationDrawer() }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Dropdown",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                TextButton(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    onClick = { expanded = !expanded }) {
                    Text(
                        text = stringResource(selectedTitle?.resId.valueOrEmptyString()).replace(
                            " ", "\n"
                        ),
                        modifier = Modifier.height(animatedSize),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .graphicsLayer(
                                rotationZ = rotationDegree, transformOrigin = TransformOrigin(
                                    0.5f, 0.5f
                                )
                            )
                            .align(Alignment.Top),
                    )
                }
                DropdownMenu(
                    expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEach { label ->
                        DropdownMenuItem(
                            text = { Text(stringResource(label.resId.valueOrEmptyString())) },
                            onClick = {
                                onTitleChanged(label)
                                expanded = false
                            })
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(modifier = Modifier.size(50.dp), onClick = onRefreshBtnPressed) {
                    Icon(
                        tint = Color.White,
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                    )
                }
            }
            TabsRow(pagerState, scope = scope, fileTypeData = fileTypeData)
        }
    }
}

@Composable
fun TabsRow(pagerState: PagerState, scope: CoroutineScope, fileTypeData: List<FileType>) {
    TabRow(
        containerColor = MaterialTheme.colorScheme.primary,
        selectedTabIndex = pagerState.currentPage,
        divider = {
            Spacer(modifier = Modifier.height(5.dp))
        },
        indicator = { tabPositions ->
            val currentTabPosition = tabPositions[pagerState.currentPage]
            Box(
                Modifier
                    .tabIndicatorOffset(currentTabPosition)
                    .height(4.dp)
                    .padding(horizontal = 20.dp)
                    .background(Color.White)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        fileTypeData.forEachIndexed { index, tab ->
            Tab(selected = pagerState.currentPage == index, onClick = {
                scope.launch {
                    pagerState.animateScrollToPage(index)
                }
            }, text = {
                Text(
                    text = tab.name,
                    color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSecondary
                )
            })
        }
    }
}

@SuppressLint("UseKtx")
@Composable
fun DrawerContent(
    navigate: (String) -> Unit,
    closeDrawer: () -> Unit,
    showOnBoardingUI: () -> Unit,
    showAutoSaveDialog: () -> Unit,
    enableAutoSave: Boolean,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth(0.70f)
            .background(MaterialTheme.colorScheme.primary)
            .fillMaxHeight()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 20.dp, horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(60.dp),
                painter = painterResource(R.drawable.ic_app_logo),
                contentDescription = ""
            )
            Text(
                modifier = Modifier.wrapContentWidth(),
                text = "WhatsApp\nStatus Saver",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(modifier = Modifier.size(50.dp), onClick = {}) {
                Icon(
                    tint = Color.White,
                    painter = if (isSystemInDarkTheme()) painterResource(R.drawable.ic_moon) else painterResource(
                        R.drawable.ic_sun
                    ),
                    contentDescription = ""
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            DrawerItem(
                title = stringResource(R.string.how_to_use),
                painter = painterResource(R.drawable.ic_howtouse),
                onBtnClick = {
                    showOnBoardingUI()
                },
            )
            DrawerItem(
                title = stringResource(R.string.saved_status),
                painter = painterResource(R.drawable.ic_saved),
                onBtnClick = {},
            )
            DrawerItem(
                title = stringResource(R.string.auto_save),
                painter = painterResource(R.drawable.ic_auto_save),
                onBtnClick = showAutoSaveDialog,
                enable = enableAutoSave
            )
            DrawerItem(
                title = stringResource(R.string.privacy_policy),
                painter = painterResource(R.drawable.ic_privacypolicy),
                onBtnClick = {
                    val encodedUrl = URLEncoder.encode(
                        "https://vsurendhar.github.io/WaStatusSaver_PrivacyPolicy/",
                        StandardCharsets.UTF_8.toString()
                    )
                    navigate("${Screens.WebView.route}/$encodedUrl")
                },
            )
            DrawerItem(
                title = stringResource(R.string.rate_app),
                painter = painterResource(R.drawable.ic_rating_star),
                onBtnClick = {},
            )
            DrawerItem(
                title = stringResource(R.string.more_app),
                painter = painterResource(R.drawable.ic_more),
                onBtnClick = {},
            )
            DrawerItem(
                title = stringResource(R.string.report_bug),
                painter = painterResource(R.drawable.ic_bug),
                onBtnClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("thevoiddevelopers@gmail.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Bug Report")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Hi Void Developers Team,\nI found the bug in your WaStatusSaver Android Application when..."
                        )
                    }

                    try {
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email client installed.", Toast.LENGTH_SHORT)
                            .show()
                    }
                    closeDrawer()
                },
            )
            DrawerItem(
                title = stringResource(R.string.about),
                painter = painterResource(R.drawable.ic_about),
                onBtnClick = {},
            )
        }
    }
}

@Composable
fun DrawerItem(title: String, painter: Painter, onBtnClick: () -> Unit, enable: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enable, onClick = { onBtnClick() })
            .padding(vertical = 10.dp)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painter,
            tint = gray,
            contentDescription = "Icon"
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title, style = MaterialTheme.typography.bodyLarge, color = gray
        )
    }
}


@Composable
fun MainBody(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    onEvent: (Event) -> Unit,
    uiState: UiState,
    launchSafPermission: (Intent) -> Unit,
) {
    val mediaFiles = uiState.mediaFiles
    val audioMediaFiles = mediaFiles.filter { it is AudioFile }
    val videoMediaFiles = mediaFiles.filter { it is VideoFile }
    val imageMediaFiles = mediaFiles.filter { it is ImageFile }
    HorizontalPager(
        state = pagerState, modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> {
                FilePreviewPage(
                    imageMediaFiles,
                    uiState,
                    onEvent = onEvent,
                    launchSafPermission = launchSafPermission
                )
            }

            1 -> {
                FilePreviewPage(
                    videoMediaFiles,
                    uiState,
                    onEvent = onEvent,
                    launchSafPermission = launchSafPermission
                )
            }

            2 -> {
                FilePreviewPage(
                    audioMediaFiles,
                    uiState,
                    onEvent = onEvent,
                    launchSafPermission = launchSafPermission
                )
            }

        }
    }
}

@Composable
fun FilePreviewPage(
    imageFiles: List<MediaFile>,
    uiState: UiState,
    onEvent: (Event) -> Unit,
    launchSafPermission: (Intent) -> Unit,
) {
    uiState.mediaFiles.forEach { println(it.id) }
    val activity = LocalActivity.current
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Log.d("Surendhar TAG 1 1", "Permission denied")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null) {
                val shouldShowRationale = activity.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS
                )
                Log.d("Surendhar TAG 1 1", "shouldShowRationale: $shouldShowRationale")

                if (shouldShowRationale) {
                    // Show Why Permission is Needed
                    Log.d(
                        "Surendhar TAG 1 1", "Showing notification permission dialog"
                    )
                    onEvent(Event.ShowNotificationPermissionDialog)
                } else {
                    // Show How to turn on the Notification Permission Settings in App Info
                    Log.d(
                        "Surendhar TAG 1 1", "Showing notification permission settings dialog"
                    )
                    onEvent(Event.ShowNotificationPermissionSettingsDialog)
                }
            }
        } else {
            Log.d("Surendhar TAG 1 1", "Permission granted")
        }
    }
    val context = LocalContext.current
    if (uiState.shouldShowOnBoardingUi == true) {
        OnBoardingDialog(onDialogDismissed = {
            onEvent(
                Event.ChangeShowOnBoardingUiStatus(
                    false
                )
            )
        })
    } else if (uiState.appInstalled == false && uiState.title != null) {
        AppNotInstalledDialog(title = uiState.title, onDownloadApp = {
            openAppInPlayStore(
                context = context, packageName = uiState.title.packageName
            )
        }, onNotNowPressed = {
            onEvent(Event.ChangeAppInstalledStatus(null))
        })
        return
    } else if (uiState.hasSafAccessPermission == false) {
        SAFAccessPermissionDialog(onGrantAccess = {
            launchSafPicker(
                newUri = uiState.title?.uri, launchPermission = launchSafPermission
            )
        }, onNotNowPressed = {
            onEvent(Event.ChangeSAFAccessPermission(null))
        })
        return
    } else if (uiState.showAutoSaveDialog) {
        AutoSaveDialog(
            selectedInterval = uiState.savedAutoSaveInterval,
            autoSaveEnable = uiState.autoSaveEnabled,
            onApplyPressed = { enabled, interval ->
                if (enabled) {
                    val hasPermission =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            activity?.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    if (!hasPermission) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                onEvent(Event.SaveAutoSaveData(interval, enabled))
            },
            onAutoSaveDialogDismissPressed = {
                onEvent(Event.AutoSaveDialogDismiss)
            })
    } else if (uiState.showNotificationPermissionDialog) {
        NotificationPermissionDialog(onOkPressed = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }, onDialogDismissed = {
            onEvent(
                Event.NotificationPermissionDialogDismiss
            )
        })
    } else if (uiState.showNotificationPermissionSettingsDialog) {
        val context = LocalContext.current
        NotificationPermissionSettingsDialog(onGoToSettingsPressed = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }, onCancelPressed = {
            onEvent(
                Event.NotificationSettingsDialogDismiss
            )
        })
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        color = Color.White,
    ) {
        if (uiState.appInstalled == null) {
            MissingSetupInfo(
                modifier = Modifier.padding(horizontal = 12.dp),
                imageId = R.drawable.ic_app_download,
                title = createColoredString(
                    stringResource(
                        R.string.not_installed,
                        stringResource(uiState.title?.resId.valueOrEmptyString())
                    ), MaterialTheme.colorScheme.primary
                ),
                description = createColoredString(
                    stringResource(
                        R.string.install_app_request,
                        stringResource(uiState.title?.resId.valueOrEmptyString())
                    ), MaterialTheme.colorScheme.primary
                ),
                buttonTxt = stringResource(
                    R.string.download_app_playstore_btn,
                    stringResource(id = uiState.title?.resId.valueOrEmptyString())
                ),
                onBtnClick = {
                    openAppInPlayStore(context, uiState.title?.packageName)
                })
        } else if (uiState.hasSafAccessPermission == null) {
            MissingSetupInfo(
                modifier = Modifier.padding(horizontal = 12.dp),
                imageId = R.drawable.ic_no_permission,
                title = createColoredString(
                    stringResource(R.string.storage_access_required),
                    MaterialTheme.colorScheme.primary
                ),
                description = createColoredString(
                    stringResource(
                        R.string.second_accesss_msg,
                        stringResource(uiState.title?.resId.valueOrEmptyString())
                    ), MaterialTheme.colorScheme.primary
                ),
                buttonTxt = stringResource(R.string.grand_access),
                onBtnClick = {
                    launchSafPicker(
                        newUri = uiState.title?.uri, launchPermission = launchSafPermission
                    )
                })
        } else {
            if (imageFiles.isEmpty()) {
                MissingSetupInfo(
                    imageId = R.drawable.ic_empty_folder, title = createColoredString(
                        stringResource(R.string.no_status_files_available),
                        MaterialTheme.colorScheme.primary
                    ), description = createColoredString(
                        stringResource(
                            R.string.no_status_des,
                            stringResource(uiState.title?.resId.valueOrEmptyString())
                        ), MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                LazyVerticalGrid(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = imageFiles, key = { it.id }) { mediaFile ->
                        PreviewItem(
                            mediaFile,
                            onDownloadClick = {
                                onEvent(Event.OnDownloadClick(mediaFile))
                            },
                            isDownloading = uiState.onGoingDownload.contains(mediaFile),
                            finishedDownloading = mediaFile.isDownloaded,
                            onPreviewClick = {
                                val intent = Intent(context, PlayerActivity::class.java)
//                                intent.extras?.putString("videoUri", "${mediaFile.uri.toString()}")
                                intent.putExtra("videoUri", "${mediaFile.uri.toString()}")
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun PreviewItem(
    mediaFile: MediaFile,
    onDownloadClick: () -> Unit,
    isDownloading: Boolean,
    finishedDownloading: Boolean,
    onPreviewClick: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .height(160.dp)
            .width(130.dp)
            .clickable {
                onPreviewClick()
            }, shape = RoundedCornerShape(
            corner = CornerSize(20.dp),
        ), border = BorderStroke(width = 1.dp, color = gray), colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (mediaFile is VideoFile || mediaFile is AudioFile) {
                var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
                var isLoading by remember { mutableStateOf(true) }

                LaunchedEffect(mediaFile.id) {
                    isLoading = true
                    imageBitmap = withContext(Dispatchers.IO) {
                        mediaFile.getThumbNailBitMap(context)?.asImageBitmap()
                    }
                    isLoading = false
                }

                if (isLoading) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.img_loading_placeholder),
                        contentDescription = "Loading",
                        contentScale = ContentScale.Crop
                    )
                } else if (imageBitmap != null) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        bitmap = imageBitmap!!,
                        contentDescription = "Video Thumbnail",
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painterResource(R.drawable.ic_failed_document),
                        contentDescription = "Failed"
                    )
                }
            } else {
                AsyncImage(
                    model = mediaFile.uri,
                    contentDescription = "Status Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.img_loading_placeholder),
                    error = painterResource(R.drawable.ic_failed_document)
                )
            }
            Card(
                colors = CardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Gray,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Gray
                ),
                modifier = Modifier
                    .height(26.dp)
                    .width(46.dp)
                    .align(Alignment.BottomEnd),
                shape = RoundedCornerShape(topStart = 10.dp),
            ) {
                IconButton(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 6.dp)
                        .align(Alignment.CenterHorizontally), onClick = {
                        onDownloadClick()
                    }, enabled = !isDownloading && !finishedDownloading
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (!isDownloading && !finishedDownloading) {
                            Icon(
                                painter = painterResource(R.drawable.ic_download),
                                contentDescription = "Download",
                                tint = light_gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (finishedDownloading) {
                            Icon(
                                painter = painterResource(R.drawable.ic_tick),
                                contentDescription = "Finished Download",
                                modifier = Modifier.size(14.dp),
                                tint = light_gray,
                            )
                        }
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(15.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun MissingSetupInfo(
    modifier: Modifier = Modifier,
    @DrawableRes imageId: Int,
    title: AnnotatedString,
    description: AnnotatedString,
    buttonTxt: String? = null,
    onBtnClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(12.dp),
        colors = CardColors(
            containerColor = Color.White,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.LightGray,
            contentColor = Color.Unspecified
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(imageId),
                contentDescription = "",
                modifier = Modifier.size(100.dp)
            )
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
                text = title, style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = description,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                color = Color.Black,
                style = MaterialTheme.typography.bodyLarge
            )
            if (buttonTxt != null && onBtnClick != null) {
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    onClick = { onBtnClick.invoke() }) {
                    androidx.compose.material.Text(
                        text = buttonTxt.valueOrDefault(),
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
            }
        }
    }
}


@Composable
@Preview(showBackground = true)
fun TopBarPreview() {
    WaStatusSaverTheme {
        MainTopBar(
            openNavigationDrawer = {},
            selectedTitle = Title.Whatsapp,
            items = emptyList(),
            {},
            pagerState = rememberPagerState(pageCount = { 3 }),
            rememberCoroutineScope(),
            fileTypeData = listOf(FileType.IMAGES, FileType.VIDEOS, FileType.AUDIO),
            onRefreshBtnPressed = {})
    }
}


@Composable
@Preview(showBackground = true)
fun NavigationDrawerPreview() {
    WaStatusSaverTheme {
        DrawerContent(
            navigate = {},
            closeDrawer = {},
            showOnBoardingUI = {},
            showAutoSaveDialog = {},
            enableAutoSave = true,
        )
    }
}


@Composable
@Preview(showBackground = true)
fun ImagePagePreview() {
    val imageImageFiles = (1..5).map {
        ImageFile(
            uri = "".toUri(), fileType = FileType.IMAGES, fileName = ""
        )
    }
    WaStatusSaverTheme {
        FilePreviewPage(
            imageImageFiles,
            uiState = UiState(),
            onEvent = { },
            launchSafPermission = {})
    }
}

@Composable
@Preview(showBackground = true)
fun FileItemPreview() {
    WaStatusSaverTheme {
        PreviewItem(
            mediaFile = UnknownFile(uri = "".toUri()),
            onDownloadClick = {},
            isDownloading = false,
            finishedDownloading = false,
            onPreviewClick = {}
        )
    }
}


@Composable
@Preview(showBackground = true)
fun MissingSetupInfoPreview() {
    WaStatusSaverTheme {
        MissingSetupInfo(
            imageId = R.drawable.ic_empty_folder,
            title = AnnotatedString("Storage Access Required"),
            description = AnnotatedString("We need access to the WhatsApp .Statuses folder to save status media. Only files in the folder you choose are accessed, and nothing leaves your phone."),
            buttonTxt = "Grand Access",
            onBtnClick = {})
    }
}

