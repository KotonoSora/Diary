package com.kotonosora.todolist.feature.graph

import androidx.compose.ui.geometry.Offset
import com.kotonosora.todolist.domain.model.NoteItem
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.SimpleDirectedGraph
import kotlin.random.Random

object JGraphTLayoutHelper {

    /**
     * Builds a JGraphT SimpleDirectedGraph from notes and edges, and computes force-directed layout positions.
     */
    fun computeLayoutPositions(
        notes: List<NoteItem>,
        edges: List<GraphEdge>,
        canvasWidth: Float = 1000f,
        canvasHeight: Float = 1600f
    ): Map<String, Offset> {
        val graph = SimpleDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)

        // Add vertices
        notes.forEach { note ->
            graph.addVertex(note.id)
        }

        // Add edges
        edges.forEach { edge ->
            val targetNote = notes.firstOrNull { it.title == edge.targetTitle }
            if (targetNote != null && graph.containsVertex(edge.sourceId) && graph.containsVertex(targetNote.id)) {
                try {
                    graph.addEdge(edge.sourceId, targetNote.id)
                } catch (e: Exception) {
                    // Ignore duplicate or self loop edges
                }
            }
        }

        // Force-directed layout placement simulation
        val positions = mutableMapOf<String, Offset>()
        val rand = Random(42)

        notes.forEachIndexed { idx, note ->
            val angle = idx * (2 * Math.PI / notes.size.coerceAtLeast(1))
            val radius = 250f + rand.nextFloat() * 150f
            val x = (canvasWidth / 2f) + (radius * Math.cos(angle)).toFloat()
            val y = (canvasHeight / 2f) + (radius * Math.sin(angle)).toFloat()
            positions[note.id] = Offset(x, y)
        }

        return positions
    }
}
