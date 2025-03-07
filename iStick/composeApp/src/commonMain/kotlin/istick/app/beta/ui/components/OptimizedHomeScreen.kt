// În ui.components/OptimizedHomeScreen.kt
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    performanceMonitor: PerformanceMonitor,
    modifier: Modifier = Modifier
) {
    // Începe măsurarea performanței
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("home_screen_render")
    }

    val offers by viewModel.offers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingNextPage by viewModel.isLoadingNextPage.collectAsState()
    val error by viewModel.error.collectAsState()
    val hasMorePages by viewModel.hasMorePages.collectAsState()

    // Încarcă ofertele când ecranul este afișat
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("initial_offers_load")
        viewModel.loadOffers()
    }

    // Layout principal
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F2030))
            .measureRenderTime("home_content", performanceMonitor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Oferte Disponibile",
            style = MaterialTheme.typography.h5,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Afișează loader în timpul încărcării inițiale
        if (isLoading && offers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF2962FF),
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            // LazyColumn cu oferte
            val lazyListState = rememberLazyListState()

            // Trigger pentru încărcarea următoarei pagini când ne apropiem de sfârșitul listei
            LaunchedEffect(lazyListState) {
                snapshotFlow {
                    val layoutInfo = lazyListState.layoutInfo
                    val visibleItemsInfo = layoutInfo.visibleItemsInfo
                    if (visibleItemsInfo.isEmpty()) {
                        false
                    } else {
                        val lastVisibleItem = visibleItemsInfo.last()
                        val lastIndex = lastVisibleItem.index
                        lastIndex >= layoutInfo.totalItemsCount - 5
                    }
                }
                    .filter { it }
                    .collect {
                        if (hasMorePages && !isLoadingNextPage) {
                            performanceMonitor.startTrace("load_next_page")
                            viewModel.loadNextPage()
                            performanceMonitor.stopTrace("load_next_page")
                        }
                    }
            }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(
                    items = offers,
                    key = { it.id }
                ) { offer ->
                    OfferCard(
                        offer = offer,
                        onClick = { /* Navigare la detalii */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .animateItemPlacement()
                    )
                }

                // Footer pentru încărcarea paginii următoare
                if (isLoadingNextPage) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF2962FF),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // Afișează erori
        error?.let {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                backgroundColor = Color(0xFF800000),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.White
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = it,
                        color = Color.White
                    )
                }
            }
        }
    }

    // Oprește măsurarea la ieșire
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("home_screen_render")
            performanceMonitor.stopTrace("initial_offers_load")
        }
    }
}

@Composable
private fun OfferCard(
    offer: Offer,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick),
        backgroundColor = Color(0xFF1A3B66),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Folosește componenta OptimizedImage
            OptimizedImage(
                imageUrl = offer.imageUrl,
                contentDescription = "Company logo",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = offer.companyName,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "${offer.price} lei",
                    color = Color.White.copy(alpha = 0.8f)
                )

                Text(
                    text = offer.description,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}