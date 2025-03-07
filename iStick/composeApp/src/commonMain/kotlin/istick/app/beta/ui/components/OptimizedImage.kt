// În ui.components/OptimizedImage.kt
@Composable
fun OptimizedImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1A3B66)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    },
    error: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF1A3B66)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.BrokenImage,
                contentDescription = "Error",
                tint = Color.White
            )
        }
    }
) {
    val context = LocalContext.current

    // Folosește un ImageRequest optimizat
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    Box(modifier = modifier) {
        // Folosește AsyncImage cu gestionare optimizată a stărilor
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize(),
            onLoading = { placeholder() },
            onError = { error() }
        )
    }
}