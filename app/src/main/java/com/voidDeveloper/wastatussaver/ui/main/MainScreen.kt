package com.voidDeveloper.wastatussaver.ui.main

import android.content.Intent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.utils.LifecycleAwarePause
import com.voidDeveloper.wastatussaver.data.utils.createColoredString
import com.voidDeveloper.wastatussaver.data.utils.extentions.valueOrDefault
import com.voidDeveloper.wastatussaver.data.utils.extentions.valueOrEmptyString
import com.voidDeveloper.wastatussaver.data.utils.launchSafPicker
import com.voidDeveloper.wastatussaver.data.utils.openAppInPlayStore
import com.voidDeveloper.wastatussaver.ui.main.Title.Whatsapp
import com.voidDeveloper.wastatussaver.ui.main.Title.WhatsappBusiness
import com.voidDeveloper.wastatussaver.ui.main.dialog.AppNotInstalledDialog
import com.voidDeveloper.wastatussaver.ui.main.dialog.OnBoardingDialog
import com.voidDeveloper.wastatussaver.ui.main.dialog.SAFAccessPermissionDialog
import com.voidDeveloper.wastatussaver.ui.theme.WaStatusSaverTheme
import com.voidDeveloper.wastatussaver.ui.theme.gray
import com.voidDeveloper.wastatussaver.ui.theme.light_gray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun MainScreen(uiState: StateFlow<UiState?>, onEvent: (Event) -> Unit) {

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val titleItems = listOf(Whatsapp, WhatsappBusiness)
    val pagerState = rememberPagerState(pageCount = { 3 })
    val fileTypeData = listOf(FileType.IMAGES, FileType.VIDEOS, FileType.AUDIO)
    val context = LocalContext.current

    LifecycleAwarePause(onResume = {
        onEvent(Event.RefreshUiState)
    })

    val state by uiState.collectAsState()
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
            DrawerContent()
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
                    )
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
) {

    var expanded by remember { mutableStateOf(false) }

    val rotationDegree by animateFloatAsState(
        if (expanded) 180f else 0f, animationSpec = tween(
            durationMillis = 400
        )
    )

    val targetSize = when (selectedTitle) {
        Whatsapp -> 30.dp
        WhatsappBusiness -> 60.dp
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
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .padding(horizontal = 4.dp)
                        .align(Alignment.Top),
                )
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

@Composable
fun DrawerContent() {
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
                painter = painterResource(R.drawable.ic_howtouse)
            )
            DrawerItem(
                title = stringResource(R.string.saved_status),
                painter = painterResource(R.drawable.ic_saved)
            )
            DrawerItem(
                title = stringResource(R.string.auto_save),
                painter = painterResource(R.drawable.ic_auto_save)
            )
            DrawerItem(
                title = stringResource(R.string.privacy_policy),
                painter = painterResource(R.drawable.ic_privacypolicy)
            )
            DrawerItem(
                title = stringResource(R.string.rate_app),
                painter = painterResource(R.drawable.ic_rating_star)
            )
            DrawerItem(
                title = stringResource(R.string.more_app),
                painter = painterResource(R.drawable.ic_more)
            )
            DrawerItem(
                title = stringResource(R.string.report_bug),
                painter = painterResource(R.drawable.ic_bug)
            )
            DrawerItem(
                title = stringResource(R.string.about),
                painter = painterResource(R.drawable.ic_about)
            )
        }
    }
}

@Composable
fun DrawerItem(title: String, painter: Painter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = true, onClick = {})
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
    } else if (uiState.hasSafAccessPermission == false) {
        SAFAccessPermissionDialog(onGrantAccess = {
            launchSafPicker(
                newUri = uiState.title?.uri, launchPermission = launchSafPermission
            )
        }, onNotNowPressed = {
            onEvent(Event.ChangeSAFAccessPermission(null))
        })
    } else {
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
                            PreviewItem(mediaFile)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PreviewItem(mediaFile: MediaFile) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .height(160.dp)
            .width(130.dp), shape = RoundedCornerShape(
            corner = CornerSize(20.dp),
        ), border = BorderStroke(width = 1.dp, color = gray), colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageBitmap = mediaFile.getThumbNailBitMap(context = context)?.asImageBitmap()
            if (imageBitmap != null) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = imageBitmap,
                    contentDescription = "Status Image",
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(R.drawable.ic_failed_document),
                    contentDescription = "Status Image"
                )
            }
            Card(
                onClick = {},
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
                        .align(Alignment.CenterHorizontally), onClick = { }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_download),
                        contentDescription = "Download",
                        tint = light_gray,
                        modifier = Modifier.size(12.dp)
                    )
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
            selectedTitle = Whatsapp,
            items = emptyList(),
            {},
            pagerState = rememberPagerState(pageCount = { 3 }),
            rememberCoroutineScope(),
            fileTypeData = listOf(FileType.IMAGES, FileType.VIDEOS, FileType.AUDIO),
        )
    }
}


@Composable
@Preview(showBackground = true)
fun NavigationDrawerPreview() {
    WaStatusSaverTheme {
        DrawerContent()
    }
}


@Composable
@Preview(showBackground = true)
fun ImagePagePreview() {
    val imageImageFiles = (1..5).map {
        ImageFile(
            uri = "".toUri(), fileType = FileType.IMAGES
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
            mediaFile = UnknownFile(uri = "".toUri())
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

