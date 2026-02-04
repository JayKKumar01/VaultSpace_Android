package com.github.jaykkumar01.vaultspace;

public class DownloadList {
    //Perfect 👍
    //Here is the **authoritative checklist** — **only lists**, **no descriptions**, **nothing missing**.
    //
    //You can treat this as the **single source of truth** and come back to it anytime.
    //
    //---
    //
    //## ✅ FINAL CLASS & API CHECKLIST
    //
    //---
    //
    //### 1️⃣ Classes (exact list)
    //
    //1. AlbumMediaActionHandler
    //2. AlbumDownloadOrchestrator
    //3. AlbumDriveHelper
    //4. AlbumDownloadService
    //5. AlbumDownloadNotificationController
    //
    //---
    //
    //### 2️⃣ Interfaces
    //
    //1. DriveDownloadCallback
    //
    //---
    //
    //### 3️⃣ AlbumMediaActionHandler — Public API
    //
    //* downloadMedia(AlbumMedia media)
    //* cancelAllDownloads()
    //* release()
    //
    //---
    //
    //### 4️⃣ AlbumDownloadOrchestrator — Public API
    //
    //* enqueue(AlbumMedia media) → boolean
    //* cancelAll()
    //* isDownloading() → boolean
    //
    //---
    //
    //### 5️⃣ AlbumDriveHelper — Public API (extended)
    //
    //* downloadMedia(AlbumMedia media, File outputFile, DriveDownloadCallback callback, AtomicBoolean cancelled)
    //
    //---
    //
    //### 6️⃣ DriveDownloadCallback — Methods
    //
    //* onProgress(long downloadedBytes, long totalBytes)
    //* onCompleted()
    //* onFailed(Exception e)
    //
    //---
    //
    //### 7️⃣ AlbumDownloadService — Lifecycle API
    //
    //* startForeground()
    //* stopForeground()
    //
    //---
    //
    //### 8️⃣ AlbumDownloadNotificationController — Public API
    //
    //* showInitial(String mediaName)
    //* updateProgress(String mediaName, long downloaded, long total)
    //* showCompleted(String mediaName)
    //* showFailed(String mediaName)
    //* dismiss()
    //
    //---
    //
    //### 9️⃣ Orchestrator Internal State (must exist)
    //
    //* Queue<AlbumMedia> queue
    //* Set<String> queuedOrActiveIds
    //* AlbumMedia active
    //* AtomicBoolean cancelled
    //* boolean running
    //
    //---
    //
    //### 🔟 Cancellation Sources (all wired)
    //
    //* UI → cancelAllDownloads()
    //* Notification action → cancel
    //* Notification swipe-away → cancel
    //
    //---
    //
    //### 1️⃣1️⃣ Download Rules (locked)
    //
    //* Single active download
    //* FIFO queue
    //* Deduplicate by AlbumMedia.fileId
    //* Cancel-all only (no per-item cancel)
    //
    //---
    //
    //### 1️⃣2️⃣ Service Rules (locked)
    //
    //* Foreground only while downloading
    //* Stops when queue empty
    //* Notification dismissable
    //* Cancel button present
    //
    //---
    //
    //### 1️⃣3️⃣ Storage Rules (locked)
    //
    //* Use getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    //* No storage permission required
    //
    //---
    //
    //### 1️⃣4️⃣ Explicit Non-Goals (locked)
    //
    //* Parallel downloads
    //* Resume after app kill
    //* DownloadManager
    //* WorkManager
    //* Background-only downloads
    //
    //---
    //
    //If you’re happy, reply with:
    //
    //**“Proceed with skeleton classes”**
    //
    //Next step will be **only skeletons**, nothing else.
}
