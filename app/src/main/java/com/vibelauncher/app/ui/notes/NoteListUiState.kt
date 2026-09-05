package com.vibelauncher.app.ui.notes

import com.vibelauncher.app.model.NoteCategory
import com.vibelauncher.app.model.NoteItem

enum class NoteFilterTab { ALL, PERSONAL, WORK, IDEAS, JOURNAL }

data class NoteListUiState(
    val notes: List<NoteItem> = emptyList(),
    val filter: NoteFilterTab = NoteFilterTab.ALL,
    val query: String = "",
    val searchVisible: Boolean = false
) {
    val filteredNotes: List<NoteItem>
        get() {
            var result = notes
            if (filter != NoteFilterTab.ALL) {
                val category = NoteCategory.valueOf(filter.name)
                result = result.filter { it.category == category }
            }
            if (query.isNotBlank()) {
                result = result.filter {
                    it.title.contains(query, ignoreCase = true) || plainTextOf(it.blocks).contains(query, ignoreCase = true)
                }
            }
            return result.sortedWith(compareByDescending<NoteItem> { it.pinned }.thenByDescending { it.updatedAt })
        }
}
