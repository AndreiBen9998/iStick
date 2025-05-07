package istick.app.beta.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

object CoroutineConfig {
    val IO: CoroutineDispatcher = Dispatchers.IO
    val Default: CoroutineDispatcher = Dispatchers.Default
    val Main: CoroutineDispatcher = Dispatchers.Main
}