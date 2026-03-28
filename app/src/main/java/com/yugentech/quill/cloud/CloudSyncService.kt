package com.yugentech.quill.cloud

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.yugentech.quill.database.entity.BookEntity
import com.yugentech.quill.database.entity.CategoryEntity
import com.yugentech.quill.database.model.BookSource
import com.yugentech.quill.domain.AuthRepository
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class CloudSyncService(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository
) {
    // Dynamically fetches the current user's ID
    private val currentUserId: String
        get() = authRepository.currentUser ?: throw IllegalStateException("No user logged in")

    // --- LIBRARY (BOOKS) ---

    // Reference to the user's library subcollection
    private fun libraryCollection() =
        firestore.collection("users").document(currentUserId).collection("library")

    /**
     * Uploads or updates a book's metadata and reading progress in Firestore.
     */
    // Inside CloudSyncService.kt
    suspend fun syncBookToCloud(book: BookEntity): Result<Unit> {
        // GUARD: Prevent uploading user-imported books
        if (book.source == BookSource.USER_IMPORTED) {
            Timber.d("Skipping cloud sync for user-imported book: ${book.title}")
            return Result.success(Unit)
        }

        return try {
            val userId = authRepository.currentUser ?: throw Exception("User not logged in")
            Timber.d("Syncing book progress to cloud: ${book.title}")

            val data = mapOf(
                "id" to book.id,
                "title" to book.title,
                "author" to book.author,
                "coverUrl" to book.coverUrl,
                "source" to book.source.name,
                "userCategory" to book.userCategory,
                "isFavorite" to book.isFavorite,
                "downloadUrl" to book.downloadUrl,
                "description" to book.description,
                "subjects" to book.subjects
            )

            firestore.collection("users")
                .document(userId)
                .collection("library")
                .document(book.id)
                .set(data, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync book to cloud")
            Result.failure(e)
        }
    }

    /**
     * Fetches the user's entire library metadata when they sign in.
     */
    suspend fun fetchCloudLibrary(): Result<List<Map<String, Any>>> {
        return try {
            Timber.d("Fetching cloud library")

            val snapshot = libraryCollection().get().await()
            val booksData = snapshot.documents.mapNotNull { it.data }

            Timber.i("Fetched ${booksData.size} books from cloud")
            Result.success(booksData)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch cloud library")
            Result.failure(e)
        }
    }

    // Add to CloudSyncService.kt
    suspend fun deleteBookFromCloud(bookId: String): Result<Unit> {
        return try {
            val userId = authRepository.currentUser ?: throw Exception("User not logged in")
            Timber.d("Deleting book from cloud: $bookId")

            // Assuming your library is stored in a sub-collection like this:
            // users/{userId}/library/{bookId}
            firestore.collection("users")
                .document(userId)
                .collection("library")
                .document(bookId)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete book from cloud")
            Result.failure(e)
        }
    }


    // --- CATEGORIES ---

    // Reference to the user's categories subcollection
    private fun categoryCollection() =
        firestore.collection("users").document(currentUserId).collection("categories")

    /**
     * Uploads or updates a single category as its own document in Firestore.
     */
    suspend fun syncCategoryToCloud(category: CategoryEntity): Result<Unit> {
        return try {
            Timber.d("Syncing category to cloud: ${category.name}")

            val data = mapOf(
                "id" to category.id.toString(),
                "name" to category.name,
                "sortOrder" to category.sortOrder,
                "isSystem" to category.isSystem
            )

            categoryCollection().document(category.id.toString())
                .set(data, SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync category to cloud")
            Result.failure(e)
        }
    }

    /**
     * Fetches all separate category documents from the user's categories subcollection.
     */
    suspend fun fetchCategories(): Result<List<Map<String, Any>>> {
        return try {
            Timber.d("Fetching categories from cloud")

            val snapshot = categoryCollection().get().await()
            val categories = snapshot.documents.mapNotNull { it.data }

            Timber.i("Fetched ${categories.size} categories from cloud")
            Result.success(categories)
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch categories")
            Result.failure(e)
        }
    }

    /**
     * Deletes a specific category document from Firestore immediately.
     */
    suspend fun deleteCategoryFromCloud(categoryId: String): Result<Unit> {
        return try {
            Timber.d("Deleting category $categoryId from cloud")

            categoryCollection().document(categoryId).delete().await()

            Timber.i("Successfully deleted category $categoryId from cloud")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete category from cloud")
            Result.failure(e)
        }
    }
}