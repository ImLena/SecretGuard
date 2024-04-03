package com.github.imlena.secretguard.utils

import com.github.imlena.secretguard.types.SecretInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

fun getSearchRegex(): Regex {
    return ".*(?i)(pass|login|api_key).*[=:].*['\"].*['\"].*".toRegex()
}

fun filterConfigFiles(file: VirtualFile?): Boolean {
    return file != null
            && !file.isDirectory
            && (file.name.endsWith(".yml")
                    || file.name.endsWith(".yaml")
                    || file.name.endsWith(".json")
                    || file.name.endsWith(".kt")
                    )
}

fun findAllSecretsInFile(project: Project,
                                file: VirtualFile,
                                secretsFound: MutableList<SecretInfo>,
                                secretPattern: Regex) {
    file.toNioPath().toFile().readLines().forEachIndexed { index, line ->
        secretPattern.find(line)?.let { matchResult ->
            println("Found secret \"${matchResult.value}\" on line number $index")
            secretsFound.add(
                SecretInfo(
                    project,
                    file,
                    index + 1,
                    matchResult.value
                )
            )
        }
    }
}