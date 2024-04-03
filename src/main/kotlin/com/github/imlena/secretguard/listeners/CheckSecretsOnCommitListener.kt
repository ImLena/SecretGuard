package com.github.imlena.secretguard.listeners;

import com.github.imlena.secretguard.types.SecretInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.changes.CommitExecutor
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.tools.ExternalToolsCheckinHandlerFactory
import com.intellij.util.PairConsumer
import org.jetbrains.annotations.NotNull
import java.io.File

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
                val securityBugs = findSecrets(panel.project)
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
                        val toolWindow = toolWindowManager.getToolWindow("ReviewSecrets")
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
            true
        }

        return secretsFound
    }

}