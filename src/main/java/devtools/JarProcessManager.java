package devtools;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages JAR process lifecycle - starting and terminating JAR applications
 */
@Getter
@Builder
public class JarProcessManager {

    private Process process;
    private final String jarPath;

    @Singular
    private final List<String> jvmArgs;

    @Builder.Default
    private boolean useNohup = false;

    @Builder.Default
    private String nohupLogFile = "server.log";

    /**
     * Start the JAR process
     */
    public boolean start(String... programArgs) {
        if (isRunning()) {
            return false;
        }

        try {
            List<String> command = buildCommand(programArgs);
            ProcessBuilder builder = new ProcessBuilder(command);

            if (!useNohup) {
                builder.inheritIO();
            } else {
                builder.redirectOutput(new File(nohupLogFile));
                builder.redirectError(ProcessBuilder.Redirect.appendTo(new File(nohupLogFile)));
            }

            process = builder.start();
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Kill the process gracefully (SIGTERM) with timeout fallback
     */
    public boolean kill(long timeoutSeconds) {
        if (!isRunning()) {
            return true;
        }

        try {
            process.destroy();

            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor();
            }

            process = null;
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Force kill immediately
     */
    public boolean forceKill() {
        if (!isRunning()) {
            return true;
        }

        process.destroyForcibly();
        process = null;
        return true;
    }

    /**
     * Check if process is running
     */
    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    /**
     * Get process exit code (blocks until process terminates)
     */
    public int waitForExit() throws InterruptedException {
        if (process == null) {
            throw new IllegalStateException("Process not started");
        }
        return process.waitFor();
    }

    private List<String> buildCommand(String... programArgs) {
        List<String> command = new ArrayList<>();

        if (useNohup) {
            command.add("nohup");
        }

        command.add("java");
        command.addAll(jvmArgs);
        command.add("-jar");
        command.add(jarPath);

        Collections.addAll(command, programArgs);

        if (useNohup) {
            command.add("&");
        }

        return command;
    }
}