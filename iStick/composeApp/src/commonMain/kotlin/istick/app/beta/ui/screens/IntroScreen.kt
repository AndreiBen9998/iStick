// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ui/screens/IntroScreen.kt
package istick.app.beta.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import istick.app.beta.ui.components.IStickLogo
import istick.app.beta.utils.PerformanceMonitor
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageContent: @Composable () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IntroScreen(
    onFinishIntro: () -> Unit,
    performanceMonitor: PerformanceMonitor,
    modifier: Modifier = Modifier
) {
    // Start performance trace
    LaunchedEffect(Unit) {
        performanceMonitor.startTrace("intro_screen")
    }

    val pages = listOf(
        OnboardingPage(
            title = "Bine ați venit la iStick",
            description = "Locul unde iti transformi masina intr-o sursa de venit",
            imageContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A3B66)),
                    contentAlignment = Alignment.Center
                ) {
                    IStickLogo(size = 180)
                }
            }
        ),
        OnboardingPage(
            title = "Alegeți rolul potrivit",
            description = "Sunteți un proprietar de mașină care dorește să câștige bani sau un brand care dorește să-și promoveze produsele?",
            imageContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A3B66)),
                    contentAlignment = Alignment.Center
                ) {
                    // Placeholder for image showing both user types
                    Text(
                        text = "Utilizator sau Brand",
                        color = Color.White,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        ),
        OnboardingPage(
            title = "Câștigați în timp ce conduceți",
            description = "Primiți plăți lunare pentru a afișa reclame pe mașina dvs.",
            imageContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A3B66)),
                    contentAlignment = Alignment.Center
                ) {
                    // Placeholder for earning/driving image
                    Text(
                        text = "Bani + Condus",
                        color = Color.White,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        ),
        OnboardingPage(
            title = "Promovați-vă brandul",
            description = "Creați campanii publicitare și alegeți mașinile care vă vor reprezenta marca în oraș, cu targhetare geografică și demografică.",
            imageContent = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A3B66)),
                    contentAlignment = Alignment.Center
                ) {
                    // Placeholder for brand promotion image
                    Text(
                        text = "Promovare Brand",
                        color = Color.White,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F2030))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section with skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onFinishIntro,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF2962FF)
                    )
                ) {
                    Text(
                        text = "Skip",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Pager for intro screens
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                IntroPageContent(
                    page = pages[page],
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pager indicators
            Row(
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val size by animateDpAsState(
                        targetValue = if (isSelected) 12.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "indicator_size"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(size)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF2962FF) else Color.Gray.copy(alpha = 0.5f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Next/Login button
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinishIntro()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF2962FF),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.size - 1) "Continuați" else "Începeți",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (pagerState.currentPage < pages.size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null
                    )
                }
            }
        }
    }

    // Stop trace when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopTrace("intro_screen")
        }
    }
}

@Composable
fun IntroPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Image content
        page.imageContent()

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = page.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}