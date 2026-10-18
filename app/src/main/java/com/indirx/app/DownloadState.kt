package com.indirx.app

import java.util.concurrent.atomic.AtomicBoolean

object DownloadState {
    val running = AtomicBoolean(false)
    @Volatile var cancelled = false
    @Volatile var progress = 0
    @Volatile var status = "İndirmeye hazır"
    @Volatile var detail = "Seçtiğin içerikler burada görünecek."
    @Volatile var hasOperation = false
    @Volatile var history = ""
        private set

    @Synchronized fun record(value: String) {
        hasOperation = true
        history = (value + "\n\n" + history).take(24000)
    }

    // onStart also runs when the existing activity returns from the background.
    @Synchronized fun beginVisibleSession() {
        history = ""
        if (!running.get()) {
            hasOperation = false
            cancelled = false
            progress = 0
            status = "İndirmeye hazır"
            detail = "Seçtiğin içerikler burada görünecek."
        }
    }
}
