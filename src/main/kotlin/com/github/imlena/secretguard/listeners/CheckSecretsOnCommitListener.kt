package com.github.imlena.secretguard.listeners;

import com.github.imlena.secretguard.toolWindow.SecretsPanel
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.tools.ExternalToolsCheckinHandlerFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.PairConsumer
import org.jetbrains.annotations.NotNull

internal class CheckSecretsOnCommitListener : ExternalToolsCheckinHandlerFactory() {

    @NotNull
    @Override
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {

            override fun checkinSuccessful() {
                println("Success")
            }

            override fun checkinFailed(exception: MutableList<VcsException>?) {
                println("Failure")
            }

            override fun beforeCheckin(
                executor: CommitExecutor?,
                additionalDataConsumer: PairConsumer<Any, Any>?
            ): ReturnResult? {
                val securityBugFound = true // TODO: check security bugs

                if (!securityBugFound) {
                    return super.beforeCheckin(executor, additionalDataConsumer)
                }

                // ask user to commit with bugs or fix
                val commit = MessageDialogBuilder.yesNoCancel(
                    VcsBundle.message("checkin.commit.checks.failed"),
                    VcsBundle.message(
                        "checkin.commit.checks.failed.with.error.message",
                        "security issues found, commit anyway?"
                    )
                )
                    .yesText(StringUtil.toTitleCase("Review secrets"))
                    .noText(StringUtil.toTitleCase("Yes")) // подтвердить коммит
                    .cancelText(StringUtil.toTitleCase("No")) // отменить коммит
                    .cancelText(VcsBundle.message("checkin.commit.checks.failed.cancel.button"))
                    .show(panel.project)

                // process user answer
                return when (commit) {
                    Messages.YES -> {
                        val toolWindowManager = ToolWindowManager.getInstance(panel.project)
                        val toolWindow = toolWindowManager.getToolWindow("Secrets") ?: return ReturnResult.CANCEL

                        toolWindow.show {
                            val secretsPanel = SecretsPanel(panel.project)
                            secretsPanel.updateList()
                            val contentFactory = ContentFactory.getInstance()
                            toolWindow.contentManager.removeAllContents(true)
                            val content = contentFactory.createContent(secretsPanel, "", false)
                            toolWindow.contentManager.addContent(content)
                        }

                        ReturnResult.CANCEL
                    }

                    Messages.NO -> {
                        ReturnResult.COMMIT
                    }

                    else -> {
                        ReturnResult.CANCEL
                    }
                }
            }
        }
    }
}