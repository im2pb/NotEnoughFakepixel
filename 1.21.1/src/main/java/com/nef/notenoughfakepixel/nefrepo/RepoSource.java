package com.nef.notenoughfakepixel.nefrepo;

import com.nef.notenoughfakepixel.utils.AtomicFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class RepoSource<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger("notenoughfakepixel.repo");
    private static final long MAX_AGE = Duration.ofHours(24).toMillis();
    private static final long RETRY_DELAY = Duration.ofMinutes(1).toMillis();
    private static final int MAX_BYTES = 256 * 1024;
    private static final int TIMEOUT = 5_000;

    public enum Status {
        LOADING, READY, UNAVAILABLE
    }

    public record Snapshot<T>(Status status, T data) {
    }

    private final Path cache;
    private final URI url;
    private final Function<String, T> parse;
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Snapshot<T> snapshot = new Snapshot<>(Status.LOADING, null);
    private volatile long nextRefresh;
    private boolean checkedDisk;

    RepoSource(Path cache, URI url, Function<String, T> parse) {
        this.cache = cache;
        this.url = url;
        this.parse = parse;
    }

    public Snapshot<T> snapshot() {
        return snapshot;
    }

    void refreshIfDue(Executor executor) {
        if (System.currentTimeMillis() < nextRefresh || !refreshing.compareAndSet(false, true)) return;
        if (System.currentTimeMillis() < nextRefresh) {
            refreshing.set(false);
            return;
        }
        try {
            executor.execute(this::refresh);
        } catch (RejectedExecutionException ignored) {
            refreshing.set(false);
        }
    }

    private void refresh() {
        try {
            if (!checkedDisk) {
                checkedDisk = true;
                long cachedAt = loadCache();
                if (cachedAt > 0) {
                    long due = Math.min(cachedAt, System.currentTimeMillis()) + MAX_AGE;
                    if (due > System.currentTimeMillis()) {
                        nextRefresh = due;
                        return;
                    }
                }
            }

            String json = download();
            publish(json);
            nextRefresh = System.currentTimeMillis() + MAX_AGE;
            try {
                AtomicFiles.writeString(cache, json);
            } catch (IOException exception) {
                LOGGER.warn("Could not save NEF repository cache {}", cache, exception);
            }
        } catch (IOException | RuntimeException exception) {
            nextRefresh = System.currentTimeMillis() + RETRY_DELAY;
            if (snapshot.data() == null) snapshot = new Snapshot<>(Status.UNAVAILABLE, null);
            LOGGER.warn("Could not refresh NEF repository feed {}", url, exception);
        } finally {
            refreshing.set(false);
        }
    }

    private long loadCache() {
        try {
            long cachedAt = Files.getLastModifiedTime(cache).toMillis();
            try (var input = Files.newInputStream(cache)) {
                publish(readBody(input));
            }
            return cachedAt;
        } catch (NoSuchFileException ignored) {
            return 0;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read NEF repository cache {}", cache, exception);
            return 0;
        }
    }

    private void publish(String json) {
        T parsed = Objects.requireNonNull(parse.apply(json));
        if (!parsed.equals(snapshot.data())) snapshot = new Snapshot<>(Status.READY, parsed);
    }

    private String download() throws IOException {
        var connection = (HttpURLConnection) url.toURL().openConnection();
        try {
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "NotEnoughFakepixel Fabric/1.21.1");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) throw new IOException("HTTP " + status);

            try (var input = connection.getInputStream()) {
                return readBody(input);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String readBody(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(MAX_BYTES + 1);
        if (bytes.length > MAX_BYTES) throw new IOException("Repository feed exceeds size limit");
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
