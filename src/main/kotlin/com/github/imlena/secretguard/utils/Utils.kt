package com.github.imlena.secretguard.utils

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase

val SIZE_KEY = DataKey.create<Int>("action.size")
fun triggerAction(project: Project, actionId: String, size: Int) {
    val actionManager = ActionManager.getInstance()
    val action = actionManager.getAction(actionId)
    val dataContext = MapDataContext()
    dataContext.put(CommonDataKeys.PROJECT, project)
    dataContext.put(SIZE_KEY, size) // Передаем параметр size

    val anActionEvent = AnActionEvent.createFromDataContext("ACTION_PLACE", null, dataContext)
    action?.actionPerformed(anActionEvent)
}

class MapDataContext : DataContext, UserDataHolder by UserDataHolderBase() {
    private val myMap = mutableMapOf<String, Any>()

    fun <T : Any> put(key: DataKey<T>, value: T) {
        myMap[key.toString()] = value
    }

    override fun getData(dataId: String): Any? {
        return myMap[dataId]
    }
}