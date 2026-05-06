# To-Do Application Project Guidelines

## Overview
This document outlines the architectural and development standards for the To-Do application.

## Architectural Patterns
- **MVVM (Model-View-ViewModel)**: Decouples UI from data logic.
- **DDD (Domain-Driven Design)**: Organizes code by feature domains (e.g., `feature.todo`, `feature.calendar`, `feature.media`).
- **Data Persistence**: Hybrid approach using Room (SQLite) for cache/indexing and external shared storage (Markdown/Plain Text) for long-term user-accessible persistence.

## Development Methodology
- **TDD (Test-Driven Development)**: All features must follow the "Red-Green-Refactor" cycle. Unit tests and instrumented tests are mandatory.
- **Performance**: Computationally expensive tasks are offloaded to C++ native code via JNI.

## File Storage Standards
- To-Do items are persisted as individual `.md` or `.txt` files in shared external storage (`Documents` directory).
- Data remains accessible to the user after app uninstallation and can be read by other applications/text editors.
- File sync logic reconciles changes between shared files and the internal SQLite database.

## Technology Stack
- **Language**: Kotlin, C++
- **UI**: Jetpack Compose
- **Database**: Room Persistence Library
- **Async/Background**: WorkManager, Coroutines
- **Architecture Components**: ViewModel, StateFlow/LiveData
- **Media**: Camera, Microphone, Storage APIs
- **Calendar**: Material Calendar / Compose-based Calendar
