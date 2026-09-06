package com.kotonosora.todolist.feature.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kotonosora.todolist.domain.model.NoteItem
import com.kotonosora.todolist.domain.model.NoteType
import com.kotonosora.todolist.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.hypot

data class GraphEdge(
    val sourceId: String,
    val targetTitle: String
)

data class GraphUiState(
    val notes: List<NoteItem> = emptyList(),
    val edges: List<GraphEdge> = emptyList()
)

@HiltViewModel
class GraphViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GraphUiState())
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    init {
        loadGraph()
    }

    fun loadGraph() {
        viewModelScope.launch {
            val notes = vaultRepository.getAllNotes().firstOrNull() ?: emptyList()
            val edges = mutableListOf<GraphEdge>()
            for (note in notes) {
                val outgoing = vaultRepository.getOutgoingLinks(note.id).firstOrNull() ?: emptyList()
                for (targetTitle in outgoing) {
                    edges.add(GraphEdge(sourceId = note.id, targetTitle = targetTitle))
                }
            }
            _uiState.value = GraphUiState(notes = notes, edges = edges)
        }
    }
}

@Composable
fun KnowledgeGraphScreen(
    viewModel: GraphViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    KnowledgeGraphContent(
        uiState = uiState,
        onBack = onBack,
        onNoteClick = onNoteClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeGraphContent(
    uiState: GraphUiState,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val nodePositions = remember { mutableStateMapOf<String, Offset>() }

    LaunchedEffect(uiState.notes, uiState.edges) {
        if (uiState.notes.isNotEmpty()) {
            val layoutMap = JGraphTLayoutHelper.computeLayoutPositions(
                notes = uiState.notes,
                edges = uiState.edges
            )
            nodePositions.clear()
            nodePositions.putAll(layoutMap)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zettelkasten Knowledge Graph") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.2f, 5f)
                            offset += pan
                        }
                    }
                    .pointerInput(nodePositions.toMap()) {
                        detectTapGestures { tapOffset ->
                            val adjustedTap = (tapOffset - offset) / scale
                            nodePositions.forEach { (noteId, pos) ->
                                if (hypot(adjustedTap.x - pos.x, adjustedTap.y - pos.y) <= 30f) {
                                    onNoteClick(noteId)
                                }
                            }
                        }
                    }
            ) {
                uiState.edges.forEach { edge ->
                    val startPos = nodePositions[edge.sourceId]
                    val targetNote = uiState.notes.firstOrNull { it.title == edge.targetTitle }
                    val endPos = if (targetNote != null) nodePositions[targetNote.id] else null

                    if (startPos != null && endPos != null) {
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.6f),
                            start = startPos * scale + offset,
                            end = endPos * scale + offset,
                            strokeWidth = 2f * scale
                        )
                    }
                }

                uiState.notes.forEach { note ->
                    val pos = nodePositions[note.id] ?: return@forEach
                    val canvasPos = pos * scale + offset
                    val nodeColor = when (note.noteType) {
                        NoteType.FLEETING -> Color(0xFFFFC107)    // Yellow
                        NoteType.LITERATURE -> Color(0xFF2196F3)  // Blue
                        NoteType.PERMANENT -> Color(0xFF4CAF50)   // Green
                        NoteType.MOC -> Color(0xFF9C27B0)         // Purple
                    }
                    drawCircle(
                        color = nodeColor,
                        radius = 16f * scale,
                        center = canvasPos
                    )
                }
            }

            // Legend Overlay Card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Legend",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.size(4.dp))
                    LegendItem(color = Color(0xFFFFC107), label = "Fleeting Note")
                    LegendItem(color = Color(0xFF2196F3), label = "Literature Note")
                    LegendItem(color = Color(0xFF4CAF50), label = "Permanent Note")
                    LegendItem(color = Color(0xFF9C27B0), label = "Map of Content (MOC)")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, name = "Zettelkasten Graph Screen Preview")
@Composable
fun KnowledgeGraphScreenPreview() {
    val sampleNotes = listOf(
        NoteItem("Note1.md", "Fleeting Thought", "", "Content", noteType = NoteType.FLEETING),
        NoteItem("Note2.md", "Book Summary", "", "Content", noteType = NoteType.LITERATURE),
        NoteItem("Note3.md", "Atomic Zettel Concept", "", "Content", noteType = NoteType.PERMANENT),
        NoteItem("Note4.md", "MOC Overview", "", "Content", noteType = NoteType.MOC)
    )
    val sampleEdges = listOf(
        GraphEdge("Note1.md", "Atomic Zettel Concept"),
        GraphEdge("Note2.md", "Atomic Zettel Concept"),
        GraphEdge("Note3.md", "MOC Overview")
    )

    MaterialTheme {
        KnowledgeGraphContent(
            uiState = GraphUiState(notes = sampleNotes, edges = sampleEdges),
            onBack = {},
            onNoteClick = {}
        )
    }
}
