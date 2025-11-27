package com.luiz.file;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileGenerator {

   public void generateFile() {

        //gera um arquivo de 200 MB
        String fileName = "benchmark.txt";
        String linhaBase = "Esta é uma linha de exemplo para o arquivo de benchmark. \n";
        long tamanho = 200L * 1024 * 1024; // 200 MB

        try {
            long tamanhoAtual = 0;
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));

            while (tamanhoAtual < tamanho) {
                writer.write(linhaBase);
                tamanhoAtual += linhaBase.getBytes().length;
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
