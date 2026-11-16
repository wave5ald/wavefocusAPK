package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NoteItem
import com.example.ui.components.WaveEmptyState
import com.example.ui.components.WaveFab
import com.example.ui.components.WaveFilterChip
import com.example.ui.components.WaveSectionLabel
import com.example.ui.theme.WaveTheme
import com.example.ui.viewmodel.RoutineViewModel
import com.example.util.LocalAppLanguage
import com.example.util.appStrings
import java.text.SimpleDateFormat
import java.util.*

// Samsung / Huawei Notes Signature Color Palette (Expanded Rich Palette)
val NotePastelColors = listOf(
    Pair("#FFFBEB", "#451A03"), // Warm Cream Yellow
    Pair("#FEF3C7", "#78350F"), // Golden Butter
    Pair("#FEF08A", "#713F12"), // Lemon Chiffon
    Pair("#F0FDF4", "#14532D"), // Mint Green
    Pair("#DCFCE7", "#166534"), // Soft Basil
    Pair("#D1FAE5", "#065F46"), // Sage Matcha
    Pair("#EFF6FF", "#1E3A8A"), // Soft Sky Blue
    Pair("#DBEAFE", "#1E40AF"), // Powder Blue
    Pair("#E0F2FE", "#075985"), // Glacier Aqua
    Pair("#FAF5FF", "#581C87"), // Lavender Purple
    Pair("#F3E8FF", "#6B21A8"), // Lilac Mist
    Pair("#EDE9FE", "#5B21B6"), // Soft Violet
    Pair("#FFF1F2", "#881337"), // Blush Peach
    Pair("#FFE4E6", "#9F1239"), // Rose Petal
    Pair("#FCE7F3", "#831843"), // Cotton Candy
    Pair("#FFEDD5", "#7C2D12"), // Apricot Sunset
    Pair("#FEE2E2", "#991B1B"), // Coral Blush
    Pair("#F5F5F4", "#292524"), // Warm Stone
    Pair("#F8FAFC", "#0F172A"), // Paper White
    Pair("#E2E8F0", "#1E293B"), // Cloud Gray
    Pair("#CCFBF1", "#115E59"), // Seafoam Cyan
    Pair("#1E293B", "#F8FAFC"), // Dark Slate
    Pair("#0F172A", "#E2E8F0"), // Midnight Navy
    Pair("#3F3F46", "#FAFAFA"), // Graphite Charcoal
    Pair("#312E81", "#EEF2FF"), // Royal Indigo
    Pair("#4A044E", "#FDF4FF")  // Deep Plum
)

/**
 * Standard Samsung / Huawei Notes App Implementation.
 * Clean, fast, document-focused note list with search, tags, grid/list toggle,
 * and a full note editor with checklists and color styling.
 */
