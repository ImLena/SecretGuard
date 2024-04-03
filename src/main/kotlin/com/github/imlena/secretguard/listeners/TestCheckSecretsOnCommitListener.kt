package com.github.imlena.secretguard.listeners

import com.github.imlena.secretguard.StringBundle
import com.github.imlena.secretguard.actions.NotifySecretsLeakAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption
import com.intellij.openapi.vcs.checkin.*
import com.intellij.openapi.vcs.ui.RefreshableOnComponent
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class SecretCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        return SecretCheckinHandler(panel.project)
    }
}

class SecretCommitProblem(private val worker: SecretCheckinHandlerWorker) : CommitProblemWithDetails {
    override val text: String get() = StringBundle.string("secretsLeakBalloonDescription", worker.inOneList().size)

    override fun showDetails(project: Project) {
        NotifySecretsLeakAction.notifySecretsLeak(project, worker.inOneList().size)

        val toolWindowManager = project.let { ToolWindowManager.getInstance(it) }
        val todoToolWindow = toolWindowManager.getToolWindow("Secret Guard")
        todoToolWindow?.activate(null)
    }

    override fun showModalSolution(project: Project, commitInfo: CommitInfo): CheckinHandler.ReturnResult {
        val commit = MessageDialogBuilder.yesNoCancel(VcsBundle.message("checkin.commit.checks.failed"),
                VcsBundle.message("checkin.commit.checks.failed.with.error.message", text))
                .yesText(StringUtil.toTitleCase(showDetailsAction))
                .noText(commitInfo.commitActionText)
                .cancelText(VcsBundle.message("checkin.commit.checks.failed.cancel.button"))
                .show(project)
        when (commit) {
            Messages.YES -> { // review
                this.showDetails(project)
                return CheckinHandler.ReturnResult.CLOSE_WINDOW
            }

            Messages.NO -> return CheckinHandler.ReturnResult.COMMIT
            else -> return CheckinHandler.ReturnResult.CANCEL
        }
    }

    override val showDetailsAction: String
        get() = StringBundle.string("secretsLeakBalloonPositiveButton")
}

class SecretCheckinHandler(private val project: Project) : CheckinHandler(), CommitCheck, DumbAware {
    private var checkSecretGuard = false

    override fun getBeforeCheckinConfigurationPanel(): RefreshableOnComponent? {
        return BooleanCommitOption(
                project = project,
                text = "Check Secrets",
                disableWhenDumb = false,
                { checkSecretGuard },
                { checkSecretGuard = it }
        )
    }

    override fun getExecutionOrder(): CommitCheck.ExecutionOrder = CommitCheck.ExecutionOrder.POST_COMMIT

    override fun isEnabled(): Boolean = checkSecretGuard

    override suspend fun runCheck(commitInfo: CommitInfo): CommitProblem? {
        // val isPostCommit = commitInfo.commitContext.isPostCommitCheck
        // val secretFilter = settings.mySecretPanelSettings.secretFilterName?.let { SecretConfiguration.getInstance().getSecretFilter(it) }
        val changes = commitInfo.committedChanges // must be on EDT
        val worker = SecretCheckinHandlerWorker(project, changes)

        withContext(Dispatchers.Default) {
            worker.execute()
        }

        val noSecret = worker.inOneList().isEmpty()
        // val noSkipped = worker.skipped.isEmpty()
        if (noSecret) return null

        return SecretCommitProblem(worker)
    }
}

class SecretCheckinHandlerWorker(
        private val project: Project,
        val changes: List<Change>
) {
    val secretsFound = mutableListOf<SecretInfo>()

    fun execute() {
        val secretPattern = "login = (.*)".toRegex()

        changes.forEach { change ->
            val virtualFile = change.virtualFile ?: return@forEach
            if (!virtualFile.isDirectory && virtualFile.isSecretsSearchable) {
                virtualFile.toNioPath().toFile().readLines().forEachIndexed { index, line ->
                    secretPattern.find(line)?.let { matchResult ->
                        secretsFound.add(
                                SecretInfo(
                                        project,
                                        virtualFile,
                                        index + 1,
                                        matchResult.value
                                )
                        )
                    }
                }
            }
        }
    }

    fun inOneList(): List<SecretInfo> = secretsFound
}

val VirtualFile.isSecretsSearchable: Boolean
    get() = this.name.endsWith(".yml") || this.name.endsWith(".yaml") ||
            this.name.endsWith(".json") || this.name.endsWith(".kt")

data class SecretInfo(
        val project: Project,
        val virtualFile: VirtualFile,
        val lineNumber: Int,
        val secret: String
)
