package com.codestorykh.local;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

final class RepoLayout {

    private static final Pattern PROJECT_VERSION = Pattern.compile(
            "<artifactId>minimal-microserivce-in-spring-boot-4</artifactId>\\s*<version>([^<]+)</version>"
    );

    private RepoLayout() {
    }

    static Path root() {
        String override = System.getProperty("alpha.local.backend", "").trim();
        if (!override.isEmpty()) {
            Path path = Path.of(override).toAbsolutePath().normalize();
            if (Files.isDirectory(path)) {
                return path;
            }
            throw new IllegalStateException("alpha.local.backend is not a directory: " + path);
        }
        Path cwd = Path.of("").toAbsolutePath().normalize();
        for (Path candidate : List.of(cwd, cwd.getParent())) {
            if (candidate != null && Files.isDirectory(candidate.resolve("account"))) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Run from the repo root, or set -Dalpha.local.backend=/path/to/repo"
        );
    }

    static String version(Path root) {
        String override = System.getProperty("alpha.local.version", "").trim();
        if (!override.isEmpty()) {
            return override;
        }
        Path pom = root.resolve("pom.xml");
        try {
            String xml = Files.readString(pom);
            var matcher = PROJECT_VERSION.matcher(xml);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read " + pom, ex);
        }
        throw new IllegalStateException("Unable to find project version in " + pom);
    }

    static Path jar(Path root, String artifactId, String version) {
        Path exact = root.resolve(artifactId).resolve("target").resolve(artifactId + "-" + version + ".jar");
        if (Files.isRegularFile(exact)) {
            return exact;
        }
        Path target = root.resolve(artifactId).resolve("target");
        if (!Files.isDirectory(target)) {
            return null;
        }
        try (Stream<Path> files = Files.list(target)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> isRunnableJar(artifactId, path.getFileName().toString()))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ex) {
            return null;
        }
    }

    static void packageIfMissing(Path root, String version, List<LocalModule> modules)
            throws InterruptedException, IOException {
        boolean missing = modules.stream().anyMatch(module -> jar(root, module.artifactId(), version) == null);
        boolean force = Boolean.parseBoolean(System.getProperty("alpha.local.repackage", "false"));
        if (!missing && !force) {
            return;
        }
        String moduleList = String.join(",", modules.stream().map(LocalModule::artifactId).toList());
        List<String> command = new ArrayList<>();
        command.add("mvn");
        if (force) {
            command.add("clean");
        } else {
            command.add("-q");
        }
        command.add("package");
        command.add("-pl");
        command.add(moduleList);
        command.add("-am");
        command.add("-DskipTests");
        Process process = new ProcessBuilder(command).directory(root.toFile()).inheritIO().start();
        if (process.waitFor() != 0) {
            throw new IllegalStateException("Maven package failed for: " + moduleList);
        }
    }

    private static boolean isRunnableJar(String artifactId, String name) {
        return name.startsWith(artifactId + "-")
                && name.endsWith(".jar")
                && !name.endsWith(".original")
                && !name.contains("-sources")
                && !name.contains("-javadoc");
    }
}
