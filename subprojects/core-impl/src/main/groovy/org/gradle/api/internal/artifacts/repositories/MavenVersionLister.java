/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.artifacts.repositories;

import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.gradle.api.internal.resource.ResourceException;
import org.gradle.api.internal.resource.ResourceNotFoundException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class MavenVersionLister implements VersionLister {
    private final String root;
    private final MavenMetadataLoader mavenMetadataLoader;

    public MavenVersionLister(ExternalResourceRepository repository, String root) {
        this.mavenMetadataLoader = new MavenMetadataLoader(repository);
        this.root = root;
    }

    public VersionList getVersionList(ModuleRevisionId moduleRevisionId, String pattern, Artifact artifact) throws ResourceException, ResourceNotFoundException {
        try {
            if (!pattern.endsWith(MavenPattern.M2_PATTERN)) {
                return new DefaultVersionList(Collections.<String>emptyList());
            }
            final Map attributes = moduleRevisionId.getModuleId().getAttributes();
            String metadataLocation = IvyPatternHelper.substituteTokens(root + "[organisation]/[module]/maven-metadata.xml", attributes);
            MavenMetadata mavenMetaData = mavenMetadataLoader.load(metadataLocation);
            return new DefaultVersionList(mavenMetaData.versions);
        } catch (IOException e) {
            throw new ResourceException("Unable to load Maven Metadata file", e);
        } catch (SAXException e) {
            throw new ResourceException("Unable to parse Maven Metadata file", e);
        } catch (ParserConfigurationException e) {
            throw new ResourceException("Unable to parse Maven Metadata file", e);
        }
    }
}