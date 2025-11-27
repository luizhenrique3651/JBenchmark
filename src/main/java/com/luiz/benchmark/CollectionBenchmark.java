package com.luiz.benchmark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectionBenchmark {

    private final int[] sizes = new int[]{10_000, 100_000};
    private final int iterations = 3; // repetições para cada cenário
    private final int warmups = 1;

    public void executeAndCompare() throws Exception {
        System.out.println("\n*********  Executando benchmark de coleções  *********");
        for (int size : sizes) {
            System.out.println("\n--- Benchmark para tamanho: " + size + " ---");
            runListBenchmarks(size);
            runSetBenchmarks(size);
            runMapBenchmarks(size);
        }
    }

    private void runListBenchmarks(int size) throws InterruptedException {
        System.out.println("List (ArrayList) — inserção");
        long ins = timedBatch(() -> insertList(size), warmups, iterations);
        System.out.println("  Inserção média: " + ins + " ms");

        System.out.println("List (ArrayList) — leitura");
        long read = timedBatch(() -> readList(size), warmups, iterations);
        System.out.println("  Leitura média: " + read + " ms");

        System.out.println("List (ArrayList) — atualização");
        long upd = timedBatch(() -> updateList(size), warmups, iterations);
        System.out.println("  Atualização média: " + upd + " ms");

        System.out.println("List (ArrayList) — deleção");
        long del = timedBatch(() -> deleteList(size), warmups, iterations);
        System.out.println("  Deleção média: " + del + " ms");
    }

    private void runSetBenchmarks(int size) throws InterruptedException {
        System.out.println("Set (HashSet) — inserção");
        long ins = timedBatch(() -> insertSet(size), warmups, iterations);
        System.out.println("  Inserção média: " + ins + " ms");

        System.out.println("Set (HashSet) — leitura (contains)");
        long read = timedBatch(() -> readSet(size), warmups, iterations);
        System.out.println("  Leitura média: " + read + " ms");

        System.out.println("Set (HashSet) — atualização (remoção+adição)");
        long upd = timedBatch(() -> updateSet(size), warmups, iterations);
        System.out.println("  Atualização média: " + upd + " ms");

        System.out.println("Set (HashSet) — deleção");
        long del = timedBatch(() -> deleteSet(size), warmups, iterations);
        System.out.println("  Deleção média: " + del + " ms");
    }

    private void runMapBenchmarks(int size) throws InterruptedException {
        System.out.println("Map (HashMap) — inserção");
        long ins = timedBatch(() -> insertMap(size), warmups, iterations);
        System.out.println("  Inserção média: " + ins + " ms");

        System.out.println("Map (HashMap) — leitura (get)");
        long read = timedBatch(() -> readMap(size), warmups, iterations);
        System.out.println("  Leitura média: " + read + " ms");

        System.out.println("Map (HashMap) — atualização (put)");
        long upd = timedBatch(() -> updateMap(size), warmups, iterations);
        System.out.println("  Atualização média: " + upd + " ms");

        System.out.println("Map (HashMap) — deleção");
        long del = timedBatch(() -> deleteMap(size), warmups, iterations);
        System.out.println("  Deleção média: " + del + " ms");
    }

    // utilitários de medida
    private long timedBatch(Runnable op, int warmups, int iterations) throws InterruptedException {
        // aquecimento (warmup)
        if(warmups > 0) {
            for (int i = 0; i < warmups; i++) {
                op.run();
                System.gc();
                Thread.sleep(50);
            }
        }
        long total = 0;
        for (int i = 0; i < iterations; i++) {
            long t0 = System.currentTimeMillis();
            op.run();
            long t1 = System.currentTimeMillis();
            long elapsed = t1 - t0;
            System.out.println("    execução " + (i + 1) + ": " + elapsed + " ms");
            total += elapsed;
            System.gc();
            Thread.sleep(50);
        }
        return total / iterations; // retorna média das repetições
    }

    // auxiliares para List
    private void insertList(int size) {
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
    }

    private void readList(int size) {
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(i);
        long sum = 0;
        for (int i = 0; i < size; i++) {
            sum += list.get(i);
        }
        // usa sum para evitar otimização excessiva
        if (sum < 0) System.out.println("negativo");
    }

    private void updateList(int size) {
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(i);
        for (int i = 0; i < size; i++) {
            list.set(i, list.get(i) + 1);
        }
        if (list.size() < 0) System.out.println("x");
    }

    private void deleteList(int size) {
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(i);
        for (int i = size - 1; i >= 0; i--) {
            list.remove(i);
        }
        if (!list.isEmpty()) System.out.println("x");
    }

    // auxiliares para Set
    private void insertSet(int size) {
        Set<Integer> set = new HashSet<>(size);
        for (int i = 0; i < size; i++) set.add(i);
    }

    private void readSet(int size) {
        Set<Integer> set = new HashSet<>(size);
        for (int i = 0; i < size; i++) set.add(i);
        long sum = 0;
        for (int i = 0; i < size; i++) {
            if (set.contains(i)) sum++;
        }
        if (sum < 0) System.out.println("x");
    }

    private void updateSet(int size) {
        Set<Integer> set = new HashSet<>(size);
        for (int i = 0; i < size; i++) set.add(i);
        // atualizar simulando remoção+adição
        for (int i = 0; i < size; i++) {
            set.remove(i);
            set.add(i + 1);
        }
        if (set.size() < 0) System.out.println("x");
    }

    private void deleteSet(int size) {
        Set<Integer> set = new HashSet<>(size);
        for (int i = 0; i < size; i++) set.add(i);
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        if (!set.isEmpty()) System.out.println("x");
    }

    // auxiliares para Map
    private void insertMap(int size) {
        Map<Integer, Integer> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) map.put(i, i);
    }

    private void readMap(int size) {
        Map<Integer, Integer> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) map.put(i, i);
        long sum = 0;
        for (int i = 0; i < size; i++) {
            Integer v = map.get(i);
            if (v != null) sum += v;
        }
        if (sum < 0) System.out.println("x");
    }

    private void updateMap(int size) {
        Map<Integer, Integer> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) map.put(i, i);
        for (int i = 0; i < size; i++) map.put(i, map.get(i) + 1);
        if (map.size() < 0) System.out.println("x");
    }

    private void deleteMap(int size) {
        Map<Integer, Integer> map = new HashMap<>(size);
        for (int i = 0; i < size; i++) map.put(i, i);
        for (int i = 0; i < size; i++) map.remove(i);
        if (!map.isEmpty()) System.out.println("x");
    }

}
