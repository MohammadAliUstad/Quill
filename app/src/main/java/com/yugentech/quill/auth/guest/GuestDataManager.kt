package com.yugentech.quill.auth.guest

import com.yugentech.quill.cloud.repository.CloudSyncRepository
import com.yugentech.quill.database.dao.BookDao
import com.yugentech.quill.database.dao.UserDao
import com.yugentech.quill.database.model.BookSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Removes a guest's data. A guest's library lives only on this phone (nothing goes to
 * Firestore), so leaving guest mode clears it here.
 */
class GuestDataManager(
    private val bookDao: BookDao,
    private val userDao: UserDao,
    private val cloudSyncRepository: CloudSyncRepository
) {

    /**
     * Removes the guest's books (their highlights and reading sessions go with them), categories
     * and profile from this phone, and the files of books they imported themselves. Books
     * downloaded from a source keep their files, as after signing out: they're the public
     * catalogue's, and the next account reuses them rather than downloading them again.
     */
    suspend fun deleteLocalData(guestId: String) = withContext(Dispatchers.IO) {
        bookDao.getAllBooksOnce()
            .filter { it.source == BookSource.USER_IMPORTED }
            .forEach { book -> book.localFilePath?.let { runCatching { File(it).delete() } } }
        cloudSyncRepository.wipeLocalData()
        userDao.deleteUser(guestId)
        Timber.i("Guest local data deleted")
    }
}
