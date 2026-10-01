package com.ssafy.s15p21a206.tiger.core.capture.writer

import java.io.File

open class CsvWriter(
    private val file: File,
    private val header: String,
) {
    fun start() {
        file.parentFile?.mkdirs()
        if (!file.exists()) file.writeText("$header\n")
    }

    fun append(row: String) {
        file.appendText("$row\n")
    }
}
