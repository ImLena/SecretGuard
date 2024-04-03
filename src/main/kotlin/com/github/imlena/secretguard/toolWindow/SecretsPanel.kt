package com.github.imlena.secretguard.toolWindow

import com.github.imlena.secretguard.types.SecretInfo
import com.github.imlena.secretguard.utils.filterConfigFiles
import com.github.imlena.secretguard.utils.findAllSecretsInFile
import com.github.imlena.secretguard.utils.getSearchRegex
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
import java.util.concurrent.CompletableFuture
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane

class SecretsPanel(val project: Project) : JPanel(), BulkFileListener {
    private val secretsList = JBList<SecretInfo>()


    fun findSecrets(project: Project): List<SecretInfo> {
        val secretPattern = getSearchRegex()
        val secretsFound = mutableListOf<SecretInfo>()
        val projectRoot = File(project.basePath!!)

        var cntVisited = 0
        FileUtil.visitFiles(projectRoot) { file ->
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            if (filterConfigFiles(virtualFile)) {
                ++cntVisited
                findAllSecretsInFile(project, virtualFile!!, secretsFound, secretPattern)
            }
            true
        }

        println("Update panel, checked $cntVisited files, found ${secretsFound.size} secrets")
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

    private fun updateList() {
        val secretsFuture = CompletableFuture.supplyAsync {
            findSecrets(project)
        }
        val secrets = secretsFuture.get()
        val listModel = DefaultListModel<SecretInfo>()
        secrets.forEach { listModel.addElement(it) }
        secretsList.model = listModel
    }

    override fun after(events: MutableList<out VFileEvent>) {
        println("Update secrets window tid=" + Thread.currentThread().id)
        for (event in events) {
            if (event.file != null && event.file!!.isInLocalFileSystem) {
                updateList()
            }
        }
    }
}
