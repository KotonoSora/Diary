# Second Brain & Zettelkasten Knowledge System Guidelines

## Overview
This document outlines the architectural, technical, and development standards for transforming the application into a local-first **Second Brain & Zettelkasten Knowledge System** (inspired by Obsidian, Niklas Luhmann's Zettelkasten Method, and Tiago Forte's CODE/PARA Framework).

## Architectural Patterns
- **MVVM (Model-View-ViewModel)**: Decouples Jetpack Compose UI from domain and data business logic.
- **DDD (Domain-Driven Design)**: Organizes code by core domain features (`feature.vault`, `feature.editor`, `feature.backlinks`, `feature.graph`, `feature.search`, `feature.tags`).
- **Local-First Data Persistence**: Primary truth is stored as standard `.md` Markdown files in user-selected local directories (Vaults).
- **SQLite FTS5 & Room Cache**: Fast full-text search, Zettelkasten note taxonomy indexing, tag extraction, and bidirectional link mapping cached in a local Room database.

## Methodological Frameworks
- **Zettelkasten Method**:
  - Ephemeral ideas -> **Fleeting Notes**.
  - Citation summaries -> **Literature Notes**.
  - Atomic thoughts -> **Permanent Notes** (1 Idea Rule).
  - High-level topic hubs -> **Maps of Content (MOC)**.
- **Second Brain CODE & PARA Framework**:
  - **C**apture: Instant quick capture drawer and voice memos.
  - **O**rganize: **PARA** folders (`1. Projects`, `2. Areas`, `3. Resources`, `4. Archives`).
  - **D**istill: Highlights, blockquotes, and progressive summarization.
  - **E**xpress: Interactive 2D knowledge graph and MOC indexing.

## Core Features & Concepts
- **Vault System**: Hierarchical folder tree stored locally using Storage Access Framework (SAF).
- **Live Preview Editor**: Jetpack Compose GFM editor with real-time formatting, inline media, and task list toggles.
- **WikiLinks & Aliases**: Bidirectional linking using `[[202603011200 Title | Display Alias]]` syntax, block references (`^block-id`), auto-completion, and incoming/outgoing link references.
- **Interactive Knowledge Graph**: 2D force-directed canvas displaying notes as nodes (color-coded by Zettelkasten note type) and links as edges.
- **Fast Full-Text Search**: Instant search over note titles, UIDs, content, and `#tags`.

## Development Methodology
- **TDD (Test-Driven Development)**: All features must follow the "Red-Green-Refactor" cycle. Unit tests and instrumented UI tests are mandatory.
- **Native JNI Performance**: Computationally heavy string parsing, frontmatter extraction, and link regex scans are offloaded to high-performance C++ native code via JNI (`todo-native.cpp`).

## Technology Stack
- **Language**: Kotlin, C++
- **UI Framework**: Jetpack Compose, Material 3
- **Database**: Room Persistence Library with FTS5
- **Async & Storage**: Kotlin Coroutines, StateFlow, Storage Access Framework (`DocumentFile`), WorkManager
- **Dependency Injection**: Hilt
- **Media**: CameraX, ExoPlayer (Media3), Coil
