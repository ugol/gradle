/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.api.tasks.wrapper;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.plugins.StartScriptGenerator;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.*;
import org.gradle.wrapper.GradleWrapperMain;
import org.gradle.wrapper.Install;
import org.gradle.wrapper.WrapperExecutor;

import java.io.File;
import java.net.URL;
import java.util.Properties;

/**
 * <p>Generates scripts (for *nix and windows) which allow you to build your project with Gradle, without having to
 * install Gradle.
 *
 * <p>When a user executes a wrapper script the first time, the script downloads and installs the appropriate Gradle
 * distribution and runs the build against this downloaded distribution. Any installed Gradle distribution is ignored
 * when using the wrapper scripts.
 *
 * <p>The scripts generated by this task are intended to be committed to your version control system. This task also
 * generates a small {@code gradle-wrapper.jar} bootstrap JAR file and properties file which should also be committed to
 * your VCS. The scripts delegates to this JAR.
 *
 * @author Hans Dockter
 */
public class Wrapper extends DefaultTask {
    public static final String DEFAULT_DISTRIBUTION_PARENT_NAME = Install.DEFAULT_DISTRIBUTION_PATH;
    public static final String DEFAULT_ARCHIVE_NAME = "gradle";
    public static final String DEFAULT_ARCHIVE_CLASSIFIER = "bin";

    private String distributionUrl;

    /**
     * Specifies how the wrapper path should be interpreted.
     */
    public enum PathBase {
        PROJECT, GRADLE_USER_HOME
    }

    private Object scriptFile;
    private Object jarFile;

    @Input
    private String distributionPath;

    @Input
    private PathBase distributionBase = PathBase.GRADLE_USER_HOME;

    private String archiveName;

    private String archiveClassifier;

    private GradleVersion gradleVersion;

    private String urlRoot;

    @Input
    private String archivePath;

    @Input
    private PathBase archiveBase = PathBase.GRADLE_USER_HOME;

    private final DistributionLocator locator = new DistributionLocator();

    public Wrapper() {
        scriptFile = "gradlew";
        jarFile = "gradle/wrapper/gradle-wrapper.jar";
        distributionPath = DEFAULT_DISTRIBUTION_PARENT_NAME;
        archiveName = DEFAULT_ARCHIVE_NAME;
        archiveClassifier = DEFAULT_ARCHIVE_CLASSIFIER;
        archivePath = DEFAULT_DISTRIBUTION_PARENT_NAME;
        gradleVersion = GradleVersion.current();
    }

    @TaskAction
    void generate() {
        File jarFileDestination = getJarFile();
        File unixScript = getScriptFile();
        FileResolver resolver = getServices().get(FileResolver.class).withBaseDir(unixScript.getParentFile());
        String jarFileRelativePath = resolver.resolveAsRelativePath(jarFileDestination);

        writeProperties(getPropertiesFile());

        URL jarFileSource = Wrapper.class.getResource("/gradle-wrapper.jar");
        if (jarFileSource == null) {
            throw new GradleException("Cannot locate wrapper JAR resource.");
        }
        GFileUtils.copyURLToFile(jarFileSource, jarFileDestination);

        StartScriptGenerator generator = new StartScriptGenerator();
        generator.setApplicationName("Gradle");
        generator.setMainClassName(GradleWrapperMain.class.getName());
        generator.setClasspath(WrapUtil.toList(jarFileRelativePath));
        generator.setOptsEnvironmentVar("GRADLE_OPTS");
        generator.setExitEnvironmentVar("GRADLE_EXIT_CONSOLE");
        generator.setAppNameSystemProperty("org.gradle.appname");
        generator.setScriptRelPath(unixScript.getName());
        generator.generateUnixScript(unixScript);
        generator.generateWindowsScript(getBatchScript());
    }

    private void writeProperties(File propertiesFileDestination) {
        Properties wrapperProperties = new Properties();
        wrapperProperties.put(WrapperExecutor.DISTRIBUTION_URL_PROPERTY, getDistributionUrl());
        wrapperProperties.put(WrapperExecutor.DISTRIBUTION_BASE_PROPERTY, distributionBase.toString());
        wrapperProperties.put(WrapperExecutor.DISTRIBUTION_PATH_PROPERTY, distributionPath);
        wrapperProperties.put(WrapperExecutor.ZIP_STORE_BASE_PROPERTY, archiveBase.toString());
        wrapperProperties.put(WrapperExecutor.ZIP_STORE_PATH_PROPERTY, archivePath);
        GUtil.saveProperties(wrapperProperties, propertiesFileDestination);
    }

