package com.github.imlena.secretguard.types

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable

data class SecretInfo(val project: Project, val virtualFile: VirtualFile, val lineNumber: Int, val secretSample: String) {

    fun toNavigatable(): Navigatable {
        println("Navigating to file: ${virtualFile.path} at line $lineNumber")
        return OpenFileDescriptor(project, virtualFile, lineNumber - 1, 0)
    }
}