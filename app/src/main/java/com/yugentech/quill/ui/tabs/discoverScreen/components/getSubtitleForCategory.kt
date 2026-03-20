fun getSubtitleForCategory(category: String): String {
    return when (category) {
        "Philosophy" -> "Expand your mind with foundational wisdom"
        "Mystery" -> "Whodunits, clues, and thrilling investigations"
        "Fantasy" -> "Epic worlds, magic, and mythical journeys"
        "Adventure" -> "Daring exploits and exciting quests"
        "Science Fiction" -> "Journeys to the future and beyond"
        "Shorts" -> "Bite-sized stories for quick reading"
        "Horror" -> "Classic tales of terror and the supernatural"
        "Comedy" -> "Witty satire and classic humor"
        "Drama" -> "Powerful plays and theatrical classics"
        "Biography" -> "Fascinating lives and historical figures"
        "Poetry" -> "Timeless verses and lyrical masterpieces"
        // --- Added Categories ---
        "Satire" -> "Sharp wit and clever social critiques"
        "Memoir" -> "Intimate reflections on personal journeys"
        "Autobiography" -> "Life stories told by those who lived them"
        "Children's" -> "Ageless tales for young readers"
        // --- New Additions ---
        "Fiction" -> "Timeless stories of imagination and wonder"
        "Non-Fiction" -> "Real-world knowledge and insightful facts"
        "Travel" -> "Adventures and explorations across the globe"
        "Spirituality" -> "Explorations of faith, meaning, and the soul"
        // ------------------------
        else -> "Curated classics from Standard Ebooks" // The fallback for random ones
    }
}