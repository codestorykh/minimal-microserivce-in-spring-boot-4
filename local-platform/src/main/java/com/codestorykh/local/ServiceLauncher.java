package com.codestorykh.local;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class ServiceLauncher {

    private final Path java;
    private final Path root;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private final List<ChildProcess> children = new ArrayList<>();

    ServiceLauncher(Path root) {
        this.root = root;
        this.java = Path.of(System.getProperty("java.home"), "bin", "java");
    }

    void start(LocalModule module, Path jar) throws Exception {
        ensurePortFree(module.port());
        Process process = new ProcessBuilder(java.toString(), "-jar", jar.toString())
                .directory(root.toFile())
                .inheritIO()
                .start();
        children.add(new ChildProcess(module, process));
        System.out.println("start " + module.artifactId() + " :http://localhost:" + module.port());
        waitUntilHealthy(module, process);
        System.out.println("up    " + module.artifactId());
    }

    void watchForever() throws InterruptedException {
        while (true) {
            for (ChildProcess child : children) {
                if (!child.process().isAlive()) {
                    stopAll();
                    throw new IllegalStateException(
                            child.module().artifactId() + " exited with code " + child.process().exitValue()
                    );
                }
            }
            Thread.sleep(2000);
        }
    }

    void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopAll, "local-platform-shutdown"));
    }

    void stopAll() {
        for (ChildProcess child : children) {
            child.process().destroy();
        }
        for (ChildProcess child : children) {
            try {
                child.process().waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            if (child.process().isAlive()) {
                child.process().destroyForcibly();
            }
        }
    }

    private void waitUntilHealthy(LocalModule module, Process process) throws InterruptedException {
        URI health = URI.create("http://127.0.0.1:" + module.port() + "/actuator/health");
        HttpRequest request = HttpRequest.newBuilder(health).timeout(Duration.ofSeconds(2)).GET().build();
        long deadline = System.nanoTime() + Duration.ofSeconds(120).toNanos();
        while (System.nanoTime() < deadline) {
            if (!process.isAlive()) {
                throw new IllegalStateException(module.artifactId() + " exited before /actuator/health was UP");
            }
            try {
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200 && response.body() != null && response.body().contains("UP")) {
                    return;
                }
            } catch (IOException ignored) {
            }
            Thread.sleep(1000);
        }
        throw new IllegalStateException(module.artifactId() + " not healthy after 120s: " + health);
    }

    private void ensurePortFree(int port) throws IOException, InterruptedException {
        if (portFree(port)) {
            return;
        }
        if (!Boolean.parseBoolean(System.getProperty("alpha.local.reclaim-ports", "true"))) {
            throw new IllegalStateException("Port " + port + " is in use. Stop the other process or set -Dalpha.local.reclaim-ports=true");
        }
        killListenersOnPort(port);
        for (int attempt = 0; attempt < 25; attempt++) {
            if (portFree(port)) {
                return;
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Port " + port + " is still in use. Try: lsof -nP -iTCP:" + port + " -sTCP:LISTEN");
    }

    private void killListenersOnPort(int port) throws IOException, InterruptedException {
        if (!Files.isExecutable(Path.of("/usr/sbin/lsof"))) {
            return;
        }
        Process process = new ProcessBuilder("lsof", "-ti", "tcp:" + port)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor(3, TimeUnit.SECONDS);
        long self = ProcessHandle.current().pid();
        for (String line : output.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                long pid = Long.parseLong(trimmed);
                if (pid != self) {
                    ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static boolean portFree(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("127.0.0.1", port));
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private record ChildProcess(LocalModule module, Process process) {
    }
}
