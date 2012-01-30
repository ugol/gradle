/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.plugins.quality

import org.gradle.api.file.FileCollection
import org.gradle.api.Project

/**
 * Configuration options for the PMD plugin.
 *
 * @see PmdPlugin
 */
class PmdExtension extends CodeQualityExtension {
    PmdExtension(Project project) {
        super(project)
    }

    /**
     *
     * The built-in rule sets to be used. See the <a href="http://pmd.sourceforge.net/rules/index.html">official list</a> of built-in rule sets.
     * Defaults to <tt>basic</tt>.
     * 
     * Example: ruleSets = ["basic", "braces"]
     */
    List<String> ruleSets
    
    /**
     * Convenience method for adding rule sets.
     *
     * Example: ruleSets "basic", "braces"
     *
     * @param ruleSets the rule sets to be added
     */
    void ruleSets(String... ruleSets) {
        this.ruleSets.addAll(ruleSets)
    }

    /**
     * The custom rule set files to be used. See the <a href="http://pmd.sourceforge.net/howtomakearuleset.html">official documentation</a> for
     * how to author a rule set file. Defaults to <tt>none</tt>.
     *
     * Example: ruleSetFiles = files("config/pmd/myRuleSet.xml")
     *
     */
    FileCollection ruleSetFiles

    /**
     * Convenience method for adding rule set files.
     *
     * Example: ruleSetFiles "config/pmd/myRuleSet.xml"
     *
     * @param ruleSetFiles the rule set files to be added
     */
    void ruleSetFiles(Object... ruleSetFiles) {
        this.ruleSetFiles.add(project.files(ruleSetFiles))
    }

    /**
     * The directory in which XML reports will be saved. Defaults to <tt>$reporting.baseDir/pmd</tt>.
     */
    File xmlReportsDir

    /**
     * The directory in which HTML reports will be saved. Defaults to <tt>$reporting.baseDir/pmd</tt>.
     */
    File htmlReportsDir
}
