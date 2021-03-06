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
package org.gradle.initialization;

import org.gradle.groovy.scripts.ScriptSource;
import org.gradle.groovy.scripts.UriScriptSource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class DirectoryInitScriptFinder implements InitScriptFinder {
    protected void findScriptsInDir(File initScriptsDir, Collection<ScriptSource> scripts) {
        if (!initScriptsDir.isDirectory()) {
            return;
        }
        List<File> files = new ArrayList<File>();
        for (File file : initScriptsDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".gradle")) {
                files.add(file);
            }
        }
        Collections.sort(files);
        for (File file : files) {
            scripts.add(new UriScriptSource("initialization script", file));
        }
    }
}
