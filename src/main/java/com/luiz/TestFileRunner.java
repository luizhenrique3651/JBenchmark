package com.luiz;

import com.luiz.benchmark.FileBenchmark;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestFileRunner {
    public static void main(String[] args) {
        Path p = Paths.get("benchmark.txt");
        try {
            // gera um arquivo menor para testes (10k linhas)
            try (BufferedWriter w = new BufferedWriter(new FileWriter(p.toFile()))) {
                for (int i = 0; i < 10_000; i++) {
                    w.write("Linha teste " + i + "\n");
                }
            }

            FileBenchmark fb = new FileBenchmark();
            fb.executeAndCompare();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            try {
                Files.deleteIfExists(p);
                Files.deleteIfExists(Paths.get("benchmark_write.txt"));
                Files.deleteIfExists(Paths.get("benchmark_edit_tmp.txt"));
            } catch (IOException ignore) {
            }
        }
    }
}

