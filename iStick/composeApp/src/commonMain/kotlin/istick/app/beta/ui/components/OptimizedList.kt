// Adaugă aceste modificări la fișierul OptimizedList.kt existent
@Composable
fun OptimizedOffersList(
    offers: List<Offer>,
    onOfferClick: (Offer) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()

    val visibleOffers by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                emptyList()
            } else {
                val firstVisibleIndex = visibleItemsInfo.first().index
                val lastVisibleIndex = visibleItemsInfo.last().index

                offers.subList(
                    maxOf(0, firstVisibleIndex - 5),
                    minOf(offers.size, lastVisibleIndex + 5)
                )
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { index ->
                if (index > offers.size - 10) {
                    // Trigger încărcare pagină următoare
                }
            }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
    ) {
        itemsIndexed(
            items = offers,
            key = { _, offer -> offer.id }
        ) { index, offer ->
            val isVisible = index >= lazyListState.firstVisibleItemIndex - 5 &&
                    index <= lazyListState.firstVisibleItemIndex + lazyListState.layoutInfo.visibleItemsInfo.size + 5

            if (isVisible) {
                OfferItemCard(
                    offer = offer,
                    onClick = { onOfferClick(offer) },
                    modifier = Modifier.animateItemPlacement()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(8.dp)
                )
            }
        }
    }
}