@Composable
fun NotesAndBoardsScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var isGridView by remember { mutableStateOf(true) }
    var editingNote by remember { mutableStateOf<NoteItem?>(null) }
    var isCreatingNewNote by remember { mutableStateOf(false) }

    // Filter and search notes
    val filteredNotes = remember(allNotes, searchQuery, selectedFilter) {
        allNotes.filter { note ->
            val matchesSearch = searchQuery.isBlank() ||
                    note.title.contains(searchQuery, ignoreCase = true) ||
                    note.content.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Pinned" -> note.pinned
                "Personal" -> note.content.contains("personal", ignoreCase = true) || note.title.contains("personal", ignoreCase = true)
                "Work" -> note.content.contains("work", ignoreCase = true) || note.title.contains("work", ignoreCase = true)
                "Checklists" -> note.content.contains("☐") || note.content.contains("[ ]")
                else -> true
            }

            matchesSearch && matchesFilter
        }.sortedWith(
            compareByDescending<NoteItem> { it.pinned }
                .thenByDescending { it.updatedAt }
        )
    }

    // Full screen note editor when editing or creating
    if (editingNote != null || isCreatingNewNote) {
        HuaweiSamsungNoteEditor(
            note = editingNote,
            onDismiss = {
                editingNote = null
                isCreatingNewNote = false
            },
            onSave = { title, content, colorHex, textColorHex, pinned ->
                viewModel.saveNote(
                    id = editingNote?.id ?: 0L,
                    title = title,
                    content = content,
                    colorHex = colorHex,
                    textColorHex = textColorHex,
                    pinned = pinned
                )
                editingNote = null
                isCreatingNewNote = false
            },
            onDelete = { noteId ->
                viewModel.deleteNoteById(noteId)
                editingNote = null
                isCreatingNewNote = false
            }
        )
        return
    }

    // Main Notes Screen
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("notes_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp)
        ) {
            // Search Bar & Grid/List Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isArabic) "البحث في الملاحظات..." else "Search notes...",
                            fontSize = 14.sp,
                            color = WaveTheme.colors.onSurfaceMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = if (isArabic) "بحث" else "Search",
                            tint = WaveTheme.colors.onSurfaceMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isArabic) "مسح البحث" else "Clear search",
                                    tint = WaveTheme.colors.onSurfaceMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("notes_search_input")
                )

                // Layout View Toggle Button
                Surface(
                    onClick = { isGridView = !isGridView },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    contentColor = WaveTheme.colors.onSurfaceMuted,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("notes_view_toggle")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View",
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            // Category Filter Pills (LazyRow to prevent wrapping/ruining Checklists and tags on small/old screens)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                val filterTags = listOf("All", "Pinned", "Personal", "Work", "Checklists")
                items(filterTags) { tag ->
                    val isSelected = selectedFilter == tag
                    val displayTag = when (tag) {
                        "All" -> if (isArabic) "الكل" else "All"
                        "Pinned" -> if (isArabic) "⭐ مثبت" else "⭐ Pinned"
                        "Personal" -> if (isArabic) "شخصي" else "Personal"
                        "Work" -> if (isArabic) "عمل" else "Work"
                        "Checklists" -> if (isArabic) "قوائم المهام" else "Checklists"
                        else -> tag
                    }
                    WaveFilterChip(
                        text = displayTag,
                        selected = isSelected,
                        onClick = { selectedFilter = tag },
                        modifier = Modifier.testTag("note_filter_$tag")
                    )
                }
            }

            // Notes Count & Status
            WaveSectionLabel(
                text = if (isArabic) "الملاحظات" else "Notes",
                trailing = if (isArabic) "${filteredNotes.size}" else "${filteredNotes.size}"
            )

            // Notes List or Grid
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WaveEmptyState(
                        emoji = "📝",
                        text = if (searchQuery.isNotBlank()) {
                            if (isArabic) "لا توجد نتائج تطابق '$searchQuery'" else "No notes match '$searchQuery'"
                        } else {
                            if (isArabic) "لا توجد ملاحظات بعد. اضغط على زر + لإنشاء ملاحظتك الأولى" else "No notes yet. Tap + to create your first note."
                        }
                    )
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useGrid = isGridView && maxWidth >= 340.dp

                    if (useGrid) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(bottom = 112.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("notes_grid_view")
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteCardItem(
                                    note = note,
                                    onClick = { editingNote = note },
                                    onPinToggle = {
                                        viewModel.saveNote(
                                            id = note.id,
                                            title = note.title,
                                            content = note.content,
                                            colorHex = note.colorHex,
                                            textColorHex = note.textColorHex,
                                            pinned = !note.pinned
                                        )
                                    },
                                    onDelete = { viewModel.deleteNoteById(note.id) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 112.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("notes_list_view")
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                NoteCardItem(
                                    note = note,
                                    onClick = { editingNote = note },
                                    onPinToggle = {
                                        viewModel.saveNote(
                                            id = note.id,
                                            title = note.title,
                                            content = note.content,
                                            colorHex = note.colorHex,
                                            textColorHex = note.textColorHex,
                                            pinned = !note.pinned
                                        )
                                    },
                                    onDelete = { viewModel.deleteNoteById(note.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to Add Note
        WaveFab(
            text = if (isArabic) "ملاحظة جديدة" else "New Note",
            icon = Icons.Default.Add,
            onClick = {
                editingNote = null
                isCreatingNewNote = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 22.dp, end = 16.dp)
                .testTag("add_note_fab")
        )
    }
}

/**
 * Clean Huawei / Samsung style note card.
 */
@Composable
private fun NoteCardItem(
    note: NoteItem,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = try {
        Color(android.graphics.Color.parseColor(note.textColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.onSurface
    }

    val formattedDate = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.12f)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("note_card_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title & Pin Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (note.pinned) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pinned Note",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onPinToggle() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body Snippet Preview
            if (note.content.isNotBlank()) {
                Text(
                    text = note.content,
                    fontSize = 12.5.sp,
                    color = textColor.copy(alpha = 0.82f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer: Date and item actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    fontSize = 10.5.sp,
                    color = textColor.copy(alpha = 0.6f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Note",
                        tint = textColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

/**
 * Fullscreen Huawei / Samsung style clean note editor.
 */
@Composable
private fun HuaweiSamsungNoteEditor(
    note: NoteItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, colorHex: String, textColorHex: String, pinned: Boolean) -> Unit,
    onDelete: (Long) -> Unit
) {
    val isArabic = LocalAppLanguage.current == "ar"
    val strings = appStrings()
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var colorHex by remember { mutableStateOf(note?.colorHex ?: "#FFFBEB") }
    var textColorHex by remember { mutableStateOf(note?.textColorHex ?: "#451A03") }
    var pinned by remember { mutableStateOf(note?.pinned ?: false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val initialTitle = remember { note?.title ?: "" }
    val initialContent = remember { note?.content ?: "" }
    val initialColorHex = remember { note?.colorHex ?: "#FFFBEB" }
    val initialTextColorHex = remember { note?.textColorHex ?: "#451A03" }
    val initialPinned = remember { note?.pinned ?: false }

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    fun hasUnsavedChanges(): Boolean {
        val changed = title != initialTitle ||
                content != initialContent ||
                colorHex != initialColorHex ||
                textColorHex != initialTextColorHex ||
                pinned != initialPinned
        val hasText = title.isNotBlank() || content.isNotBlank()
        return changed && hasText
    }

    fun handleExit() {
        if (hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onDismiss()
        }
    }

    BackHandler(enabled = true) {
        handleExit()
    }

    val currentBg = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.surface
    }

    val currentText = try {
        Color(android.graphics.Color.parseColor(textColorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.onSurface
    }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("note_editor_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Editor Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { handleExit() },
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isArabic) "رجوع" else "Back",
                        tint = currentText
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Color Palette Button
                    IconButton(
                        onClick = { showColorPicker = !showColorPicker },
                        modifier = Modifier.testTag("editor_color_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = if (isArabic) "تغيير لون الملاحظة" else "Change Note Color",
                            tint = currentText
                        )
                    }

                    // Pin Toggle
                    IconButton(
                        onClick = { pinned = !pinned },
                        modifier = Modifier.testTag("editor_pin_button")
                    ) {
                        Icon(
                            imageVector = if (pinned) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (pinned) (if (isArabic) "إلغاء التثبيت" else "Unpin") else (if (isArabic) "تثبيت" else "Pin"),
                            tint = if (pinned) Color(0xFFF59E0B) else currentText
                        )
                    }

                    // Delete Note
                    if (note != null && note.id != 0L) {
                        IconButton(
                            onClick = { onDelete(note.id) },
                            modifier = Modifier.testTag("editor_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = if (isArabic) "حذف الملاحظة" else "Delete Note",
                                tint = currentText.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Save / Done Checkmark
                    IconButton(
                        onClick = {
                            onSave(title, content, colorHex, textColorHex, pinned)
                        },
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = if (isArabic) "تم" else "Done",
                            tint = currentText
                        )
                    }
                }
            }

            // Color Palette Selector Row (Scrollable across 26 colors)
            AnimatedVisibility(visible = showColorPicker) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(NotePastelColors) { (bg, text) ->
                        val color = Color(android.graphics.Color.parseColor(bg))
                        val isSelected = colorHex == bg
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    colorHex = bg
                                    textColorHex = text
                                    showColorPicker = false
                                }
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(android.graphics.Color.parseColor(text)),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.Center)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = currentText.copy(alpha = 0.12f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Note Content Canvas (Title + Body)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Title Field
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text(
                            text = if (isArabic) "العنوان" else "Title",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentText.copy(alpha = 0.4f)
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = currentText,
                        unfocusedTextColor = currentText,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentText
                    ),
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.clearFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input")
                )

                // Date & Character count
                Text(
                    text = if (isArabic) {
                        "${SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())}  •  ${content.length} حرف"
                    } else {
                        "${SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())}  •  ${content.length} characters"
                    },
                    fontSize = 12.sp,
                    color = currentText.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Body Text Area
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            text = if (isArabic) "ابدأ بكتابة ملاحظتك أو قائمة مهامك أو أفكارك..." else "Start writing your note, checklist, or ideas...",
                            fontSize = 16.sp,
                            color = currentText.copy(alpha = 0.4f),
                            lineHeight = 22.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = currentText,
                        unfocusedTextColor = currentText,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = currentText
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .testTag("editor_body_input")
                )
            }

            // Quick Formatting Bar at bottom
            Surface(
                color = currentText.copy(alpha = 0.05f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Checkbox insertion
                    TextButton(
                        onClick = { content = if (content.isEmpty()) "☐ " else "$content\n☐ " },
                        modifier = Modifier.testTag("editor_add_checkbox")
                    ) {
                        Icon(
                            Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (isArabic) "إضافة قائمة مهام" else "Add Checkbox",
                            tint = currentText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "قائمة مهام" else "Checklist", color = currentText, fontSize = 12.sp)
                    }

                    // Bullet point insertion
                    TextButton(
                        onClick = { content = if (content.isEmpty()) "• " else "$content\n• " },
                        modifier = Modifier.testTag("editor_add_bullet")
                    ) {
                        Icon(
                            Icons.Default.FormatListBulleted,
                            contentDescription = if (isArabic) "إضافة نقطة" else "Add Bullet",
                            tint = currentText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "نقطة" else "Bullet", color = currentText, fontSize = 12.sp)
                    }

                    // Time stamp insertion
                    TextButton(
                        onClick = {
                            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                            content = "$content [$time] "
                        },
                        modifier = Modifier.testTag("editor_add_timestamp")
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = if (isArabic) "إضافة الوقت" else "Add Time",
                            tint = currentText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isArabic) "الوقت" else "Time", color = currentText, fontSize = 12.sp)
                    }
                }
            }
        }

        if (showUnsavedChangesDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedChangesDialog = false },
                title = { Text(if (isArabic) "حفظ التغييرات؟" else "Save Note Changes?") },
                text = { Text(if (isArabic) "لديك تغييرات غير محفوظة في هذه الملاحظة. هل ترغب في حفظها قبل المغادرة أم تجاهلها؟" else "You have unsaved changes in this note. Would you like to save them before leaving, or discard?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showUnsavedChangesDialog = false
                            onSave(title, content, colorHex, textColorHex, pinned)
                        },
                        modifier = Modifier.testTag("unsaved_note_save_button")
                    ) {
                        Text(if (isArabic) "حفظ" else "Save")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showUnsavedChangesDialog = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("unsaved_note_discard_button")
                        ) {
                            Text(if (isArabic) "تجاهل" else "Discard")
                        }
                        TextButton(
                            onClick = { showUnsavedChangesDialog = false },
                            modifier = Modifier.testTag("unsaved_note_cancel_button")
                        ) {
                            Text(strings.cancel)
                        }
                    }
                }
            )
        }
    }
}
