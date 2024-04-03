package com.github.imlena.secretguard.listeners;

import com.github.imlena.secretguard.types.SecretInfo
import com.github.imlena.secretguard.utils.filterConfigFiles
import com.github.imlena.secretguard.utils.findAllSecretsInFile
import com.github.imlena.secretguard.utils.getSearchRegex
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.tools.ExternalToolsCheckinHandlerFactory
import com.intellij.util.PairConsumer
import org.jetbrains.annotations.NotNull

internal class CheckSecretsOnCommitListener : ExternalToolsCheckinHandlerFactory() {
    private var checkSecretGuard = false

    @NotNull
    @Override
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return object : CheckinHandler() {

            override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent? {
                return BooleanCommitOption(
                        panel = panel,
                        text = "Check Secrets",
                        disableWhenDumb = false,
                        { checkSecretGuard },
                        { checkSecretGuard = it }
                )
            }

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
                val securityBugs = findSecrets(panel.project, panel.virtualFiles)
                val securityBugFound = securityBugs.isNotEmpty()

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
                        val toolWindow = toolWindowManager.getToolWindow("Secret Guard")
                        toolWindow?.activate(null)
                        ReturnResult.CLOSE_WINDOW
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

    fun findSecrets(project: Project, filesForCommit: MutableCollection<VirtualFile>): List<SecretInfo> {
        println("Commit listener tid=" + Thread.currentThread().id)
        val secretPattern = getSearchRegex()
        println("Files in review: " + filesForCommit.size)

        val secretsFound = mutableListOf<SecretInfo>()
        filesForCommit
            .filter { file -> filterConfigFiles(file) }
            .forEach { file -> findAllSecretsInFile(project, file, secretsFound, secretPattern)}
        return secretsFound
    }

}