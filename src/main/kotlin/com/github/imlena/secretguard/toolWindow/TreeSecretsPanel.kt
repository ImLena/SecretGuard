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
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

class TreeSecretsPanel(val project: Project) : JPanel(), BulkFileListener {
    private val secretsTree = Tree(DefaultMutableTreeNode())


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
        updateTree()
        add(JScrollPane(secretsTree), BorderLayout.CENTER)
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, this)

        secretsTree.cellRenderer = object : DefaultTreeCellRenderer() {
            override fun getTreeCellRendererComponent(
                tree: JTree?,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ): Component {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
                if (value is DefaultMutableTreeNode) {
                    when {
                        value.isRoot -> {
                            icon = null
                        }

                        value.childCount > 0 -> {
                            if (value.userObject is SecretInfo) {
                                icon = null
                            } else {
                                icon = AllIcons.FileTypes.Yaml
                            }
                        }

                        value.userObject is SecretInfo -> {
                            val secret = value.userObject as SecretInfo
                            icon = AllIcons.FileTypes.Text
                            text = "${secret.secretSample}:${secret.lineNumber}"
                        }
                    }
                }
                return this
            }
        }

        secretsTree.addTreeSelectionListener { e ->
            if (e.isAddedPath) {
                println("Selected path: ${e.path}")
                println("Last component class: ${e.path.lastPathComponent::class.java.name}")
                (e.path.lastPathComponent as? DefaultMutableTreeNode)?.let { node ->
                    (node.userObject as? SecretInfo)?.let { secretInfo ->
                        SwingUtilities.invokeLater {
                            secretInfo.toNavigatable().navigate(true)
                        }
                    }
                }
            }
        }
    }

    fun buildTree(secretsInfo: List<SecretInfo>): DefaultMutableTreeNode {
        val filesCount = secretsInfo.map { it.virtualFile }.distinct().size
        val rootNode = DefaultMutableTreeNode("Found ${secretsInfo.size} Secrets in $filesCount files")

        // Group
        val secretsByFile = secretsInfo.groupBy { it.virtualFile }

        secretsByFile.forEach { (virtualFile, secrets) ->
            val fileNode = DefaultMutableTreeNode(virtualFile.name)
            secrets.forEach { secret ->
                // Вот ту если не передавать secret, а например "${secret.secretSample}:${secret.lineNumber}", то рантайм работает
                val secretNode = DefaultMutableTreeNode(secret)
                fileNode.add(secretNode)
            }
            rootNode.add(fileNode)
        }

        return rootNode
    }

    private fun updateTree() {
        val secretsInfo = findSecrets(project)
        val root = buildTree(secretsInfo)
        // чтобы раскрыть все узлы дерева после его обновления
        secretsTree.model = DefaultTreeModel(root)
        // (secretsTree.model as DefaultTreeModel).reload()
        TreeUtil.expandAll(secretsTree)
    }

    override fun after(events: MutableList<out VFileEvent>) {
        println("Update secrets window tid=" + Thread.currentThread().id)
        for (event in events) {
            if (event.file != null && event.file!!.isInLocalFileSystem) {
                updateTree()
            }
        }
    }
}
