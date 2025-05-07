package istick.app.beta.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface OcrProcessor {
    suspend fun processImage(bitmap: Bitmap): String
}

class AndroidOcrProcessor(private val context: Context) : OcrProcessor {
    private val TAG = "OcrProcessor"
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun processImage(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image)
                .addOnSuccessListener { text ->
                    continuation.resume(text.text)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error processing image", e)
                    continuation.resumeWithException(e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating input image", e)
            continuation.resumeWithException(e)
        }
    }
}

fun createOcrProcessor(context: Context): OcrProcessor {
    return AndroidOcrProcessor(context)
}