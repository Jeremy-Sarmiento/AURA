package com.example.aura.data.storage

/**
 * ARCHIVO: LocalFileManager.kt
 * FUNCIÓN: Acceso al almacenamiento local mediante SAF: escaneo recursivo de las carpetas que el
 *          usuario autoriza, lectura de metadatos de URIs y gestión de carpetas de música.
 * NAVEGACIÓN: AuraViewModel -> LocalFileManager; la UI abre el flujo desde FileManagerSheet.
 *
 * La exploración de árbol está basada en `SafUtils.listTree` de Namida
 * (https://github.com/namidaco/namida): cola BFS, sin recursión y sin repetir directorios.
 */

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import com.example.aura.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File

// Sin permiso persistente, la carpeta importada deja de leerse tras reiniciar la app.
fun Context.takePersistableReadPermission(uri: Uri) {
    try {
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // Algunos proveedores no conceden permisos persistentes; la carpeta funciona solo en esta sesión.
    }
}

// Libera el permiso persistente cuando el usuario quita una carpeta de la biblioteca.
fun Context.releasePersistableReadPermission(uri: Uri) {
    try {
        contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: Exception) {
        // El proveedor pudo revocar el permiso antes; no hay nada más que liberar.
    }
}

/**
 * MusicFolder: carpeta autorizada por el usuario. El URI es un tree URI SAF y se conserva en
 * SharedPreferences para que la selección sobreviva a las actualizaciones de la app.
 */
data class MusicFolder(
    val uri: Uri,
    val displayName: String,
    val pathLabel: String,
    val hasReadPermission: Boolean = true
) {
    val uriString: String get() = uri.toString()

    // Prefijo con separador para no confundir "Música" con "Música2".
    val songUriPrefix: String get() = "$uriString/"
}

/**
 * FolderScanResult: canciones leídas y todos los URIs de audio encontrados. Los URIs permiten
 * distinguir un archivo borrado de uno que existe pero cuyos metadatos fallaron temporalmente.
 */
data class FolderScanResult(
    val songs: List<Song>,
    val discoveredUriStrings: Set<String>
)

class LocalFileManager(private val context: Context) {

    companion object {
        // Duración mínima de 1 minuto (60,000 ms) para ignorar notas de voz, tonos y sonidos cortos
        const val MIN_SONG_DURATION_MS = 60_000L

        private val AUDIO_EXTENSIONS = setOf(
            "flac", "wav", "alac", "aiff", "aif", "mp3", "m4a", "m4b", "aac", "ogg", "opus", "wma", "ape"
        )
    }

    /**
     * describeFolder: obtiene el nombre visible y la ruta legible de una carpeta SAF.
     */
    fun describeFolder(treeUri: Uri): MusicFolder {
        return MusicFolder(
            uri = treeUri,
            displayName = getFolderDisplayName(treeUri),
            pathLabel = getFolderPathLabel(treeUri),
            hasReadPermission = hasPersistedReadPermission(treeUri)
        )
    }

    // Comprueba que la app conserve el permiso de lectura persistente para la carpeta.
    fun hasPersistedReadPermission(treeUri: Uri): Boolean {
        return try {
            context.contentResolver.persistedUriPermissions.any { permission ->
                permission.isReadPermission && permission.uri == treeUri
            }
        } catch (_: Exception) {
            false
        }
    }

