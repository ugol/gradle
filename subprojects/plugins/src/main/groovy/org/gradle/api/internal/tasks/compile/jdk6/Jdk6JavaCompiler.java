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
package org.gradle.api.internal.tasks.compile.jdk6;

import org.gradle.api.internal.tasks.compile.CommandLineJavaCompilerSupport;
import org.gradle.api.internal.tasks.compile.CompilationFailedException;
import org.gradle.api.internal.tasks.compile.SimpleWorkResult;
import org.gradle.api.tasks.WorkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.nio.charset.Charset;
import java.util.List;

public class Jdk6JavaCompiler extends CommandLineJavaCompilerSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(Jdk6JavaCompiler.class);

    public WorkResult execute() {
        LOGGER.info("Compiling using JDK 6 Java Compiler API.");

        List<String> options = generateCommandLineOptions();
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, compileOptions.getEncoding() != null ? Charset.forName(compileOptions.getEncoding()) : null);
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(source);
        javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(null, null, null, options, null, compilationUnits);

        boolean success = task.call();
        if (!success) {
            throw new CompilationFailedException();
        }
        return new SimpleWorkResult(true);
    }
}
