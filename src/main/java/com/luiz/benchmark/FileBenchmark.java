package com.luiz.benchmark;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class FileBenchmark {

    Path path = Paths.get("benchmark.txt");

    public void executeAndCompare() throws Exception{
        System.out.println("*********  Executando benchmark de leitura de arquivo  *********\n ");
        writeBenchmark();
        editBenchmark();
        deleteBenchmark();
    }

    // ------------------ Escrita / Inserção ------------------
    private void writeBenchmark() throws IOException {
        System.out.println("\n--- Benchmark de escrita (inserção) ---");
        javaIOWriteBenchmark();
        javaNIOWriteBenchmark();
        javaNIO2WriteBenchmark();
    }

    private void javaIOWriteBenchmark() throws IOException {
        Path out = Paths.get("benchmark_write.txt");
        long t0 = System.currentTimeMillis();
        BufferedWriter writer = new BufferedWriter(new FileWriter(out.toFile()));
        String linha = "Linha de exemplo para escrita\n";
        int lines = 50_000; // quantidade de linhas escritas
        for (int i = 0; i < lines; i++) {
            writer.write(linha);
        }
        writer.close();
        long t1 = System.currentTimeMillis();
        System.out.println("Java IO - escrita com BufferedWriter, tempo: " + (t1 - t0) + " ms");
    }

    private void javaNIOWriteBenchmark() throws IOException {
        Path out = Paths.get("benchmark_write.txt");
        long t0 = System.currentTimeMillis();
        FileChannel channel = FileChannel.open(out, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        String linha = "Linha de exemplo para escrita\n";
        byte[] bytes = linha.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        int lines = 50_000;
        long totalWritten = 0;
        for (int i = 0; i < lines; i++) {
            buffer.rewind();
            while (buffer.hasRemaining()) {
                totalWritten += channel.write(buffer);
            }
        }
        channel.close();
        long t1 = System.currentTimeMillis();
        System.out.println("Java NIO - escrita com FileChannel, tempo: " + (t1 - t0) + " ms (bytes escritos: " + totalWritten + ")");
    }

    private void javaNIO2WriteBenchmark() throws IOException {
        Path out = Paths.get("benchmark_write.txt");
        long t0 = System.currentTimeMillis();
        int lines = 50_000;
        List<String> content = new ArrayList<>(lines);
        for (int i = 0; i < lines; i++) content.add("Linha de exemplo para escrita");
        Files.write(out, content, StandardCharsets.UTF_8);
        long t1 = System.currentTimeMillis();
        System.out.println("Java NIO2 - escrita com Files.write, tempo: " + (t1 - t0) + " ms");
    }

    // ------------------ Edição ------------------
    private void editBenchmark() throws IOException {
        System.out.println("\n--- Benchmark de edição ---");
        javaIOEditBenchmark();
        javaNIOEditBenchmark();
        javaNIO2EditBenchmark();
    }

    private void javaIOEditBenchmark() throws IOException {
        Path tmp = Paths.get("benchmark_edit_tmp.txt");
        long t0 = System.currentTimeMillis();
        BufferedReader br = new BufferedReader(new FileReader(path.toFile()));
        BufferedWriter bw = new BufferedWriter(new FileWriter(tmp.toFile()));
        String line;
        while ((line = br.readLine()) != null) {
            bw.write(line + " - editado\n");
        }
        br.close();
        bw.close();
        long t1 = System.currentTimeMillis();
        System.out.println("Java IO - edição via stream (BufferedReader/Writer), tempo: " + (t1 - t0) + " ms");
    }

    private void javaNIOEditBenchmark() throws IOException {
        Path tmp = Paths.get("benchmark_edit_tmp.txt");
        long t0 = System.currentTimeMillis();
        List<String> linhas = Files.readAllLines(path, StandardCharsets.UTF_8);
        // substitui o loop por replaceAll para ser mais conciso
        linhas.replaceAll(s -> s + " - editado");
        Files.write(tmp, linhas, StandardCharsets.UTF_8);
        long t1 = System.currentTimeMillis();
        System.out.println("Java NIO - edição via FileChannel/ByteBuffer (via readAllLines fallback), tempo: " + (t1 - t0) + " ms");
    }

    private void javaNIO2EditBenchmark() throws IOException {
        // reutiliza a estratégia do NIO2 (Files.readAllLines + Files.write)
        javaNIOEditBenchmark();
        System.out.println("Java NIO2 - edição (reaproveitado NIO), tempo: (consistente com NIO)");
    }

    // ------------------ Deleção ------------------
    private void deleteBenchmark() throws IOException {
        System.out.println("\n--- Benchmark de deleção ---");
        javaIODeleteBenchmark();
        javaNIODeleteBenchmark();
        javaNIO2DeleteBenchmark();
    }

    private void javaIODeleteBenchmark() {
        Path out = Paths.get("benchmark_write.txt");
        long t0 = System.currentTimeMillis();
        boolean deleted = out.toFile().delete();
        long t1 = System.currentTimeMillis();
        System.out.println("Java IO - deleção via File.delete(), sucesso: " + deleted + ", tempo: " + (t1 - t0) + " ms");
    }

    private void javaNIODeleteBenchmark() throws IOException {
        Path tmp = Paths.get("benchmark_edit_tmp.txt");
        long t0 = System.currentTimeMillis();
        boolean exists = Files.exists(tmp);
        if (exists) Files.delete(tmp);
        long t1 = System.currentTimeMillis();
        System.out.println("Java NIO - deleção via Files.delete(), arquivo existia: " + exists + ", tempo: " + (t1 - t0) + " ms");
    }

    private void javaNIO2DeleteBenchmark() throws IOException {
        // mesma implementação que NIO
        javaNIODeleteBenchmark();
        System.out.println("Java NIO2 - deleção (reaproveitado NIO), tempo: (consistente com NIO)");
    }


}
