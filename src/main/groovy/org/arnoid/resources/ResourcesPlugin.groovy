package org.arnoid.resources

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class ResourcesPlugin implements Plugin<Project> {
    @Override
    void apply(Project target) {
        target.apply plugin: "java"
        target.extensions.create(ResourcesExtension.NAME, ResourcesExtension)
        Task generateResourcesTask = target.tasks.create(GenerateResourcesTask.NAME, GenerateResourcesTask)

        target.tasks.assemble*.dependsOn generateResourcesTask
    }
}
