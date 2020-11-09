/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */
package com.nextcode.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication

/**
 * This helper function to setup the pom configuration for projects using the 'internal' configuration.
 *
 * Pom that is created does:
 * 1. All dependencies that are not 'internal' configuration are included as normally.
 * 2. For 'internal' project dependencies we don't include the dependency directly in the pom but
 *    instead wee add dependency's dependencies.  If any of those dependencies are 'internal' we repeat the
 *    process for those.
 *
 * Based on:  https://github.com/Kotlin/dokka/commit/487dba687ad0891eebe61433c83c9ad5bf529b7b
 *
 * @param publication   the publication to corrrect.
 * @param project       the project we are correcting.
 */
static void configure(MavenPublication publication, Project project) {
    publication.artifact(project.tasks.shadowJar)

    publication.pom { MavenPom pom ->                                       
        pom.withXml { xml ->
            def dependenciesNode = xml.asNode().appendNode('dependencies')
            project.configurations.internal.allDependencies.each {
                handleInternalDependency(dependenciesNode, it)
            }
        }
    }
}

private static void handleInternalDependency(Node dependenciesNode, Dependency dependency) {
    if (dependency instanceof ProjectDependency) {
        dependency.getDependencyProject().configurations.each {
            def configName = it.name
            it.getDependencies().each {
                if (configName.equals("internal")) {
                    handleInternalDependency(dependenciesNode, it)
                } else if (configName in ['api', 'implementation', 'runTime', 'compile']) {
                    addDependency(dependenciesNode, it)
                }
            }
        }
    } else {
        addDependency(dependenciesNode, dependency)
    }
}

private static void addDependency(Node dependenciesNode, dep) {
    if (!(dep instanceof SelfResolvingDependency)) {
        if (!dependenciesNode.find { getDepKey(dep).equals(getNodeKey(it))}) {
            def dependencyNode = dependenciesNode.appendNode('dependency')
            dependencyNode.appendNode('groupId', dep.group)
            dependencyNode.appendNode('artifactId', dep.name)
            dependencyNode.appendNode('version', dep.version)
            dependencyNode.appendNode('scope', 'runtime')

            // for exclusions
            if (dep.excludeRules.size() > 0) {
                def exclusions = dependencyNode.appendNode('exclusions')
                dep.excludeRules.each { ExcludeRule ex ->
                    def exclusion = exclusions.appendNode('exclusion')
                    exclusion.appendNode('groupId', ex.group)
                    exclusion.appendNode('artifactId', ex.module)
                }
            }
        }
    }
}

private static String getDepKey(Dependency dep) {
    return "${dep.group}:${dep.name}:${dep.version}"
}

private static String getNodeKey(Node node) {
    return node.get('groupId').text() + ':' + node.get('artifactId').text()  + ':' + node.get('version').text()
}