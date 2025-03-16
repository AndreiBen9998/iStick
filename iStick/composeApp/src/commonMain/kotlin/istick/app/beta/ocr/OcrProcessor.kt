// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/ocr/OcrProcessor.kt
package istick.app.beta.ocr

import kotlinx.coroutines.flow.Flow

/**
 * Interface for OCR processing
 */
interface OcrProcessor {
    /**
     * Process an image and extract mileage reading
     * 
     * @param imageBytes The image data
     * @return Result with extracted mileage value or error
     */
    suspend fun extractMileage(imageBytes: ByteArray): Result<Int>
    
    /**
     * Process image with progress tracking
     * 
     * @param imageBytes The image data
     * @return Flow emitting processing progress and result
     */
    fun extractMileageWithProgress(imageBytes: ByteArray): Flow<OcrProgress>
}

/**
 * Platform-specific OCR processor factory
 */
expect fun createOcrProcessor(): OcrProcessor

/**
 * OCR processing progress and result
 */
sealed class OcrProgress {
    /**
     * Processing is in progress with percent complete
     */
    data class Processing(val percent: Int) : OcrProgress()
    
    /**
     * Processing completed successfully with extracted mileage
     */
    data class Success(val mileage: Int) : OcrProgress()
    
    /**
     * Processing failed with error message
     */
    data class Error(val message: String) : OcrProgress()
}

/**
 * OCR extraction exceptions
 */
sealed class OcrException(message: String) : Exception(message) {
    /**
     * No text was found in the image
     */
    object NoTextFound : OcrException("No text found in the image")
    
    /**
     * No numbers were found in the extracted text
     */
    object NoNumbersFound : OcrException("No numbers found in the extracted text")
    
    /**
     * Multiple number sequences were found and it's unclear which is the mileage
     */
    object AmbiguousResult : OcrException("Multiple number sequences found, please take a clearer photo")
    
    /**
     * Mileage value is unreasonable (too low, too high)
     */
    class UnreasonableValue(value: Int) : OcrException("Extracted value $value appears unreasonable")
    
    /**
     * Generic OCR processing error
     */
    class ProcessingError(message: String) : OcrException(message)
}