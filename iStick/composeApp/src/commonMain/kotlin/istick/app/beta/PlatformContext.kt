package istick.app.beta

import androidx.compose.runtime.Composable

/**
 * Retrieves the platform-specific context outside of Composable functions
 * @return Platform-specific context object
 */
expect fun getPlatformContext(): Any?

/**
 * Composable function to get platform context
 */
@Composable
expect fun getPlatformContextComposable(): Any?