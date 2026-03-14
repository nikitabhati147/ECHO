package echo.shared

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object EnvFile {
    private val values: Map<String, String> by lazy { load(Paths.get(".env")) }

    fun get(key: String): String? = System.getenv(key) ?: values[key]

    private fun load(path: Path): Map<String, String> {
        if (!Files.exists(path)) {
            return emptyMap()
        }

        return Files.readAllLines(path)
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val separatorIndex = line.indexOf('=')
                if (separatorIndex <= 0) {
                    return@mapNotNull null
                }

                val key = line.substring(0, separatorIndex).trim()
                val value = line.substring(separatorIndex + 1).trim().trim('"', '\'')
                key to value
            }
            .toMap()
    }
}
