package com.luiz;

import com.luiz.benchmark.FileBenchmark;
import com.luiz.benchmark.CollectionBenchmark;
import com.luiz.file.FileGenerator;

public class Main {
    public static void main(String[] args) {
        //gera arquivo de benchmark
        FileGenerator fileGenerator = new FileGenerator();
        fileGenerator.generateFile();

        //inicia os benchmarks de leitura de arquivo e coleções
        try {
            FileBenchmark fileBenchmark = new com.luiz.benchmark.FileBenchmark();
            fileBenchmark.executeAndCompare();

            //executa benchmark de colecoes
            CollectionBenchmark collectionBenchmark = new com.luiz.benchmark.CollectionBenchmark();
            collectionBenchmark.executeAndCompare();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
