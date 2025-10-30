package net.leberfinger;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSplittingWriter implements AutoCloseable {

    private final ImmutableList<BufferedWriter> writers;
    private final int splitFactor;
    private final ImmutableList<Path> outFiles;

    public FileSplittingWriter(Path origFile, int splitFactor) throws IOException {
        this.splitFactor = splitFactor;
        MutableList<BufferedWriter> writers = Lists.mutable.empty();
        MutableList<Path> outFiles = Lists.mutable.empty();
        for (int i = 0; i < splitFactor; i++) {
            Path outFile = getDestFile(origFile, i);
            outFiles.add(outFile);
            writers.add(Files.newBufferedWriter(outFile));
        }
        this.writers = writers.toImmutable();
        this.outFiles = outFiles.toImmutable();
    }

    private static Path getDestFile(Path origFile, int number) {
        String origFilename = origFile.getFileName().toString();
        origFilename = FilenameUtils.removeExtension(origFilename);
        String destFilename = origFilename + ".part" + number + ".geojson";
        return Paths.get(destFilename);
    }

    public Writer getWriter(int hash) {
        int writerIndex = Math.abs(hash % splitFactor);
        return writers.get(writerIndex);
    }

    @Override
    public void close() {
        writers.forEach(bufferedWriter -> {
            try {
                bufferedWriter.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public ImmutableList<Path> getOutFiles() {
        return outFiles;
    }
}
