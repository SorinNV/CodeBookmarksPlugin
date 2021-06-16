package com.github.sorinnv.codebookmarksplugin.services

import com.github.sorinnv.codebookmarksplugin.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
