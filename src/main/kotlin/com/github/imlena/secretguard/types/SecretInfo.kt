package com.github.imlena.secretguard.types

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable

data class SecretInfo(val project: Project, val virtualFile: VirtualFile, val lineNumber: Int, val secretSample: String) {
    val filePath: String get() = virtualFile.path
    fun toNavigatable(): Navigatable {
        return OpenFileDescriptor(project, virtualFile, lineNumber - 1, 0)
    }
}