package com.facebook.swift.parser;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import org.testng.ITestContext;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.Files.walkFileTree;

public class TestingUtils
{
    public static Path getResourcePath(String resourceName)
    {
        try {
            return Paths.get(Resources.getResource(resourceName).toURI());
        }
        catch (URISyntaxException e) {
            throw new AssertionError(e);
        }
    }

    public static List<Path> listMatchingFiles(Path start, String glob)
            throws IOException
    {
        final ImmutableList.Builder<Path> list = ImmutableList.builder();
        final PathMatcher matcher = start.getFileSystem().getPathMatcher("glob:" + glob);
        walkFileTree(start, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                if (matcher.matches(file)) {
                    list.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return list.build();
    }

    public static String getTestParameter(ITestContext context, String parameterName)
    {
        String value = context.getCurrentXmlTest().getParameter(parameterName);
        return checkNotNull(value, "test parameter not set: %s", parameterName);
    }

    public static Iterator<Object[]> listDataProvider(Object... list)
    {
        return listDataProvider(Arrays.asList(list));
    }

    public static Iterator<Object[]> listDataProvider(List<?> list)
    {
        return Lists.transform(list, new Function<Object, Object[]>()
        {
            @Override
            public Object[] apply(@Nullable Object input)
            {
                return new Object[] {input};
            }
        }).iterator();
    }
}