    /**
     * Returns the file to write the wrapper script to.
     */
    @OutputFile
    public File getScriptFile() {
        return getProject().file(scriptFile);
    }

    public void setScriptFile(Object scriptFile) {
        this.scriptFile = scriptFile;
    }

    /**
     * Returns the file to write the wrapper batch script to.
     */
    @OutputFile
    public File getBatchScript() {
        File scriptFile = getScriptFile();
        return new File(scriptFile.getParentFile(), scriptFile.getName().replaceFirst("(\\.[^\\.]+)?$", ".bat"));
    }

    /**
     * Returns the script destination path, relative to the project directory.
     *
     * @see #setScriptDestinationPath(String)
     */
    @Deprecated
    public String getScriptDestinationPath() {
        DeprecationLogger.nagUserOfReplacedMethod("Wrapper.getScriptDestinationPath()", "getScriptFile()");
        return getProject().relativePath(getScriptFile().getParentFile());
    }

    /**
     * Specifies a path as the parent dir of the scripts which are generated when executing the wrapper task. This path
     * specifies a directory <i>relative</i> to the project dir.  Defaults to empty string, i.e. the scripts are placed
     * into the project root dir.
     *
     * @param scriptDestinationPath Any object which <code>toString</code> method specifies the path. Most likely a
     * String or File object.
     */
    @Deprecated
    public void setScriptDestinationPath(String scriptDestinationPath) {
        DeprecationLogger.nagUserOfReplacedMethod("Wrapper.setScriptDestinationPath()", "setScriptFile()");
        setScriptFile(scriptDestinationPath + "/gradlew");
    }

    /**
     * Returns the file to write the wrapper jar file to.
     */
    @OutputFile
    public File getJarFile() {
        return getProject().file(jarFile);
    }

