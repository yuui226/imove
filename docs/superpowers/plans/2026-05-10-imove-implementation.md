# iMove Implementation Plan

**Goal:** Build an Android app that transfers photos/videos from USB-connected cameras to phone storage.

**Architecture:** MVVM with Jetpack Compose UI, Room for persistence, SAF for USB device access, Foreground Service for transfers.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, DataStore, Coil, Navigation Compose

---

## Phase 1: Project Scaffolding

### Task 1: Gradle Project Setup

- [ ] Create Android project with `io.github.imove` package, minSdk 29, targetSdk 35
- [ ] Configure `build.gradle.kts` (project) with Hilt, Room, Compose plugin versions
- [ ] Configure `app/build.gradle.kts` with all dependencies:
  - Compose BOM, Material3, Navigation Compose
  - Hilt (core + navigation-compose)
  - Room (runtime, ktx, compiler)
  - DataStore preferences
  - Coil Compose
  - Lifecycle ViewModel Compose
  - Testing: JUnit5, MockK, Compose UI Test, Turbine
- [ ] Configure Kotlin compiler options for Compose
- [ ] Run `./gradlew assembleDebug` to verify build

### Task 2: AndroidManifest & Permissions

- [ ] Create `app/src/main/AndroidManifest.xml` with:
  - Package: `io.github.imove`
  - Permissions: `USB_PERMISSION`, `MANAGE_EXTERNAL_STORAGE`, `READ_EXTERNAL_STORAGE`, `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS`
  - Activity with `android:exported="true"`
  - Intent filter for `ACTION_USB_DEVICE_ATTACHED`
  - `TransferService` declaration with `foregroundServiceType="dataSync"`
  - Meta-data for USB device filter resource
- [ ] Create `res/xml/device_filter.xml` (empty, placeholder for USB filters)

### Task 3: Material 3 Theme

- [ ] Create `ui/theme/Color.kt` with light/dark color definitions
- [ ] Create `ui/theme/Type.kt` with typography scale
- [ ] Create `ui/theme/Theme.kt` with `IMoveTheme` composable supporting light/dark/dynamic color
- [ ] Create `ui/theme/Shape.kt` with rounded corner shapes (12dp cards, 20dp buttons, 28dp dialogs)

### Task 4: Navigation

- [ ] Create `ui/navigation/Screen.kt` with sealed class for routes: `Home`, `Transfer`, `Preview`, `Settings`
- [ ] Create `ui/navigation/NavGraph.kt` with `NavHost` setup
- [ ] Create `MainActivity.kt` with `IMoveTheme` + `NavGraph` scaffold
- [ ] Run app, verify empty screens navigate correctly

---

## Phase 2: Domain Layer

### Task 5: Domain Models

- [ ] Create `domain/model/StorageDevice.kt` (id, volumeUuid, volumeLabel, sourcePath, lastConnected)
- [ ] Create `domain/model/MediaFile.kt` (id, name, path, size, mimeType, dateTaken, dateModified, isVideo)
- [ ] Create `domain/model/TransferItem.kt` (id, file, status, addedAt, completedAt)
- [ ] Create `domain/model/TransferStatus.kt` enum (QUEUED, TRANSFERRING, COMPLETED, SKIPPED, FAILED, CANCELLED)
- [ ] Create `domain/model/UserPreferences.kt` data class

### Task 6: Repository Interfaces

- [ ] Create `domain/repository/DeviceRepository.kt` with Flow-based API
- [ ] Create `domain/repository/MediaRepository.kt` with file listing and transfer tracking
- [ ] Create `domain/repository/TransferRepository.kt` with queue management
- [ ] Create `domain/repository/UserPreferencesRepository.kt` with settings Flow

---

## Phase 3: Data Layer

### Task 7: Room Database

