# Project Plan

GestBraccianti (GestBracc) - An Android app to manage agricultural workers' hours and earnings during the harvest, organized by year, with offline support and data migration between years.

## Project Brief

# GestBraccianti (GestBracc) Project Brief

## Features

*   **Yearly Harvest Management**: Organize data by harvest year with the ability to migrate worker lists and plantation info from the previous year when starting a new season.
*   **Worker Registry & Rates**: Manage a list of agricultural workers, including their individual hourly rates to support financial calculations.
*   **Daily Hour Logging**: A streamlined interface for entering daily working hours for each individual during the grape harvest.
*   **Financial & Work Summary**: A comprehensive grid view displaying total hours worked and total earnings per worker based on their specific hourly rate.
*   **Offline-First Operation**: Full local persistence using a database to ensure functionality in remote vineyard locations with poor connectivity.

## High-Level Technical Stack
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose with Material Design 3 (Edge-to-Edge)
*   **Database**: Room (using **KSP** for code generation)
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Design**: Vibrant and energetic color scheme following Material 3 guidelines.

## Implementation Steps

### Task_1_DataLayer: Define data models and set up Room database for workers, logs, and harvest years.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - Room entities for Worker, WorkLog, and HarvestYear defined
  - DAOs for CRUD operations implemented
  - Room database initialized and building with KSP
  - App builds successfully
- **StartTime:** 2026-04-29 11:50:33 CEST

### Task_2_ViewModels: Implement repositories and ViewModels for business logic, including year migration and earnings calculations.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Repositories for data abstraction implemented
  - ViewModels for Worker management and Logging created
  - Earnings calculation logic implemented
  - Year-to-year migration logic for workers/plantations implemented

### Task_3_UIImplementation: Create the UI screens using Jetpack Compose: Year selection, Worker Registry, Daily Logging, and Financial Summary.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Navigation between screens implemented
  - Worker Registry and Daily Hour Logging screens functional
  - Financial Summary grid view displays correct totals
  - Material Design 3 components used for all UI elements

### Task_4_ThemingAndPolish: Apply Material 3 theming, Edge-to-Edge display, and create a custom app icon.
- **Status:** PENDING
- **Acceptance Criteria:**
  - Vibrant Material 3 color scheme implemented (Light/Dark)
  - Full Edge-to-Edge display active
  - Adaptive app icon matching the harvest theme created
  - App builds, runs without crashes, and meets all requirements

