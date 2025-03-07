package istick.app.beta


// Kotlin and Coroutines
import kotlinx.coroutines.launch

// Compose Runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue

// Compose Foundation
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions

// Compose Material
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar

// Material Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning

// Compose UI
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Compose Animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

// Jetbrains Compose Preview
import org.jetbrains.compose.ui.tooling.preview.Preview

// App-specific imports
import istick.app.beta.auth.FirebaseAuthRepository
import istick.app.beta.storage.FirebaseStorageRepository
import istick.app.beta.camera.rememberCameraLauncher
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector




// Kotlin standard library
import kotlin.math.abs

// Define the CarouselItem class for type safety
data class CarouselItem(val title: String, val subtitle: String = "")

val optimizedOffersRepository = remember { OptimizedOffersRepository() }
val homeViewModel = remember { HomeViewModel(optimizedOffersRepository) }

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    performanceMonitor: PerformanceMonitor,
    modifier: Modifier = Modifier
) {
    // Începe măsurarea performanței pentru întreg ecranul
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("login_screen_render")
    }

    // Optimization: Memoizăm repository-ul pentru a evita reconstrucția
    val authRepository = remember { FirebaseAuthRepository() }
    val coroutineScope = rememberCoroutineScope()

    // State-urile pentru diferite părți ale UI-ului
    var showLoginForm by remember { mutableStateOf(false) }
    var currentPage by remember { mutableStateOf(0) }

    // Optimization: Memoizăm lista pentru a preveni recrearea acesteia la fiecare recompunere
    val welcomeMessages = remember {
        listOf(
            CarouselItem("Singurul loc in care iti transformi masina intr-o sursa de venit!", "Investeste in promovarea masinii tale"),
            CarouselItem("Nu ai bani si vrei sa faci promovare", "Ai gasit locul perfect!"),
            CarouselItem("Ai bani si vrei promovare", "Ai gasit locul perfect!")
        )
    }

    // State pentru formularul de login
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Măsoară timpul de randare pentru container-ul principal
            .measureRenderTime("login_container", performanceMonitor)
    ) {
        // Background - deep navy blue
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A1929))
        )

        // Optimization: Folosim AnimatedVisibility pentru tranziții fluide între ecrane
        AnimatedVisibility(
            visible = !showLoginForm,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            // Ecranul de onboarding
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Măsoară timpul de randare pentru onboarding
                    .measureRenderTime("onboarding_screen", performanceMonitor),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar optimizat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "iStick",
                            style = MaterialTheme.typography.h5,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2962FF)
                        )
                    }

                    // Menu
                    IconButton(onClick = { /* Menu options */ }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
                    }
                }

                // Optimization: Carousel Netflix-style cu lazy rendering
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Vom actualiza performanceMonitor-ul cu durata vizualizării fiecărei pagini
                    LaunchedEffect(currentPage) {
                        performanceMonitor.recordMetric("carousel_page_view_$currentPage", 1)
                    }

                    CarouselContent(
                        currentPage = currentPage,
                        onPageChange = {
                            currentPage = it
                        },
                        items = welcomeMessages,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Register button cu animații fluide
                Button(
                    onClick = {
                        performanceMonitor.recordMetric("register_button_click", 1)
                        showLoginForm = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF2962FF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("Înregistrează-te")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign in text
                Text(
                    text = "Ai deja un cont? Autentifică-te",
                    color = Color.White,
                    modifier = Modifier
                        .clickable {
                            performanceMonitor.recordMetric("signin_text_click", 1)
                            showLoginForm = true
                        }
                        .padding(bottom = 24.dp)
                )
            }
        }

        // Optimization: Folosim AnimatedVisibility pentru tranziții fluide între ecrane
        AnimatedVisibility(
            visible = showLoginForm,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            // Login form
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    // Măsoară timpul de randare pentru formularul de login
                    .measureRenderTime("login_form", performanceMonitor),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Back button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        performanceMonitor.recordMetric("back_button_click", 1)
                        showLoginForm = false
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        "Autentificare",
                        style = MaterialTheme.typography.h6,
                        color = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Error message cu animații fluide
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = Color.Red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    }
                }

                // Email field optimizat
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email sau număr de telefon") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White,
                        leadingIconColor = Color.White,
                        trailingIconColor = Color.White,
                        focusedBorderColor = Color(0xFF2962FF),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = Color(0xFF2962FF),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                // Password field optimizat
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Parolă") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.White,
                        cursorColor = Color.White,
                        leadingIconColor = Color.White,
                        trailingIconColor = Color.White,
                        focusedBorderColor = Color(0xFF2962FF),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedLabelColor = Color(0xFF2962FF),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Optimization: Login button cu feedback vizual îmbunătățit
                Button(
                    onClick = {
                        performanceMonitor.startTrace("login_attempt")

                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null

                            val result = authRepository.signIn(email, password)

                            result.fold(
                                onSuccess = {
                                    performanceMonitor.recordMetric("login_success", 1)
                                    onLoginSuccess()
                                },
                                onFailure = {
                                    performanceMonitor.recordMetric("login_failure", 1)
                                    errorMessage = it.message ?: "Eroare de autentificare"
                                }
                            )

                            isLoading = false
                            performanceMonitor.stopTrace("login_attempt")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = email.isNotBlank() && password.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF2962FF),
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Autentificare")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Need help
                Text(
                    text = "Ai nevoie de ajutor?",
                    color = Color.Gray,
                    modifier = Modifier
                        .padding(8.dp)
                        .clickable {
                            performanceMonitor.recordMetric("help_text_click", 1)
                            /* Help action */
                        }
                )

                Spacer(modifier = Modifier.weight(1f))

                // Sign up prompt
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Nu ai încă un cont iStick? ",
                        color = Color.Gray
                    )
                    Text(
                        "Înregistrează-te acum",
                        color = Color(0xFF2962FF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            performanceMonitor.recordMetric("register_prompt_click", 1)
                            showLoginForm = false
                        }
                    )
                }
            }
        }
    }

    // Optimization: Oprește urmărirea performanței când compunerea dispare
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("login_screen_render")
        }
    }
}