- [ ] Create `data/local/database/entity/DeviceEntity.kt` (Room Entity for devices_table)
- [ ] Create `data/local/database/entity/TransferredFileEntity.kt` (Room Entity for transferred_files_table)
- [ ] Create `data/local/database/dao/DeviceDao.kt` with CRUD operations
- [ ] Create `data/local/database/dao/TransferredFileDao.kt` with insert + query
- [ ] Create `data/local/database/IMoveDatabase.kt` with Room database class
- [ ] Create `data/local/database/converter/Converters.kt` for type conversions
- [ ] Write unit tests for DAOs using in-memory database

### Task 8: DataStore Preferences

- [ ] Create `data/local/datastore/UserPreferencesDataSource.kt`
  - Read/write `UserPreferences` via DataStore
  - Flow-based observation
- [ ] Write unit tests for preferences serialization

### Task 9: Repository Implementations

- [ ] Create `data/repository/DeviceRepositoryImpl.kt`
  - Maps between DeviceEntity and domain StorageDevice
  - Observes connected device via Flow
- [ ] Create `data/repository/MediaRepositoryImpl.kt`
  - Scans directory for media files using SAF
  - Reads EXIF data for dateTaken
  - Checks transferred_files_table for duplicates
- [ ] Create `data/repository/TransferRepositoryImpl.kt`
  - In-memory queue with MutableStateFlow
  - Queue operations: add, remove, clear, cancel
- [ ] Create `data/repository/UserPreferencesRepositoryImpl.kt`
  - Bridges DataStore to domain UserPreferences
- [ ] Write unit tests for each repository

---

## Phase 4: Device Connection

### Task 10: USB Device Detection

- [ ] Create `data/usb/UsbDeviceManager.kt`
  - Register BroadcastReceiver for `ACTION_USB_DEVICE_ATTACHED`
  - Detect device via StorageManager volumes
  - Match by Volume UUID + Label
  - Emit connected device as StateFlow
- [ ] Create `data/usb/StorageAccessManager.kt`
  - `ACTION_OPEN_DOCUMENT_TREE` for directory selection
  - `takePersistableUriPermission` for persistent access
  - List files via DocumentFile API
- [ ] Write unit tests for device matching logic

### Task 11: EXIF & File Utilities

- [ ] Create `util/ExifUtils.kt`
  - Extract dateTaken from EXIF
  - Fallback to file modified date
- [ ] Create `util/FileUtils.kt`
  - Check if file is image or video by MIME type
  - Format file size for display
  - Get destination path for transfer
- [ ] Create `util/DateUtils.kt`
  - Format dates for display
  - Filter files by date range (today, last 3 days)
- [ ] Write unit tests for all utility functions

---

## Phase 5: Transfer System

### Task 12: Transfer Service

- [ ] Create `service/TransferService.kt`
  - Extends `LifecycleService`
  - Foreground notification with progress
  - Collects from TransferRepository queue
  - Copies files one-by-one (single-threaded)
  - Buffered IO (8KB buffer)
  - Progress updates every 1MB
  - Handles cancellation via coroutine
  - Auto-stops when queue empty
- [ ] Write unit tests for transfer logic (mock file operations)

### Task 13: Notifications

- [ ] Create `service/TransferNotificationManager.kt`
  - Create notification channel
  - Build progress notification (transferring)
  - Build completion notification (summary)
  - Build error notification (with retry action)
  - Cancel action on progress notification
- [ ] Write unit tests for notification content

---

## Phase 6: ViewModels

### Task 14: HomeViewModel

- [ ] Create `viewmodel/HomeViewModel.kt`
  - Observe connected device state
  - Expose device connection status
  - Actions: navigate to transfer modes (today, 3 days, custom, all)
- [ ] Write unit tests

### Task 15: TransferViewModel

- [ ] Create `viewmodel/TransferViewModel.kt`
  - Load files from device (filtered by date if applicable)
  - Manage grid column count from preferences
  - Selection state (single mode: tap to queue; multi mode: tap to select)
  - Queue management (add, remove, clear)
  - Trigger transfer service
- [ ] Write unit tests

### Task 16: PreviewViewModel

- [ ] Create `viewmodel/PreviewViewModel.kt`
  - Track current file index in list
  - Navigate between files
  - Action: move current file to queue
