package eu.kanade.presentation.browse

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import eu.kanade.data.source.NoResultsException
import eu.kanade.domain.manga.model.Manga
import eu.kanade.domain.source.interactor.GetRemoteManga
import eu.kanade.presentation.browse.components.BrowseSourceComfortableGrid
import eu.kanade.presentation.browse.components.BrowseSourceCompactGrid
import eu.kanade.presentation.browse.components.BrowseSourceEHentaiList
import eu.kanade.presentation.browse.components.BrowseSourceList
import eu.kanade.presentation.browse.components.BrowseSourceToolbar
import eu.kanade.presentation.components.DownloadedOnlyModeBanner
import eu.kanade.presentation.components.EmptyScreen
import eu.kanade.presentation.components.ExtendedFloatingActionButton
import eu.kanade.presentation.components.IncognitoModeBanner
import eu.kanade.presentation.components.LoadingScreen
import eu.kanade.presentation.components.Scaffold
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourcePresenter
import eu.kanade.tachiyomi.ui.library.setting.LibraryDisplayMode
import eu.kanade.tachiyomi.ui.more.MoreController
import eu.kanade.tachiyomi.widget.EmptyView
import exh.metadata.metadata.base.RaisedSearchMetadata
import exh.source.isEhBasedSource

@Composable
fun BrowseSourceScreen(
    presenter: BrowseSourcePresenter,
    navigateUp: () -> Unit,
    onFabClick: () -> Unit,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
    onWebViewClick: () -> Unit,
    // SY -->
    onSettingsClick: () -> Unit,
    // SY <--
    incognitoMode: Boolean,
    downloadedOnlyMode: Boolean,
) {
    val columns by presenter.getColumnsPreferenceForCurrentOrientation()

    val mangaList = presenter.getMangaList().collectAsLazyPagingItems()

    val snackbarHostState = remember { SnackbarHostState() }

    val uriHandler = LocalUriHandler.current

    val onHelpClick = {
        uriHandler.openUri(LocalSource.HELP_URL)
    }

    Scaffold(
        topBar = { scrollBehavior ->
            Column {
                BrowseSourceToolbar(
                    state = presenter,
                    source = presenter.source!!,
                    displayMode = presenter.displayMode.takeUnless { presenter.source!!.isEhBasedSource() && presenter.ehentaiBrowseDisplayMode },
                    onDisplayModeChange = { presenter.displayMode = it },
                    navigateUp = navigateUp,
                    onWebViewClick = onWebViewClick,
                    onHelpClick = onHelpClick,
                    onSearch = { presenter.search() },
                    // SY -->
                    onSettingsClick = onSettingsClick,
                    // SY <--
                    scrollBehavior = scrollBehavior,
                )
                if (downloadedOnlyMode) {
                    DownloadedOnlyModeBanner()
                }
                if (incognitoMode) {
                    IncognitoModeBanner()
                }
            }
        },
        floatingActionButton = {
            BrowseSourceFloatingActionButton(
                isVisible = presenter.filters.isNotEmpty(),
                onFabClick = onFabClick,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        BrowseSourceContent(
            state = presenter,
            mangaList = mangaList,
            getMangaState = { presenter.getManga(it) },
            // SY -->
            getMetadataState = { manga, metadata ->
                presenter.getRaisedSearchMetadata(manga, metadata)
            },
            // SY <--
            columns = columns,
            // SY -->
            ehentaiBrowseDisplayMode = presenter.ehentaiBrowseDisplayMode,
            // SY <--
            displayMode = presenter.displayMode,
            snackbarHostState = snackbarHostState,
            contentPadding = paddingValues,
            onWebViewClick = onWebViewClick,
            onHelpClick = { uriHandler.openUri(MoreController.URL_HELP) },
            onLocalSourceHelpClick = onHelpClick,
            onMangaClick = onMangaClick,
            onMangaLongClick = onMangaLongClick,
            header = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    FilterChip(
                        selected = presenter.currentQuery == GetRemoteManga.QUERY_POPULAR,
                        onClick = {
                            presenter.resetFilter()
                            presenter.search(GetRemoteManga.QUERY_POPULAR)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Favorite,
                                contentDescription = "",
                                modifier = Modifier
                                    .size(FilterChipDefaults.IconSize),
                            )
                        },
                        label = {
                            Text(text = stringResource(id = R.string.popular))
                        },
                    )
                    if (presenter.source?.supportsLatest == true) {
                        FilterChip(
                            selected = presenter.currentQuery == GetRemoteManga.QUERY_LATEST,
                            onClick = {
                                presenter.resetFilter()
                                presenter.search(GetRemoteManga.QUERY_LATEST)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.NewReleases,
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = {
                                Text(text = stringResource(id = R.string.latest))
                            },
                        )
                    }
                    /* SY --> if (presenter.filters.isNotEmpty())*/ run /* SY <-- */ {
                        FilterChip(
                            selected = presenter.currentQuery != GetRemoteManga.QUERY_POPULAR && presenter.currentQuery != GetRemoteManga.QUERY_LATEST,
                            onClick = onFabClick,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = "",
                                    modifier = Modifier
                                        .size(FilterChipDefaults.IconSize),
                                )
                            },
                            label = {
                                // SY -->
                                Text(
                                    text = if (presenter.filters.isNotEmpty()) {
                                        stringResource(id = R.string.action_filter)
                                    } else {
                                        stringResource(id = R.string.saved_searches)
                                    },
                                )
                                // SY <--
                            },
                        )
                    }
                }
            },
        )
    }
}

