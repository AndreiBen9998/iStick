// File: iStick/composeApp/src/commonMain/kotlin/istick/app/beta/storage/StorageExtensions.kt
package istick.app.beta.storage

/**
 * Compress an image with the specified quality
 */
expect fun compressImage(imageBytes: ByteArray, quality: Int): ByteArray