    public void setJarFile(Object jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * Returns the file to write the wrapper properties to.
     */
    @OutputFile
    public File getPropertiesFile() {
        File jarFileDestination = getJarFile();
        return new File(jarFileDestination.getParentFile(), jarFileDestination.getName().replaceAll("\\.jar$",
                ".properties"));
    }

    /**
     * Returns the jar path, relative to the project directory.
     *
     * @see #setJarPath(String)
     */
    @Deprecated
    public String getJarPath() {
        DeprecationLogger.nagUserOfReplacedMethod("Wrapper.getJarPath()", "getJarFile()");
        return getProject().relativePath(getJarFile().getParentFile());
    }

    /**
     * When executing the wrapper task, the jar path specifies the path where the gradle-wrapper.jar is copied to. The
     * jar path must be a path relative to the project dir. The gradle-wrapper.jar must be submitted to your version
     * control system. Defaults to empty string, i.e. the jar is placed into the project root dir.
     */
    @Deprecated
    public void setJarPath(String jarPath) {
        DeprecationLogger.nagUserOfReplacedMethod("Wrapper.setJarPath()", "setJarFile()");
        setJarFile(jarPath + "/gradle-wrapper.jar");
    }

    /**
     * Returns the path where the gradle distributions needed by the wrapper are unzipped. The path is relative to the
     * distribution base directory
     *
     * @see #setDistributionPath(String)
     */
    public String getDistributionPath() {
        return distributionPath;
    }

    /**
     * Sets the path where the gradle distributions needed by the wrapper are unzipped. The path is relative to the
     * distribution base directory
     *
     * @see #setDistributionPath(String)
     */
    public void setDistributionPath(String distributionPath) {
        this.distributionPath = distributionPath;
    }

    /**
     * Returns the gradle version for the wrapper.
     *
     * @see #setGradleVersion(String)
     */
    public String getGradleVersion() {
        return gradleVersion.getVersion();
    }

    /**
     * The version of the gradle distribution required by the wrapper. This is usually the same version of Gradle you
     * use for building your project.
     */
    public void setGradleVersion(String gradleVersion) {
        this.gradleVersion = GradleVersion.version(gradleVersion);
    }

    /**
     * The URL to download the gradle distribution from.
     *
     * <p>If not set, the download URL is assembled by the pattern: <code>[urlRoot]/[archiveName]-[gradleVersion]-[archiveClassifier].zip</code>
     *
     * <p>The wrapper downloads a certain distribution only once and caches it. If your distribution base is the
     * project, you might submit the distribution to your version control system. That way no download is necessary at
     * all. This might be in particular interesting, if you provide a custom gradle snapshot to the wrapper, because you
     * don't need to provide a download server then.
     */
    @Input
    public String getDistributionUrl() {
        if (distributionUrl != null) {
            return distributionUrl;
        }
        return locator.getDistribution(getUrlRoot(), gradleVersion, archiveName, archiveClassifier);
    }

    public void setDistributionUrl(String url) {
        this.distributionUrl = url;
    }

    /**
     * The base URL to download the gradle distribution from.
     *
     * <p>The download URL is assembled by the pattern: <code>[urlRoot]/[archiveName]-[gradleVersion]-[archiveClassifier].zip</code>
     */
    @Deprecated
    public String getUrlRoot() {
        if (urlRoot != null) {
            return urlRoot;
        }
        return locator.getDistributionRepository(gradleVersion);
    }

    /**
     * Sets the base URL to download the gradle distribution from.
     */
    @Deprecated
    public void setUrlRoot(String urlRoot) {
        DeprecationLogger.nagUserOfReplacedMethod("Wrapper.setUrlRoot()", "setDistributionUrl()");
        this.urlRoot = urlRoot;
    }

    /**
     * The distribution base specifies whether the unpacked wrapper distribution should be stored in the project or in
     * the gradle user home dir.
     */
    public PathBase getDistributionBase() {
        return distributionBase;
    }

    /**
     * The distribution base specifies whether the unpacked wrapper distribution should be stored in the project or in
     * the gradle user home dir.
     */
    public void setDistributionBase(PathBase distributionBase) {
        this.distributionBase = distributionBase;
    }

    /**
     * Returns the path where the gradle distributions archive should be saved (i.e. the parent dir). The path is
     * relative to the archive base directory.
     */
    public String getArchivePath() {
        return archivePath;
    }

    /**
     * Set's the path where the gradle distributions archive should be saved (i.e. the parent dir). The path is relative
     * to the parent dir specified with {@link #getArchiveBase()}.
     */
    public void setArchivePath(String archivePath) {
        this.archivePath = archivePath;
    }

    /**
     * The archive base specifies whether the unpacked wrapper distribution should be stored in the project or in the
     * gradle user home dir.
     */
    public PathBase getArchiveBase() {
        return archiveBase;
    }

    /**
     * The archive base specifies whether the unpacked wrapper distribution should be stored in the project or in the
     * gradle user home dir.
     */
    public void setArchiveBase(PathBase archiveBase) {
        this.archiveBase = archiveBase;
    }

    /**
     * The name of the archive as part of the download URL.
     *
     * <p>The download URL is assembled by the pattern: <code>[urlRoot]/[archiveName]-[gradleVersion]-[archiveClassifier].zip</code>
     *
     * <p>The default for the archive name is {@value #DEFAULT_ARCHIVE_NAME}.
     */
    @Deprecated
    public String getArchiveName() {
        return archiveName;
    }

    @Deprecated
    public void setArchiveName(String archiveName) {
        DeprecationLogger.nagUserOfReplacedMethod("Wrapper.setArchiveName()", "setDistributionUrl()");
        this.archiveName = archiveName;
    }

    /**
     * The classifier of the archive as part of the download URL.
     *
     * <p>The download URL is assembled by the pattern: <code>[urlRoot]/[archiveName]-[gradleVersion]-[archiveClassifier].zip</code>
     *
     * <p>The default for the archive classifier is {@value #DEFAULT_ARCHIVE_CLASSIFIER}.
     */
    @Deprecated
    public String getArchiveClassifier() {
        return archiveClassifier;
    }

    @Deprecated
    public void setArchiveClassifier(String archiveClassifier) {
        DeprecationLogger.nagUserOfReplacedMethod("Wrapper.setArchiveClassifier()", "setDistributionUrl()");
        this.archiveClassifier = archiveClassifier;
    }
}