@Composable
fun BrowseSourceFloatingActionButton(
    modifier: Modifier = Modifier.navigationBarsPadding(),
    isVisible: Boolean,
    onFabClick: () -> Unit,
) {
    run {
        ExtendedFloatingActionButton(
            modifier = modifier,
            text = {
                Text(
                    text = if (isVisible) {
                        stringResource(id = R.string.action_filter)
                    } else {
                        stringResource(id = R.string.saved_searches)
                    },
                )
            },
            icon = { Icon(Icons.Outlined.FilterList, contentDescription = "") },
            onClick = onFabClick,
        )
    }
}

@Composable
fun BrowseSourceContent(
    state: BrowseSourceState,
    mangaList: LazyPagingItems</* SY --> */Pair<Manga, RaisedSearchMetadata?>/* SY <-- */>,
    getMangaState: @Composable ((Manga) -> State<Manga>),
    // SY -->
    getMetadataState: @Composable ((Manga, RaisedSearchMetadata?) -> State<RaisedSearchMetadata?>),
    // SY <--
    header: (@Composable () -> Unit)? = null,
    columns: GridCells,
    ehentaiBrowseDisplayMode: Boolean,
    displayMode: LibraryDisplayMode,
    snackbarHostState: SnackbarHostState,
    contentPadding: PaddingValues,
    // SY -->
    onWebViewClick: (() -> Unit)?,
    onHelpClick: (() -> Unit)?,
    onLocalSourceHelpClick: (() -> Unit)?,
    // SY <--
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    val context = LocalContext.current

    val errorState = mangaList.loadState.refresh.takeIf { it is LoadState.Error }
        ?: mangaList.loadState.append.takeIf { it is LoadState.Error }

    val getErrorMessage: (LoadState.Error) -> String = { state ->
        when {
            state.error is NoResultsException -> context.getString(R.string.no_results_found)
            state.error.message == null -> ""
            state.error.message!!.startsWith("HTTP error") -> "${state.error.message}: ${context.getString(R.string.http_error_hint)}"
            else -> state.error.message!!
        }
    }

    LaunchedEffect(errorState) {
        if (mangaList.itemCount > 0 && errorState != null && errorState is LoadState.Error) {
            val result = snackbarHostState.showSnackbar(
                message = getErrorMessage(errorState),
                actionLabel = context.getString(R.string.action_webview_refresh),
                duration = SnackbarDuration.Indefinite,
            )
            when (result) {
                SnackbarResult.Dismissed -> snackbarHostState.currentSnackbarData?.dismiss()
                SnackbarResult.ActionPerformed -> mangaList.refresh()
            }
        }
    }

    if (mangaList.itemCount <= 0 && errorState != null && errorState is LoadState.Error) {
        EmptyScreen(
            message = getErrorMessage(errorState),
            actions = if (state.source is LocalSource /* SY --> */ && onLocalSourceHelpClick != null /* SY <-- */) {
                listOf(
                    EmptyView.Action(R.string.local_source_help_guide, R.drawable.ic_help_24dp) { onLocalSourceHelpClick() },
                )
            } else {
                listOfNotNull(
                    EmptyView.Action(R.string.action_retry, R.drawable.ic_refresh_24dp) { mangaList.refresh() },
                    // SY -->
                    EmptyView.Action(R.string.action_open_in_web_view, R.drawable.ic_public_24dp) { onWebViewClick?.invoke() }.takeIf { onWebViewClick != null },
                    EmptyView.Action(R.string.label_help, R.drawable.ic_help_24dp) { onHelpClick?.invoke() }.takeIf { onHelpClick != null },
                    // SY <--
                )
            },
        )

        return
    }

    if (mangaList.itemCount == 0 && mangaList.loadState.refresh is LoadState.Loading) {
        LoadingScreen()
        return
    }

    // SY -->
    if (state.source?.isEhBasedSource() == true && ehentaiBrowseDisplayMode) {
        BrowseSourceEHentaiList(
            mangaList = mangaList,
            getMangaState = getMangaState,
            getMetadataState = getMetadataState,
            contentPadding = contentPadding,
            onMangaClick = onMangaClick,
            onMangaLongClick = onMangaLongClick,
            header = header,
        )
        return
    }
    // SY <--

    when (displayMode) {
        LibraryDisplayMode.ComfortableGrid -> {
            BrowseSourceComfortableGrid(
                mangaList = mangaList,
                getMangaState = getMangaState,
                // SY -->
                getMetadataState = getMetadataState,
                // SY <--
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
                header = header,
            )
        }
        LibraryDisplayMode.List -> {
            BrowseSourceList(
                mangaList = mangaList,
                getMangaState = getMangaState,
                // SY -->
                getMetadataState = getMetadataState,
                // SY <--
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
                header = header,
            )
        }
        else -> {
            BrowseSourceCompactGrid(
                mangaList = mangaList,
                getMangaState = getMangaState,
                // SY -->
                getMetadataState = getMetadataState,
                // SY <--
                columns = columns,
                contentPadding = contentPadding,
                onMangaClick = onMangaClick,
                onMangaLongClick = onMangaLongClick,
                header = header,
            )
        }
    }
}