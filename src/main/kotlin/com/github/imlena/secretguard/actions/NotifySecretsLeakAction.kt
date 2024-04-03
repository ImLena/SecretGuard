package com.github.imlena.secretguard.actions

import com.github.imlena.secretguard.StringBundle
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

const val NOTIFICATION_GROUP_ID = "SecretGuardNotificationGroup"

class NotifySecretsLeakAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        notifySecretsLeak(
                project = project,
                leaksCount = 3
        )
    }

    companion object {
        fun notifySecretsLeak(
                project: Project?,
                leaksCount: Int,
                onReviewButtonClick: () -> Unit = {
                    val toolWindowManager = project?.let { ToolWindowManager.getInstance(it) }
                    val todoToolWindow = toolWindowManager?.getToolWindow("Secret Guard")
                    todoToolWindow?.activate(null)
                }
        ) {
            NotificationGroupManager.getInstance()
                    .getNotificationGroup(NOTIFICATION_GROUP_ID)
                    .createNotification(
                            title = StringBundle.string("secretsLeakBalloonTitle"),
                            content = StringBundle.string("secretsLeakBalloonDescription", leaksCount),
                            type = NotificationType.INFORMATION
                    )
                    .addAction(NotificationAction.createSimpleExpiring(StringBundle.string("secretsLeakBalloonPositiveButton")) {
                        thisLogger().info("Review secrets clicked")
                        onReviewButtonClick()
                    })
                    .addAction(NotificationAction.createSimpleExpiring(StringBundle.string("secretsLeakBalloonDismissButton")) {
                        thisLogger().info("Ignore clicked")
                    })
                    .notify(project)
        }
    }
}