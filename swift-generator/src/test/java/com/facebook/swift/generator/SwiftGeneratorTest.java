/*
 * Copyright (C) 2012 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.facebook.swift.generator;

import com.facebook.swift.testing.TestingUtils;
import java.nio.file.Path;
import java.util.Iterator;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static com.facebook.swift.testing.TestingUtils.getResourcePath;
import static com.facebook.swift.testing.TestingUtils.listDataProvider;
import static com.facebook.swift.testing.TestingUtils.listMatchingFiles;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.FileAssert.fail;
import org.testng.annotations.BeforeClass;

public class SwiftGeneratorTest {

    private static final String outputFolderRoot = System.getProperty("test.output.directory");

    @BeforeClass
    public static void ensureRootFolder() {
        Path outputPath = Paths.get(outputFolderRoot);

        // Clean up output if it already exists
        if (Files.isDirectory(outputPath)) {
            try {
                TestingUtils.deleteRecursively(outputPath);
            } catch (IOException ex) {
                fail("Unable to delete Swift output directory "
                        + outputFolderRoot
                        + " due to " + ex);
            }
        }

        if (!outputPath.toFile().mkdirs()) {
            fail("Unable to create Swift output directory " + outputFolderRoot);
        }
    }

    @DataProvider
    public Iterator<Object[]> thriftProvider()
            throws Exception {
        return listDataProvider(listMatchingFiles(getResourcePath(""), "**/*.thrift"));
    }

    @Test(dataProvider = "thriftProvider")
    public void testGenerate(Path path) throws Exception {

        // Create a nice output directory for these generated files
        Path rootPath = getResourcePath("");
        Path relativePath = rootPath.relativize(path);
        String testPath = relativePath.toString().replace(
                relativePath.getFileSystem().getSeparator(),
                "_");
        File outputDirectory = new File(outputFolderRoot, testPath);
        File sourceDirectory = new File(outputDirectory, "source");
        File classesDirectory = new File(outputDirectory, "classes");

        final SwiftGeneratorConfig config = SwiftGeneratorConfig.builder()
                .inputBase(Resources.getResource(getClass(), "/").toURI())
                .outputFolder(sourceDirectory)
                .generateIncludedCode(true)
                .codeFlavor("java-immutable")
                .defaultPackage("com.facebook.swift")
                .addTweak(SwiftGeneratorTweak.ADD_CLOSEABLE_INTERFACE)
                .addTweak(SwiftGeneratorTweak.EXTEND_RUNTIME_EXCEPTION)
                .addTweak(SwiftGeneratorTweak.ADD_THRIFT_EXCEPTION)
                .addTweak(SwiftGeneratorTweak.USE_PLAIN_JAVA_NAMESPACE)
                .build();

        final SwiftGenerator generator = new SwiftGenerator(config);
        generator.parse(Collections.singletonList(path.toUri()));

        assertCompilation(
                sourceDirectory.toPath(),
                classesDirectory.toPath());
    }

    private void assertCompilation(Path sourceDirectory, Path outputDirectory) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        List<File> files = TestingUtils
                .listMatchingFiles(
                        sourceDirectory,
                        "**/*.java")
                .stream()
                .map(p -> p.toFile())
                .collect(Collectors.toList());

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.
                getStandardFileManager(diagnostics, null, null);
        
        // Make sure the output directory exists
        outputDirectory.toFile().mkdirs();

        CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                Arrays.asList(
                        "-d",
                        outputDirectory.toAbsolutePath().toString()),
                null,
                fileManager.
                getJavaFileObjectsFromFiles(files));

        task.call();
        
        // Make sure no errors
        assertEquals(
                0, 
                diagnostics.getDiagnostics()
                .stream()
                .filter(e -> e.getKind() == Kind.ERROR)
                .count());
    }

}