// Carousel content with navigation
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarouselContent(
    currentPage: Int,
    onPageChange: (Int) -> Unit,
    items: List<CarouselItem>,
    modifier: Modifier = Modifier
) {
    // Folosim PagerState pentru a gestiona swipe-ul și animațiile
    val pagerState = rememberPagerState(initialPage = currentPage) { items.size }

    // Sincronizăm pagerState cu currentPage (controller extern)
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage) {
            pagerState.animateScrollToPage(currentPage)
        }
    }

    // Sincronizăm currentPage cu pagerState (când utilizatorul face swipe)
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentPage) {
            onPageChange(pagerState.currentPage)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // HorizontalPager pentru swipe fluid între pagini
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // Afișăm pagina corespunzătoare indexului
            CarouselPage(
                title = items[page].title,
                subtitle = items[page].subtitle,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Indicator pagini (bullet points)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(items.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) Color.White
                            else Color.Gray.copy(alpha = 0.5f)
                        )
                        .clickable {
                            onPageChange(index)
                        }
                )
            }
        }
    }
}

// Helper component for carousel pages
@Composable
private fun CarouselPage(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Center illustration/image
        Box(
            modifier = Modifier
                .size(280.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A3B66))  // Lighter navy blue
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Simplified illustration
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Heading
        Text(
            text = title,
            style = MaterialTheme.typography.h4,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subheading
        Text(
            text = subtitle,
            style = MaterialTheme.typography.subtitle1,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

// Ecranul principal după autentificare
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel,
    performanceMonitor: PerformanceMonitor,
    modifier: Modifier = Modifier
) {
    val authRepository = remember { FirebaseAuthRepository() }
    val storageRepository = remember { FirebaseStorageRepository() }
    val coroutineScope = rememberCoroutineScope()

    // State pentru a urmări tab-ul selectat
    var selectedTab by remember { mutableStateOf(0) }

    // State pentru fotografii și încărcare
    var isUploading by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }

    // Funcția pentru a încărca fotografia după ce a fost capturată
    val uploadPhoto: (ByteArray) -> Unit = { imageBytes ->
        coroutineScope.launch {
            isUploading = true
            uploadMessage = null

            try {
                val userId = authRepository.getCurrentUserId() ?: ""
                val fileName = "photo_${System.currentTimeMillis()}.jpg"

                storageRepository.uploadImage(imageBytes, "$userId/$fileName").fold(
                    onSuccess = { url ->
                        photos = photos + url
                        uploadMessage = "Fotografie încărcată cu succes!"
                    },
                    onFailure = { e ->
                        uploadMessage = "Eroare la încărcarea fotografiei: ${e.message}"
                    }
                )
            } catch (e: Exception) {
                uploadMessage = "Eroare la încărcarea fotografiei: ${e.message}"
            } finally {
                isUploading = false
            }
        }
    }

    // Obținem launcher-ul pentru cameră
    val launchCamera = rememberCameraLauncher(onPhotoTaken = uploadPhoto)

    // Încărcăm fotografiile existente când ecranul este afișat
    LaunchedEffect(Unit) {
        try {
            val userId = authRepository.getCurrentUserId() ?: ""
            storageRepository.getUserImages(userId).fold(
                onSuccess = { urls -> photos = urls },
                onFailure = { /* Gestionează eroarea */ }
            )
        } catch (e: Exception) {
            // Gestionează eroarea
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(selectedTab) {
                            0 -> "Acasă"
                            1 -> "Harta"
                            2 -> "Fotografii"
                            3 -> "Profil"
                            else -> "iStick"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            authRepository.signOut()
                            onLogout()
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Deconectare")
                    }
                },
                backgroundColor = Color(0xFF0A1929), // Culoare Riot-like
                contentColor = Color.White
            )
        },
        bottomBar = {
            // Bara de navigare în stil Riot Games
            BottomNavigation(
                backgroundColor = Color(0xFF0A1929),
                contentColor = Color.White
            ) {
                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Acasă") },
                    label = { Text("Acasă") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    selectedContentColor = Color(0xFF2962FF),
                    unselectedContentColor = Color.Gray
                )

                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Place, contentDescription = "Harta") },
                    label = { Text("Harta") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    selectedContentColor = Color(0xFF2962FF),
                    unselectedContentColor = Color.Gray
                )

                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Add, contentDescription = "Fotografii") },
                    label = { Text("Fotografii") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    selectedContentColor = Color(0xFF2962FF),
                    unselectedContentColor = Color.Gray
                )

                BottomNavigationItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
                    label = { Text("Profil") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    selectedContentColor = Color(0xFF2962FF),
                    unselectedContentColor = Color.Gray
                )
            }
        },
        floatingActionButton = {
            // Arată FloatingActionButton doar în ecranul de fotografii
            if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = {
                        if (!isUploading) {
                            launchCamera()
                        }
                    },
                    backgroundColor = Color(0xFF2962FF)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Fă o poză", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        // Conținut bazat pe tab-ul selectat
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F2030)) // Fundal închis
        ) {
            when (selectedTab) {
                0 -> HomeScreen(homeViewModel, performanceMonitor)
                1 -> MapScreen()
                2 -> PhotosScreen(photos, isUploading, uploadMessage)
                3 -> ProfileScreen(onLogout)
                else -> HomeScreen()
            }
        }
    }
}

