package com.github.imlena.secretguard.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.io.File
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane

class SecretsPanel(val project: Project) : JPanel() {
    private val secretsList = JBList<SecretInfo>()

    data class SecretInfo(val project: Project, val virtualFile: VirtualFile, val lineNumber: Int, val secretSample: String) {
        val filePath: String get() = virtualFile.path

        fun toNavigatable(): Navigatable {
            return OpenFileDescriptor(project, virtualFile, lineNumber - 1, 0)
        }
    }
    fun findSecrets(project: Project): List<SecretInfo> {

        val secretPatterns = listOf(
            "SECRET_KEY: \"(.*)\"".toRegex(),
            "PASSWORD: \"(.*)\"".toRegex(),
            "API_KEY: \"(.*)\"".toRegex(),
            "SECRET_KEY: '(.*)'".toRegex(),
            "PASSWORD: '(.*)'".toRegex(),
            "API_KEY: '(.*)'".toRegex(),
            "SECRET_KEY: (.*)".toRegex(),
            "PASSWORD: (.*)".toRegex(),
            "API_KEY: (.*)".toRegex()
        )

        val secretsFound = mutableListOf<SecretInfo>()
        val projectRoot = File(project.basePath!!)

        FileUtil.visitFiles(projectRoot) { file ->
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            if (virtualFile != null && !virtualFile.isDirectory && virtualFile.name.endsWith(".yaml")) {
                virtualFile.toNioPath().toFile().readLines().forEachIndexed { index, line ->
                    secretPatterns.forEach { pattern ->
                        pattern.find(line)?.let { matchResult ->
                            secretsFound.add(SecretInfo(project,virtualFile, index + 1, matchResult.value))
                        }
                    }
                }
            }
            true
        }

        return secretsFound
    }

    init {
        layout = BorderLayout()
        updateList()
        add(JScrollPane(secretsList), BorderLayout.CENTER)

        secretsList.cellRenderer = object : ColoredListCellRenderer<SecretInfo>() {
            override fun customizeCellRenderer(
                list: JList<out SecretInfo>,
                value: SecretInfo,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                append(value.virtualFile.name + " : " + value.lineNumber + " - " + value.secretSample)
                icon = AllIcons.FileTypes.Config
            }
        }

        secretsList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selectedValue = secretsList.selectedValue
                selectedValue?.toNavigatable()?.navigate(true)
            }
        }
    }

    fun updateList() {
        val secrets = findSecrets(project)
        val listModel = DefaultListModel<SecretInfo>()
        secrets.forEach { listModel.addElement(it) }
        secretsList.model = listModel
    }
}