    // Nombre visible de la carpeta raíz del árbol SAF (por ejemplo "Música").
    fun getFolderDisplayName(treeUri: Uri): String {
        val fallback = try {
            DocumentsContract.getTreeDocumentId(treeUri)
                .substringAfterLast(':')
                .substringAfterLast('/')
                .takeIf { it.isNotBlank() }
                ?: "Carpeta de música"
        } catch (_: Exception) {
            "Carpeta de música"
        }

        return try {
            val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
            context.contentResolver.query(
                documentUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }?.takeIf { it.isNotBlank() } ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    // Ruta legible para mostrar debajo del nombre de la carpeta.
    fun getFolderPathLabel(treeUri: Uri): String {
        return try {
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val volumeId = documentId.substringBefore(':', missingDelimiterValue = documentId)
            val relativePath = documentId.substringAfter(':', missingDelimiterValue = "").trim('/')
            val volumeLabel = if (volumeId.equals("primary", ignoreCase = true)) {
                "Almacenamiento interno"
            } else {
                volumeId
            }
            listOf(volumeLabel, relativePath)
                .filter { it.isNotBlank() }
                .joinToString("/")
        } catch (_: Exception) {
            treeUri.toString()
        }
    }

    /**
     * parseSongFromUri: lee una URI concreta (selector SAF o explorador) con MediaMetadataRetriever,
     * comprueba que dure más de 1 minuto y descarta notas de voz o tonos.
     */
    suspend fun parseSongFromUri(uri: Uri): Song? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?: "Audio Sin Título"
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: "Artista Desconocido"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?: "Álbum Local"
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L
            val titleLower = title.lowercase()
            val uriLower = uri.toString().lowercase()

            val isWhatsApp = uriLower.contains("whatsapp") || titleLower.startsWith("ptt-") || titleLower.startsWith("aud-")
            val isRingtoneOrNotif = uriLower.contains("ringtones") || uriLower.contains("notifications") || uriLower.contains("alarms")

            // Filtrar audios de menos de 1 minuto, WhatsApp o notificaciones
            if (durationMs < MIN_SONG_DURATION_MS || isWhatsApp || isRingtoneOrNotif) {
                return@withContext null
            }

            val fileName = uri.lastPathSegment ?: ""
            val format = when {
                fileName.endsWith(".flac", ignoreCase = true) -> "FLAC"
                fileName.endsWith(".wav", ignoreCase = true) -> "WAV"
                fileName.endsWith(".alac", ignoreCase = true) -> "ALAC"
                fileName.endsWith(".aiff", ignoreCase = true) || fileName.endsWith(".aif", ignoreCase = true) -> "AIFF"
                fileName.endsWith(".m4a", ignoreCase = true) || fileName.endsWith(".m4b", ignoreCase = true) -> "M4A"
                fileName.endsWith(".ogg", ignoreCase = true) -> "OGG"
                fileName.endsWith(".opus", ignoreCase = true) -> "OPUS"
                fileName.endsWith(".aac", ignoreCase = true) -> "AAC"
                fileName.endsWith(".wma", ignoreCase = true) -> "WMA"
                fileName.endsWith(".ape", ignoreCase = true) -> "APE"
                else -> "MP3"
            }
            val isLossless = isFormatLossless(format)
            val specs = extractAudioSpecs(retriever, isLossless)
            val localPath = if (uri.scheme == "file") uri.path else null

            Song(
                // Id determinista: reimportar el mismo archivo no crea duplicados.
                id = "saf_$uri",
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                uriString = uri.toString(),
                filePath = localPath,
                format = format,
                sampleRate = specs.sampleRate,
                bitDepth = specs.bitDepth,
                bitrateKbps = specs.bitrateKbps,
                isLossless = isLossless,
                lyrics = readSiblingLyrics(localPath),
                coverColorHex = getPaletteColor(title.hashCode())
            )
        } catch (e: Exception) {
            Log.e("LocalFileManager", "Error parsing uri: $uri", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun extractAudioSpecs(
        retriever: MediaMetadataRetriever,
        isLossless: Boolean
    ): AudioSpecs {
        val sampleRateStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
            } catch (_: Exception) { null }
        } else null

        val bitrateStr = try {
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        } catch (_: Exception) { null }

        val sampleRate = sampleRateStr?.toIntOrNull() ?: 44100
        val rawBitrate = bitrateStr?.toIntOrNull()
        val bitrateKbps = if (rawBitrate != null && rawBitrate > 0) rawBitrate / 1000 else if (isLossless) 1411 else 320
        val bitDepth = if (sampleRate >= 88200) 24 else 16

        return AudioSpecs(
            sampleRate = sampleRate,
            bitDepth = bitDepth,
            bitrateKbps = bitrateKbps
        )
    }

    private data class AudioSpecs(
        val sampleRate: Int,
        val bitDepth: Int,
        val bitrateKbps: Int
    )

    /**
     * scanFolderFromTree: porta `listTree` de Namida. Recorre todo el árbol SAF con una cola BFS,
     * incluidas las subcarpetas, y devuelve solo pistas de más de 1 minuto.
     */
    suspend fun scanFolderFromTree(treeUri: Uri): FolderScanResult = withContext(Dispatchers.IO) {
        val audioUris = listAudioDocumentUris(treeUri)
        val songs = mutableListOf<Song>()

        // Namida procesa los metadatos en lotes; aquí se hace de 4 en 4 para no saturar el proveedor.
        audioUris.chunked(4).forEach { batch ->
            val parsed = coroutineScope {
                batch.map { uri -> async { parseSongFromUri(uri) } }.awaitAll()
            }
            songs += parsed.filterNotNull()
        }

        FolderScanResult(
            songs = songs,
            discoveredUriStrings = audioUris.mapTo(mutableSetOf()) { it.toString() }
        )
    }

    /**
     * listAudioDocumentUris: recorre el árbol sin recursión para evitar desbordes de pila.
     * Si la carpeta raíz falla (por ejemplo, permiso revocado), lanza la excepción para que el
     * ViewModel no interprete el fallo como "carpeta vacía" y borre canciones válidas.
     */
    private fun listAudioDocumentUris(treeUri: Uri): List<Uri> {
        val audioUris = mutableListOf<Uri>()
        val pending = ArrayDeque<Pair<String, String>>()
        val visited = mutableSetOf<String>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        pending.add(DocumentsContract.getTreeDocumentId(treeUri) to "")
        var isRoot = true

        while (pending.isNotEmpty()) {
            val (parentId, parentPath) = pending.removeFirst()
            if (!visited.add(parentId)) continue

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            val cursor = try {
                context.contentResolver.query(childrenUri, projection, null, null, null)
            } catch (e: Exception) {
                if (isRoot) throw e
                Log.w("LocalFileManager", "No se pudo leer la subcarpeta $parentPath", e)
                null
            }
            if (isRoot && cursor == null) {
                isRoot = false
                throw IllegalStateException("No se pudo abrir la carpeta: $treeUri")
            }
            isRoot = false

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = c.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (c.moveToNext()) {
                    val docId = c.getString(idCol) ?: continue
                    val name = c.getString(nameCol) ?: ""
                    val mime = c.getString(mimeCol) ?: ""

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val childPath = if (parentPath.isEmpty()) name else "$parentPath/$name"
                        pending.add(docId to childPath)
                        continue
                    }

                    if (isAudioDocument(name, mime)) {
                        audioUris.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, docId))
                    }
                }
            }
        }

        return audioUris
    }

    // Acepta tanto el MIME del proveedor como la extensión, igual que hace Namida con su lista.
    private fun isAudioDocument(name: String, mime: String): Boolean {
        if (mime.startsWith("audio/", ignoreCase = true)) return true
        val extension = name.substringAfterLast('.', "").lowercase()
        return extension in AUDIO_EXTENSIONS
    }

    private fun readSiblingLyrics(audioPath: String?): String {
        if (audioPath.isNullOrEmpty()) return ""
        return try {
            val audio = File(audioPath)
            val dir = audio.parentFile ?: return ""
            val base = audio.nameWithoutExtension
            listOf(File(dir, "$base.lrc"), File(dir, "$base.LRC"))
                .firstOrNull { it.isFile && it.canRead() && it.length() in 1..(512 * 1024) }
                ?.readText()
                ?.takeIf { it.isNotBlank() }
                ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    private fun isFormatLossless(format: String): Boolean {
        return format.uppercase() in listOf("FLAC", "WAV", "ALAC", "AIFF", "APE")
    }

    private fun getPaletteColor(seed: Int): Long {
        val palette = listOf(
            0xFF9155FD, 0xFF5B78EE, 0xFF2EC4B6, 0xFFE85D9E, 0xFF8B7DE8, 0xFFE65D79
        )
        val index = kotlin.math.abs(seed) % palette.size
        return palette[index]
    }
}