// Ecranul Home
@Composable
private fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bine ai venit la iStick!",
            style = MaterialTheme.typography.h5,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Aplicația ta de promovare auto",
            style = MaterialTheme.typography.subtitle1,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// Ecranul Map
@Composable
private fun MapScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Place,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF2962FF)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hartă Promoții",
            style = MaterialTheme.typography.h5,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Aici vei putea vedea mașinile disponibile pentru promovare în zona ta.",
            style = MaterialTheme.typography.body1,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

// Ecranul de fotografii (similar cu MainScreen-ul original)
@Composable
private fun PhotosScreen(
    photos: List<String>,
    isUploading: Boolean,
    uploadMessage: String?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Fotografiile tale",
            style = MaterialTheme.typography.h5,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        if (photos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nu ai încă fotografii încărcate. Apasă + pentru a face o poză.",
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                "Ai ${photos.size} fotografii încărcate",
                color = Color.White.copy(alpha = 0.7f)
            )

            // Afișare fotografii
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            ) {
                photos.forEach { photoUrl ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = 4.dp,
                        backgroundColor = Color(0xFF1A3B66)
                    ) {
                        Text(
                            text = "Foto: $photoUrl",
                            modifier = Modifier.padding(8.dp),
                            maxLines = 2,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (isUploading) {
            CircularProgressIndicator(color = Color(0xFF2962FF))
            Text(
                "Se încarcă fotografia...",
                color = Color.White
            )
        }

        uploadMessage?.let {
            Text(
                text = it,
                color = if (it.startsWith("Eroare")) Color.Red else Color(0xFF2962FF),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// Ecranul de profil
@Composable
private fun ProfileScreen(onLogout: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar utilizator
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A3B66)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Profil Utilizator",
            style = MaterialTheme.typography.h5,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Opțiuni profil
        ProfileOption(
            icon = Icons.Default.Person,
            title = "Informații Personale",
            onClick = { /* Acțiune */ }
        )

        ProfileOption(
            icon = Icons.Default.Settings,
            title = "Setări Cont",
            onClick = { /* Acțiune */ }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Buton deconectare
        Button(
            onClick = { onLogout() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color(0xFF2962FF),
                contentColor = Color.White
            )
        ) {
            Icon(
                Icons.Default.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Deconectare")
        }
    }
}

// Component reutilizabil pentru opțiunile de profil
@Composable
private fun ProfileOption(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = 4.dp,
        backgroundColor = Color(0xFF1A3B66)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Funcția principală a aplicației
@Composable
@Preview
fun App() {
    // Inițializează PerformanceMonitor
    val context = LocalContext.current
    val performanceMonitor = remember { PerformanceMonitor(context) }

    // Măsoară timpul de pornire al aplicației
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("app_startup")
    }

    // Inițializează repository-urile optimizate și viewModel-urile
    val optimizedOffersRepository = remember { OptimizedOffersRepository() }
    val homeViewModel = remember { HomeViewModel(optimizedOffersRepository) }

    var isLoggedIn by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background
        ) {
            if (isLoggedIn) {
                MainScreen(
                    onLogout = {
                        isLoggedIn = false
                    },
                    homeViewModel = homeViewModel,
                    performanceMonitor = performanceMonitor
                )
            } else {
                LoginScreen(
                    onLoginSuccess = {
                        isLoggedIn = true
                    },
                    performanceMonitor = performanceMonitor
                )
            }
        }
    }

    // Oprește măsurarea la ieșire
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("app_startup")
            performanceMonitor.monitorMemory() // Check memory usage
        }
    }
}