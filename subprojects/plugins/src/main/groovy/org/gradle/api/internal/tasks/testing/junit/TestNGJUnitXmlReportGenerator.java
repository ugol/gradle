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

package org.gradle.api.internal.tasks.testing.junit;

import org.gradle.api.internal.tasks.testing.TestCompleteEvent;
import org.gradle.api.internal.tasks.testing.TestDescriptorInternal;
import org.gradle.api.internal.tasks.testing.TestResultProcessor;
import org.gradle.api.internal.tasks.testing.TestStartEvent;
import org.gradle.api.internal.tasks.testing.junit.result.XmlTestsuite;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestOutputEvent;
import org.gradle.internal.TimeProvider;
import org.gradle.internal.TrueTimeProvider;

import java.io.File;
import java.util.*;

import static java.util.Collections.emptySet;

public class TestNGJUnitXmlReportGenerator implements TestResultProcessor {

    private final File testResultsDir;

    private Map<Object, TestDescriptorInternal> tests = new HashMap<Object, TestDescriptorInternal>();
    private Map<String, XmlTestsuite> testSuites = new HashMap<String, XmlTestsuite>();
    private Map<Object, Set<Throwable>> failures = new HashMap<Object, Set<Throwable>>();

    private TimeProvider timeProvider = new TrueTimeProvider();

    public TestNGJUnitXmlReportGenerator(File testResultsDir) {
        this.testResultsDir = testResultsDir;
    }

    public void started(TestDescriptorInternal test, TestStartEvent event) {
        //it would be nice if we didn't have to maintain the testId->descriptor map
        if (!tests.containsKey(test.getId())) {
            tests.put(test.getId(), test);
        }

        if (!test.isComposite()) { //test method
            if (!testSuites.containsKey(test.getClassName())) {
                testSuites.put(test.getClassName(), new XmlTestsuite(testResultsDir, test.getClassName(), timeProvider.getCurrentTime()));
            }
        }
    }

    public void completed(final Object testId, final TestCompleteEvent event) {
        final TestDescriptorInternal test = tests.remove(testId);
        if (!test.isComposite()) { //test method
            XmlTestsuite xmlTestsuite = testSuites.get(test.getClassName());
            Collection<Throwable> failures = this.failures.containsKey(testId) ? this.failures.remove(testId) : (Collection) emptySet();
            xmlTestsuite.addTestCase(test.getName(), event.getResultType(), 0, failures);
        } else if (test.getParent() == null) {
            //we can reuse the same XmlReportGenerator for JUnit fairly easily
            for (XmlTestsuite xmlTestsuite : testSuites.values()) {
                xmlTestsuite.writeSuiteData(0);
            }
            testSuites.clear();
        }
    }

    public void output(Object testId, TestOutputEvent event) {
        TestDescriptor test = tests.get(testId);
        if (testSuites.containsKey(test.getClassName())) {
            //if the suite does not exist it means the suite has already completed and we have received the output
            testSuites.get(test.getClassName()).addOutput(event.getDestination(), event.getMessage());
        }
    }

    public void failure(Object testId, Throwable failure) {
        if (!failures.containsKey(testId)) {
            failures.put(testId, new LinkedHashSet<Throwable>());
        }
        failures.get(testId).add(failure);
    }
}
