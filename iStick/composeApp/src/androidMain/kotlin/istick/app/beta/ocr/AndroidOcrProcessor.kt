// File: iStick/composeApp/src/androidMain/kotlin/istick/app/beta/ocr/AndroidOcrProcessor.kt
package istick.app.beta.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android implementation of OCR Processor using ML Kit
 */
class AndroidOcrProcessor(
    private val context: Context
) : OcrProcessor {
    // ML Kit text recognizer
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    override suspend fun extractMileage(imageBytes: ByteArray): Result<Int> = withContext(Dispatchers.Default) {
        try {
            // 1. Process image
            val bitmap = processBitmap(imageBytes)
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            
            // 2. Recognize text
            val text = recognizeText(inputImage)
            
            // 3. Extract mileage value
            val mileage = extractMileageFromText(text)
                ?: return@withContext Result.failure(OcrException.NoNumbersFound)
            
            // 4. Validate mileage value
            if (mileage < 1 || mileage > 1_000_000) {
                return@withContext Result.failure(OcrException.UnreasonableValue(mileage))
            }
            
            Result.success(mileage)
        } catch (e: Exception) {
            when (e) {
                is OcrException -> Result.failure(e)
                else -> Result.failure(OcrException.ProcessingError(e.message ?: "Unknown error"))
            }
        }
    }
    
    override fun extractMileageWithProgress(imageBytes: ByteArray): Flow<OcrProgress> = flow {
        try {
            // Start processing
            emit(OcrProgress.Processing(10))
            
            // Process image
            val bitmap = processBitmap(imageBytes)
            emit(OcrProgress.Processing(30))
            
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            emit(OcrProgress.Processing(50))
            
            // Recognize text
            val text = recognizeText(inputImage)
            emit(OcrProgress.Processing(80))
            
            // Extract mileage value
            val mileage = extractMileageFromText(text)
                ?: throw OcrException.NoNumbersFound
                
            // Validate mileage value
            if (mileage < 1 || mileage > 1_000_000) {
                throw OcrException.UnreasonableValue(mileage)
            }
            
            // Success!
            emit(OcrProgress.Processing(100))
            emit(OcrProgress.Success(mileage))
        } catch (e: Exception) {
            val message = when (e) {
                is OcrException -> e.message
                else -> "Error processing image: ${e.message}"
            }
            emit(OcrProgress.Error(message ?: "Unknown error"))
        }
    }
    
    /**
     * Process the bitmap - resize, rotate, and enhance for OCR
     */
    private suspend fun processBitmap(imageBytes: ByteArray): Bitmap = withContext(Dispatchers.IO) {
        // Decode bitmap
        val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        
        // Check rotation from EXIF data
        val rotation = try {
            val exif = ExifInterface(ByteArrayInputStream(imageBytes))
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: Exception) {
            0f
        }
        
        // Apply rotation if needed
        val rotatedBitmap = if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(
                originalBitmap, 0, 0,
                originalBitmap.width, originalBitmap.height,
                matrix, true
            )
        } else {
            originalBitmap
        }
        
        // Resize if too large for memory
        val maxDimension = 1280
        val scaledBitmap = if (rotatedBitmap.width > maxDimension || rotatedBitmap.height > maxDimension) {
            val scale = minOf(
                maxDimension.toFloat() / rotatedBitmap.width,
                maxDimension.toFloat() / rotatedBitmap.height
            )
            
            val matrix = Matrix().apply { postScale(scale, scale) }
            Bitmap.createBitmap(
                rotatedBitmap, 0, 0,
                rotatedBitmap.width, rotatedBitmap.height,
                matrix, true
            )
        } else {
            rotatedBitmap
        }
        
        // We could add more image processing here for better OCR results
        // - Contrast enhancement
        // - Binarization
        // - Noise reduction
        // But for this example, we'll return the scaled bitmap as is
        
        scaledBitmap
    }
    
    /**
     * Recognize text in an image using ML Kit
     */
    private suspend fun recognizeText(image: InputImage): Text = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(image)
            .addOnSuccessListener { text ->
                continuation.resume(text)
            }
            .addOnFailureListener { e ->
                continuation.resumeWithException(
                    OcrException.ProcessingError("Text recognition failed: ${e.message}")
                )
            }
            
        continuation.invokeOnCancellation {
            // No need to cancel ML Kit operations as they're managed by the framework
        }
    }
    
    /**
     * Extract mileage from recognized text
     * This method contains the intelligence to identify which number is the mileage
     */
    private fun extractMileageFromText(text: Text): Int? {
        // If no text was found
        if (text.text.isEmpty()) {
            throw OcrException.NoTextFound
        }
        
        // Extract all numbers from the text
        val numberPattern = Regex("\\d+")
        val allNumbers = numberPattern.findAll(text.text)
            .map { it.value.toInt() }
            .toList()
            
        if (allNumbers.isEmpty()) {
            throw OcrException.NoNumbersFound
        }
        
        // Analyze the text blocks to find the most likely mileage value
        // This is a heuristic approach that could be improved with machine learning
        
        // 1. Look for "km", "miles", "mi", "odometer" nearby numbers
        val mileageKeywords = listOf("km", "kilometer", "mile", "mi", "odometer", "odo", "mileage")
        
        val blocks = text.textBlocks
        for (block in blocks) {
            val blockText = block.text.lowercase()
            
            // Check if this block contains mileage keywords
            val containsKeyword = mileageKeywords.any { blockText.contains(it) }
            
            if (containsKeyword) {
                // Extract numbers from this block
                val numbers = numberPattern.findAll(block.text)
                    .map { it.value.toInt() }
                    .toList()
                    
                if (numbers.isNotEmpty()) {
                    // If multiple numbers, take the largest as it's likely the total mileage
                    return numbers.maxOrNull()
                }
            }
        }
        
        // 2. If no keywords found, look for numbers that match typical mileage patterns
        
        // Typical mileage is 5-7 digits
        val typicalMileageNumbers = allNumbers.filter { it.toString().length in 5..7 }
        if (typicalMileageNumbers.isNotEmpty()) {
            return typicalMileageNumbers.maxOrNull() // Take the highest value
        }
        
        // 3. If multiple numbers and we can't determine which is mileage, take the largest
        // This is a fallback and may not be accurate
        if (allNumbers.size > 3) {
            // Too many numbers, might be misleading
            throw OcrException.AmbiguousResult
        }
        
        return allNumbers.maxOrNull()
    }
}

/**
 * Create Android OCR processor
 */
actual fun createOcrProcessor(): OcrProcessor {
    // This is just a stub - in real implementation, we need context
    throw IllegalStateException("Context must be provided for Android OcrProcessor")
}

/**
 * Create OcrProcessor with Android context
 */
fun createOcrProcessor(context: Context): OcrProcessor {
    return AndroidOcrProcessor(context)
}