- [ ] Write unit tests

### Task 17: SettingsViewModel

- [ ] Create `viewmodel/SettingsViewModel.kt`
  - Read/write all UserPreferences fields
  - Language change triggers AppCompatDelegate
  - Theme change triggers recomposition
- [ ] Write unit tests

---

## Phase 7: UI Screens

### Task 18: Common Components

- [ ] Create `ui/components/MediaThumbnail.kt`
  - Coil AsyncImage for thumbnails
  - Overlay icons: checkmark (transferred), clock (queued), play button (video)
  - Video duration badge
- [ ] Create `ui/components/QueueBottomSheet.kt`
  - List of queued files
  - Remove action (except currently transferring)
  - Transfer progress indicator
- [ ] Create `ui/components/TransferModeCard.kt`
  - Card with icon and label for each transfer mode

### Task 19: HomeScreen

- [ ] Create `ui/home/HomeScreen.kt`
  - No device: centered icon + "请连接存储设备" text
  - Device connected: 4 cards (今日/近三日/自定义/全部)
  - Settings entry point (top-right icon)
- [ ] Write Compose UI tests

### Task 20: TransferScreen

- [ ] Create `ui/transfer/TransferScreen.kt`
  - Single mode: top bar with back + count + multi-select toggle
  - Multi mode: top bar with close + "Move (N)" button
  - LazyVerticalGrid with MediaThumbnail items
  - Tap behavior: single→queue, multi→select
  - Long press: navigate to preview
  - Bottom: queue button with count badge
  - Grid columns from preferences (1-4)
- [ ] Write Compose UI tests

### Task 21: PreviewScreen

- [ ] Create `ui/preview/PreviewScreen.kt`
  - HorizontalPager for swipe navigation
  - Image: full-screen Coil image
  - Video: play button overlay (video playback optional for v1)
  - Top bar: back button
  - Bottom bar: "Move" button
- [ ] Write Compose UI tests

### Task 22: SettingsScreen

- [ ] Create `ui/settings/SettingsScreen.kt`
  - Target directory picker (SAF)
  - Source directory re-selection
  - Language selector: 跟随系统 / 中文 / English
  - Dark mode selector: 跟随系统 / 浅色 / 深色
  - Grid columns selector: 1 / 2 / 3 / 4
- [ ] Write Compose UI tests

---

## Phase 8: DI & Integration

### Task 23: Hilt Modules

- [ ] Create `di/AppModule.kt` — provides DataStore, Context
- [ ] Create `di/DatabaseModule.kt` — provides Room database, DAOs
- [ ] Create `di/RepositoryModule.kt` — binds repository interfaces to implementations
- [ ] Create `IMoveApplication.kt` with `@HiltAndroidApp`
- [ ] Annotate `MainActivity` with `@AndroidEntryPoint`
- [ ] Annotate ViewModels with `@HiltViewModel`
- [ ] Run app, verify Hilt injection works

### Task 24: Internationalization

- [ ] Create `res/values/strings.xml` (Chinese, default)
- [ ] Create `res/values-en/strings.xml` (English)
- [ ] Replace all hardcoded strings in UI with `stringResource()`
- [ ] Implement language switching in SettingsViewModel using `AppCompatDelegate.setApplicationLocales()`
- [ ] Test language switch without app restart

### Task 25: Integration Testing

- [ ] Write end-to-end test: device connect → browse files → select → transfer → verify file copied
- [ ] Write test: duplicate file handling (skip existing)
- [ ] Write test: queue ordering (FIFO)
- [ ] Write test: cancellation mid-transfer
- [ ] Write test: service lifecycle (auto-stop when queue empty)

---

## Commit Strategy

One commit per task. Format: `feat: <task description>`

Examples:
- `feat: project scaffolding with Gradle and dependencies`
- `feat: domain models and repository interfaces`
- `feat: Room database with DAOs and entities`
- `feat: USB device detection and SAF integration`
- `feat: transfer foreground service with notifications`
- `feat: home screen with device connection states`
