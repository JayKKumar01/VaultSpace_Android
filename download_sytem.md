---

# 📥 VaultSpace – Album Media Download Architecture

## Purpose

Provide a **production-grade, user-visible, cancellable download system** for album media files that:

* Supports **large files**
* Shows **real progress**
* Survives UI changes
* Uses a **foreground service**
* Avoids duplicate downloads
* Allows **cancel-all** from UI or notification
* Does **not** require storage permission
* Is simple (single download at a time)

This system is **explicitly designed** to be extended later without breaking contracts.

---

## Package Scope

All core classes live under:

```
com.github.jaykkumar01.vaultspace.album.helper
```

Service may live under a service package if needed, but is part of this system.

---

## Final Class List (Authoritative)

### Classes

1. AlbumMediaActionHandler
2. AlbumDownloadOrchestrator
3. AlbumDriveHelper
4. AlbumDownloadService
5. AlbumDownloadNotificationController

### Interfaces

6. DriveDownloadCallback

> ❗ No other helper, manager, worker, or background system is allowed in this design.

---

## High-Level Flow

```
UI
 → AlbumMediaActionHandler
   → AlbumDownloadOrchestrator
     → AlbumDownloadService (foreground)
     → AlbumDriveHelper (single download)
     → AlbumDownloadNotificationController
```

---

## 1. AlbumMediaActionHandler

### Role

* Entry point from UI / controller layer
* Owns **one orchestrator instance**
* Delegates user intent

### Responsibilities

* Accept download request
* Accept cancel-all request
* Handle lifecycle release

### Responsibilities it MUST NOT have

* Download logic
* Queue management
* Notification logic
* Service control

### Public API

```
downloadMedia(AlbumMedia media)
cancelAllDownloads()
release()
```

---

## 2. AlbumDownloadOrchestrator (Core Brain)

### Role

Single source of truth for all download activity.

### Responsibilities

* Maintain download queue
* Enforce **single active download**
* Deduplicate by `AlbumMedia.fileId`
* Control foreground service lifecycle
* Coordinate progress updates
* Handle global cancellation

### Internal State (Mandatory)

* Queue<AlbumMedia> pendingQueue
* Set<String> queuedOrActiveIds
* AlbumMedia active
* AtomicBoolean cancelled
* boolean running

### Public API

```
enqueue(AlbumMedia media) → boolean
cancelAll()
isDownloading() → boolean
```

### Behavioral Rules

* FIFO queue
* Same `fileId` cannot be queued twice
* Cancel affects active + queued items
* When queue is empty → stop service + dismiss notification

---

## 3. AlbumDriveHelper

### Role

Pure **Drive I/O layer**.

### Responsibilities

* Download **one** media file
* Stream bytes
* Report progress
* Respect cancellation

### Responsibilities it MUST NOT have

* Queue logic
* Notification logic
* Service lifecycle
* Thread ownership

### Public API (Extended)

```
downloadMedia(
    AlbumMedia media,
    File outputFile,
    DriveDownloadCallback callback,
    AtomicBoolean cancelled
)
```

### Cancellation Contract

* Must frequently check `cancelled.get()`
* Must exit early when cancelled
* Must call exactly one terminal callback

---

## 4. DriveDownloadCallback (Interface)

### Role

Decouple Drive I/O from orchestrator and Android.

### Methods

```
onProgress(long downloadedBytes, long totalBytes)
onCompleted()
onFailed(Exception e)
```

### Rules

* No Android types
* No threading guarantees
* Orchestrator decides thread marshaling

---

## 5. AlbumDownloadService (Foreground Service)

### Role

* Keep process alive during download
* Host foreground notification

### Responsibilities

* Start foreground when requested
* Stop foreground when requested
* Forward cancel intent to orchestrator

### Responsibilities it MUST NOT have

* Download logic
* Queue logic
* Progress calculation

### UX Rules

* Notification is **dismissable**
* Swipe-away == cancel-all
* Cancel button present
* No sticky / sketchy behavior

---

## 6. AlbumDownloadNotificationController

### Role

Notification UI owner.

### Responsibilities

* Build notification
* Update progress
* Show active media name
* Expose cancel action
* Dismiss notification

### Public API

```
showInitial(String mediaName)
updateProgress(String mediaName, long downloaded, long total)
showCompleted(String mediaName)
showFailed(String mediaName)
dismiss()
```

### Cancel Wiring

* Cancel action sends intent → service
* Service calls orchestrator.cancelAll()

---

## Cancellation Semantics (Locked)

### Cancellation Sources

* UI (cancel button)
* Notification action
* Notification swipe-away

### Cancellation Effects

1. cancelled.set(true)
2. Active download stops cooperatively
3. Queue cleared
4. Notification dismissed
5. Service stopped

---

## Download Rules (Locked)

* Single active download only
* FIFO queue
* Deduplicate by `AlbumMedia.fileId`
* No per-item cancel
* No resume after app kill

---

## Storage Rules (Locked)

* Download location:

```
getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
```

### Implications

* No storage permission needed
* Scoped storage compliant
* Large files supported
* User can export/share later

---

## Explicit Non-Goals (Do NOT add)

* Parallel downloads
* DownloadManager
* WorkManager
* Resume after app kill
* Background-only downloads
* Public external storage writes

---

## Extension Safety

This design allows future addition of:

* Parallel downloads
* Resume support
* MediaStore export
* Per-item cancel
* Retry policies

**Without breaking existing APIs.**

---

## Status

✅ Design complete
✅ APIs locked
✅ No missing pieces

---