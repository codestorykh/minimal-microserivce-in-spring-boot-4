package com.codestorykh.local;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LocalPlatformApplication {

    private static final List<LocalModule> MODULES = List.of(
            new LocalModule("config-server", 8888),
            new LocalModule("account", 8081),
            new LocalModule("screening", 8082),
            new LocalModule("payment", 8083)
    );

    static void main(String[] args) throws Exception {
        Path root = RepoLayout.root();
        String version = RepoLayout.version(root);
        Set<String> skip = skipSet();
        List<LocalModule> requested = MODULES.stream()
                .filter(module -> !skip.contains(module.artifactId()))
                .toList();

        System.out.println();
        System.out.println("local-platform  root=" + root + "  version=" + version);
        if (!skip.isEmpty()) {
            System.out.println("skip            " + String.join(", ", skip));
        }
        System.out.println();

        RepoLayout.packageIfMissing(root, version, requested);

        ServiceLauncher launcher = new ServiceLauncher(root);
        launcher.registerShutdownHook();

        for (LocalModule module : MODULES) {
            if (skip.contains(module.artifactId())) {
                continue;
            }
            Path jar = RepoLayout.jar(root, module.artifactId(), version);
            if (jar == null) {
                launcher.stopAll();
                throw new IllegalStateException("Missing jar for " + module.artifactId() + " (version " + version + ")");
            }
            launcher.start(module, jar);
        }

        System.out.println();
        System.out.println();
        launcher.watchForever();
    }

    private static Set<String> skipSet() {
        LinkedHashSet<String> skip = new LinkedHashSet<>();
        Arrays.stream(System.getProperty("alpha.local.skip", "").split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .forEach(skip::add);
        return skip;
    }
}
