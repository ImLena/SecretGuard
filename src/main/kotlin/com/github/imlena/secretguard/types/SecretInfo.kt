package com.github.imlena.secretguard.types

import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable

class SecretInfo(val project: Project, val virtualFile: VirtualFile, val lineNumber: Int, val secretSample: String) {

    fun toNavigatable(): Navigatable {
        println("Navigating to file: ${virtualFile.path} at line $lineNumber")
        return OpenFileDescriptor(project, virtualFile, lineNumber - 1, 0)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SecretInfo

        if (project != other.project) return false
        if (virtualFile != other.virtualFile) return false
        if (lineNumber != other.lineNumber) return false
        if (secretSample != other.secretSample) return false

        return true
    }

    override fun hashCode(): Int {
        var result = project.hashCode()
        result = 31 * result + virtualFile.hashCode()
        result = 31 * result + lineNumber
        result = 31 * result + secretSample.hashCode()
        return result
    }

    override fun toString(): String {
        return "SecretInfo(project=$project, virtualFile=$virtualFile, lineNumber=$lineNumber, secretSample='$secretSample')"
    }
}