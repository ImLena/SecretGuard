package com.github.imlena.secretguard.toolWindow;

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class SecretsToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val secretsPanel = SecretsPanel(project)
        val treeSecretsPanel =  TreeSecretsPanel(project)
        val content = ContentFactory.getInstance().createContent(treeSecretsPanel, "Secrets", false)
        toolWindow.contentManager.addContent(content)
    }
}
