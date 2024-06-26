package lol.koblizek.bytelens.core.utils;

import lol.koblizek.bytelens.api.util.IconifiedTreeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StandardDirectoryWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardDirectoryWatcher.class);

    private final WatchService watcher;
    private final Path dir;
    private final ExecutorService executor;
    private final IconifiedTreeItem root;
    private final Map<WatchKey, Path> keys;

    public StandardDirectoryWatcher(Path dir, IconifiedTreeItem root) throws IOException {
        this.root = root;
        this.watcher = FileSystems.getDefault().newWatchService();
        this.dir = dir;
        this.executor = Executors.newSingleThreadExecutor();
        this.keys = new HashMap<>();
        registerDir(dir);
    }

    private void registerDir(Path dir) {
        try {
            keys.put(dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE), dir);
        } catch (IOException e) {
            LOGGER.error("Exception occurred while registering directory to watcher", e);
        }
    }

    public void start() {
        executor.submit(this::processEvents);
    }

    private void processEvents() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            Path path = keys.get(key);

            for (WatchEvent<?> event : key.pollEvents()) {
                LOGGER.trace("New file event");
                WatchEvent.Kind<?> kind = event.kind();

                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                Path child = path.resolve(fileName);

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        registerDir(child);
                    }
                    addTreeItem(root, dir.relativize(child), child);
                } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    removeTreeItem(root, dir.relativize(child));
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private void addTreeItem(IconifiedTreeItem parentItem, Path relative, Path full) {
        for (Path path : relative) {
            var opt = parentItem.getChildren().stream()
                    .filter(it -> it.getValue().equals(path.toString())).findFirst();
            if (opt.isPresent())
                parentItem = (IconifiedTreeItem) opt.get();
            else {
                var nP = new IconifiedTreeItem(full);
                parentItem.getChildren().add(nP);
                parentItem = nP;
            }
        }
    }

    private void removeTreeItem(IconifiedTreeItem parentItem, Path relative) {
        for (Path path : relative) {
            parentItem = (IconifiedTreeItem) parentItem.getChildren().stream().filter(it -> it.getValue().equals(path.toString()))
                    .findFirst().orElse(null);
            assert parentItem != null;
        }
        parentItem.getParent().getChildren().remove(parentItem);
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
