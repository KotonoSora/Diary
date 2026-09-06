# Markdown Editor & Knowledge Base Project Guidelines

## Overview
This document outlines the architectural, technical, and development standards for rebuilding and expanding the application into a local-first **Markdown File Editor & Knowledge Base** (inspired by Obsidian and Inkdrop).

## Architectural Patterns
- **MVVM (Model-View-ViewModel)**: Decouples Jetpack Compose UI from domain and data business logic.
- **DDD (Domain-Driven Design)**: Organizes code by core domain features (e.g., `feature.vault`, `feature.editor`, `feature.backlinks`, `feature.graph`, `feature.search`).
- **Local-First Data Persistence**: Primary truth is stored as standard `.md` Markdown files in user-selected local directories (Vaults).
- **SQLite FTS5 & Room Cache**: Fast full-text search, tag extraction, and bidirectional link mapping cached in a local Room database.

## Core Features & Concepts
- **Vault System**: Hierarchical file and folder tree stored locally using Storage Access Framework (SAF).
- **Live Preview Editor**: Jetpack Compose GFM (GitHub Flavored Markdown) editor with real-time formatting, inline media, and task list toggles.
- **WikiLinks & Backlinks**: Bidirectional linking using `[[Note Title]]` syntax, auto-completion, and incoming/outgoing link references.
- **Interactive Knowledge Graph**: 2D force-directed canvas displaying notes as nodes and links as edges.
- **Fast Full-Text Search**: Instant search over note titles, content, and `#tags`.

## Development Methodology
- **TDD (Test-Driven Development)**: All features must follow the "Red-Green-Refactor" cycle. Unit tests and instrumented UI tests are mandatory.
- **Native JNI Performance**: Computationally heavy string parsing, frontmatter extraction, and link regex scans are offloaded to high-performance C++ native code via JNI (`md-native.cpp`).

## Technology Stack
- **Language**: Kotlin, C++
- **UI Framework**: Jetpack Compose, Material 3
- **Database**: Room Persistence Library with FTS5
- **Async & Storage**: Kotlin Coroutines, StateFlow, Storage Access Framework (`DocumentFile`), WorkManager
- **Dependency Injection**: Hilt
- **Media**: CameraX, ExoPlayer (Media3), Coil
