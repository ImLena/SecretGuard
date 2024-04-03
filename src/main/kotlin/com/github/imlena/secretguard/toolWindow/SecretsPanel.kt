package com.github.imlena.secretguard.toolWindow

import com.github.imlena.secretguard.types.SecretInfo
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBList
import java.awt.BorderLayout
import java.io.File
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import com.intellij.openapi.wm.ToolWindowId.TODO_VIEW

class SecretsPanel(val project: Project) : JPanel(), BulkFileListener {
    private val secretsList = JBList<SecretInfo>()


    fun findSecrets(project: Project): List<SecretInfo> {

        val secretPattern =
           // "^(?i)(pass|login|api_key).*[=:].*['\"].*['\"].*".toRegex()
        listOf(/*
            "SECRET_KEY: \"(.*)\"".toRegex(),
            "(?i)pass\\W*=\\W*['\"]\\s*['\"]\n".toRegex(),
            "API_KEY: \"(.*)\"".toRegex(),
            "SECRET_KEY: '(.*)'".toRegex(),
            "PASSWORD: '(.*)'".toRegex(),
            "API_KEY: '(.*)'".toRegex(),
            "SECRET_KEY: (.*)".toRegex(),
            "PASSWORD: (.*)".toRegex(),
            "API_KEY: (.*)".toRegex()*/
            "login = (.*)".toRegex()
        )

        val secretsFound = mutableListOf<SecretInfo>()
        val projectRoot = File(project.basePath!!)

        FileUtil.visitFiles(projectRoot) { file ->
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            if (virtualFile != null && !virtualFile.isDirectory && (virtualFile.name.endsWith(".yml")
                        ||virtualFile.name.endsWith(".yaml") || virtualFile.name.endsWith(".json") || virtualFile.name.endsWith(".kt"))) {
                virtualFile.toNioPath().toFile().readLines().forEachIndexed { index, line ->
                    secretPattern.forEach { pattern ->
                        pattern.find(line)?.let { matchResult ->
                            secretsFound.add(SecretInfo(project, virtualFile, index + 1, matchResult.value))
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
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, this)


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
    override fun after(events: MutableList<out VFileEvent>) {
        for (event in events) {
            if (event.file != null && event.file!!.isInLocalFileSystem) {
                val filePath = event.file!!.path
                updateList()
            }
        }
    }
}
