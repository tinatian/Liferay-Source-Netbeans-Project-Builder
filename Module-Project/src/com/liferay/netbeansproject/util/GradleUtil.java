/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.netbeansproject.util;

import com.liferay.netbeansproject.container.Module.ModuleDependency;

import java.io.BufferedReader;
import java.io.IOException;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Tom Wang
 */
public class GradleUtil {

	public static String getJarDependencies(Path modulePath) throws Exception {
		String dependencies = _extractDependency(modulePath);

		Matcher projectMatcher = _projectPattern.matcher(dependencies);

		dependencies = projectMatcher.replaceAll("");

		Matcher unusedDependencyMatcher = _unusedDependencyPattern.matcher(
			dependencies);

		dependencies = unusedDependencyMatcher.replaceAll("");

		Matcher portalMatcher = _portalPattern.matcher(dependencies);

		dependencies = portalMatcher.replaceAll("");

		return _replaceKeywords(dependencies);
	}

	public static List<ModuleDependency> getModuleDependencies(Path modulePath)
		throws Exception {

		Path gradleFilePath = modulePath.resolve("build.gradle");

		if (!Files.exists(gradleFilePath)) {
			return Collections.emptyList();
		}

		List<ModuleDependency> moduleInfos = new ArrayList<>();

		try(
			BufferedReader bufferedReader = Files.newBufferedReader(
				gradleFilePath, Charset.defaultCharset())) {

			String line = null;

			while ((line = bufferedReader.readLine()) != null) {
				line = line.trim();

				if (line.startsWith("compile project") ||
					line.startsWith("provided project") ||
					line.startsWith("frontendThemes project") ||
					line.startsWith("testCompile project") ||
					line.startsWith("testIntegrationCompile project")) {

					int index1 = line.indexOf('\"');

					if (index1 < 0) {
						throw new IllegalStateException(
							"Broken syntax in " + gradleFilePath);
					}

					int index2 = line.indexOf('\"', index1 + 1);

					if (index2 < 0) {
						throw new IllegalStateException(
							"Broken syntax in " + gradleFilePath);
					}

					String moduleLocation = line.substring(index1 + 1, index2);

					boolean test = false;

					if (line.startsWith("testCompile project") ||
						line.startsWith("testIntegrationCompile project")) {

						test = true;
					}

					moduleInfos.add(new ModuleDependency(moduleLocation, test));
				}
			}
		}

		return moduleInfos;
	}

	private static String _extractDependency(Path modulePath)
		throws IOException {

		Path buildGradlePath = modulePath.resolve("build.gradle");

		if (!Files.exists(buildGradlePath)) {
			return "";
		}

		String content = new String(Files.readAllBytes(buildGradlePath));

		StringBuilder sb = new StringBuilder();

		Matcher dependencyMatcher = _dependencyPattern.matcher(content);

		while (dependencyMatcher.find()) {
			sb.append(dependencyMatcher.group(0));
			sb.append('\n');
		}

		return sb.toString();
	}

	private static String _replaceKeywords(String dependencies) {
		dependencies = StringUtil.replace(dependencies, "optional, ", "");
		dependencies = StringUtil.replace(
			dependencies, "antlr group", "compile group");
		dependencies = StringUtil.replace(
			dependencies, "jarjar group", "compile group");
		dependencies = StringUtil.replace(
			dependencies, "jruby group", "compile group");
		dependencies = StringUtil.replace(
			dependencies, "jnaerator classifier: \"shaded\",", "compile");
		dependencies = StringUtil.replace(dependencies, "provided", "compile");
		dependencies = StringUtil.replace(
			dependencies, "testIntegrationCompile", "testCompile");

		dependencies = StringUtil.replace(dependencies, "dependencies {", "");
		dependencies = StringUtil.replace(dependencies, "}", "");

		return StringUtil.replace(
			dependencies, "testCompile", "testConfiguration");
	}

	private static final Pattern _dependencyPattern = Pattern.compile(
		"dependencies(\\s*)\\{[^}]*}");
	private static final Pattern _portalPattern = Pattern.compile(
		"\t(compile|provided|testCompile|testIntegrationCompile)\\s*group:" +
			"\\s\"com\\.liferay\\.portal\".*\\n");
	private static final Pattern _projectPattern = Pattern.compile(
		"\t(compile|provided|testCompile|testIntegrationCompile|" +
			"frontendThemes)\\s*project.*\\n");
	private static final Pattern _unusedDependencyPattern = Pattern.compile(
		"\tconfigAdmin\\s*group.*\\n");

}