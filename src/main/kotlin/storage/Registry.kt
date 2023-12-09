package storage

import UserData
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object Registry {
    private val databasePath: String by lazy { System.getenv("DATABASE") }
    private val databaseFile: File by lazy { File(databasePath) }

    private val cache: MutableMap<String, UserData> = mutableMapOf()

    private val lock = ReentrantLock()

    /**
     * Updates the in-memory cache with the contents of the data file.
     */
    fun update() = lock.withLock {
        cache.clear()

        if (!databaseFile.exists()) {
            println("Tried to update in-memory cache, but database doesn't exist.")
            println("Creating empty database.")
            databaseFile.createNewFile()
        }

        println("Updating in-memory cache...")
        val lines = databaseFile.readLines(charset = Charsets.UTF_8)
            // Filter blank lines just in case
            .filter { it.isNotBlank() }

        for (line in lines) {
            val pieces = line.split('\t')
            if (pieces.size != UserData.FIELDS_COUNT + 1) continue
            val (username, phone, userId) = pieces
            cache[username] = UserData(phone, userId.toLong())
        }

        println("Loaded ${cache.size} elements from FS database into memory.")
    }

    /**
     * Persists the contents of the in-memory cache into the filesystem.
     */
    fun store() = lock.withLock {
        println("Storing in-memory cache into FS...")
        if (databaseFile.exists()) {
            databaseFile.delete()
            databaseFile.createNewFile()
        }
        val lines = cache
            .toList()
            .joinToString("\n") { (username, phone) -> "$username\t$phone" }
        databaseFile.writeText(lines)
    }

    /**
     * Checks if the [username] is currently in the cache.
     *
     * @param username The username to check for
     * @param refresh If `true`, the in-memory cache will be updated before performing the check.
     */
    fun has(username: String, refresh: Boolean = true): Boolean = lock.withLock {
        if (refresh) update()

        return cache.containsKey(username)
    }

    fun set(username: String, phone: String, userId: Long, persist: Boolean = true) = lock.withLock {
        cache[username] = UserData(phone, userId)

        if (persist) store()
    }

    fun findByPhone(phone: String): UserData? = lock.withLock {
        cache.toList().find { (_, data) -> data.phone == phone }?.second
    }
}
