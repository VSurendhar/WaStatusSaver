package com.voidDeveloper.wastatussaver.ui.main

import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.voidDeveloper.wastatussaver.R
import com.voidDeveloper.wastatussaver.data.utils.extentions.valueOrEmptyString
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
    val pagerState = rememberPagerState(pageCount = { 2 })
    val fileTypeData = listOf(FileType.IMAGES, FileType.VIDEOS)
    val context = LocalContext.current

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
                if (state?.shouldShowOnBoardingUi == true) {
                    OnBoardingDialog(onDialogDismissed = {
                        onEvent(
                            Event.ChangeShowOnBoardingUiStatus(
                                false
                            )
                        )
                    })
                } else if (state?.appInstalled == false && state?.title != null) {
                    AppNotInstalledDialog(
                        title = state!!.title!!, onDownloadApp = {
                            openAppInPlayStore(
                                context = context, packageName = state?.title?.packageName
                            )
                        })
                } else if (state?.hasSafAccessPermission == false) {
                    SAFAccessPermissionDialog(onGrantAccess = {
                        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                            putExtra(
                                DocumentsContract.EXTRA_INITIAL_URI, state?.title?.uri
                            )
                        }
                        launcher.launch(intent)
                    })
                } else if (state != null) {
                    MainBody(
                        title = state!!.title!!,
                        modifier = Modifier.padding(innerPadding),
                        pagerState = pagerState,
                        onEvent = onEvent
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
                    .padding(horizontal = 18.dp, vertical = 8.dp),
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
    title: Title,
) {
    val imageFiles = (1..20).map {
        File(
            uri = "".toUri(), fileType = FileType.IMAGES
        )
    }
    val videoFiles = (1..20).map {
        File(
            uri = "".toUri(), fileType = FileType.VIDEOS
        )
    }
    HorizontalPager(
        state = pagerState, modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> {
                onEvent(Event.FetchStatusesMedia(title))
                FilePreviewPage(imageFiles)
            }

            1 -> {
                onEvent(Event.FetchStatusesMedia(title))
                FilePreviewPage(videoFiles)
            }
        }
    }
}

@Composable
fun FilePreviewPage(files: List<File>) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(12.dp),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = files, key = { it.id }) {
                PreviewItem()
            }
        }
    }
}

@Composable
fun PreviewItem() {
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
            Image(
                modifier = Modifier.fillMaxSize(),
                painter = painterResource(R.drawable.img_sample),
                contentDescription = "Status Image"
            )
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
@Preview(showBackground = true)
fun TopBarPreview() {
    WaStatusSaverTheme {
        MainTopBar(
            openNavigationDrawer = {},
            selectedTitle = Whatsapp,
            items = emptyList(),
            {},
            pagerState = rememberPagerState(pageCount = { 2 }),
            rememberCoroutineScope(),
            fileTypeData = listOf(FileType.IMAGES, FileType.VIDEOS),
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
    val imageFiles = (1..20).map {
        File(
            uri = "".toUri(), fileType = FileType.IMAGES
        )
    }
    WaStatusSaverTheme {
        FilePreviewPage(imageFiles)
    }
}

@Composable
@Preview(showBackground = true)
fun FileItemPreview() {
    WaStatusSaverTheme {
        PreviewItem()
    }
}
