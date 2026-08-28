package com.yugentech.quill.aira.util

// A safety net for when the model slips in markdown syntax despite being told to use plain text
// -- strips common annotations down to their plain content instead of leaving raw
// asterisks/hashes/bullets visible to the reader.
fun String.stripMarkdown(): String {
    var result = this

    // Bold/italic (**text**, __text__, *text*, _text_) -> text
    result = result.replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
    result = result.replace(Regex("__(.+?)__"), "$1")
    result = result.replace(Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"), "$1")
    result = result.replace(Regex("(?<!_)_(?!_)(.+?)(?<!_)_(?!_)"), "$1")

    // Inline code (`text`) -> text
    result = result.replace(Regex("`([^`]*)`"), "$1")

    // Links [text](url) -> text
    result = result.replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")

    // Headers at line start (# .. ######) -> removed
    result = result.replace(Regex("(?m)^#{1,6}\\s+"), "")

    // Bullet markers at line start (-, *, •) -> removed
    result = result.replace(Regex("(?m)^\\s*[-*•]\\s+"), "")

    // Numbered list markers at line start (1. , 2) ) -> removed
    result = result.replace(Regex("(?m)^\\s*\\d+[.)]\\s+"), "")

    // Blockquote markers at line start (> ) -> removed
    result = result.replace(Regex("(?m)^>\\s?"), "")

    // Horizontal rules on their own line (---, ***, ___) -> removed
    result = result.replace(Regex("(?m)^\\s*([-*_])\\1{2,}\\s*$"), "")

    return result.trim()
}
