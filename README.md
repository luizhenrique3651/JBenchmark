# JBenchmark

Repositório de benchmarks simples em Java para comparar estratégias de I/O (API clássica Java IO, NIO e NIO2) e medir operações básicas em coleções (`ArrayList`, `HashSet`, `HashMap`). O objetivo é educacional: demonstrar diferenças de implementação, trade-offs e comportamento de desempenho em cenários práticos.

## Tecnologias e dependências

- Linguagem: Java (testado com Zulu JDK 25.0.1)
- Build: Maven (`pom.xml` presente)
- APIs usadas nos benchmarks:
  - Java IO (`BufferedReader`, `BufferedWriter`, `File.delete()`)
  - Java NIO (`FileChannel`, `ByteBuffer`, `StandardOpenOption`)
  - Java NIO2 (`Files`, `readAllLines`, `write`, `delete`)

## Estrutura do projeto (arquivos principais)

- `FileGenerator` — gera o arquivo `benchmark.txt` (por padrão ~200 MB) usado nos benchmarks.
- `FileBenchmark` — implementa cenários de leitura, escrita, edição e deleção usando IO/NIO/NIO2.
- `CollectionBenchmark` — mede operações de inserção, leitura, atualização e deleção em `ArrayList`, `HashSet` e `HashMap` para tamanhos configurados (por padrão 10_000 e 100_000).
- `Main` — ponto de entrada: gera arquivo e executa os benchmarks.
- `TestFileRunner` — runner de teste rápido (gera um arquivo pequeno de 10k linhas e limpa artefatos).

## Estratégia de benchmark e métricas

- Arquivo de entrada: o `FileGenerator` cria `benchmark.txt` (aprox. 200 MB) com linhas repetidas para simular leitura/escrita intensiva.
- Leitura:
  - IO: `BufferedReader.readLine()` (leitura linha a linha, streaming).
  - NIO: `FileChannel` + `ByteBuffer` (leitura por blocos de bytes, buffer de leitura configurado — comportamento orientado a blocos).
  - NIO2: `Files.readAllLines()` (carrega todas as linhas em memória como `List<String>`).
- Escrita:
  - IO: `BufferedWriter` escrevendo linha a linha.
  - NIO: `FileChannel` escrevendo `ByteBuffer` por linha ou bloco.
  - NIO2: `Files.write()` com um `List<String>` de conteúdo.
- Edição:
  - IO: leitura via `BufferedReader` e escrita para arquivo temporário linha a linha (streaming).
  - NIO/NIO2: implementação que recorre a `readAllLines()` + transformações + `Files.write()` (carrega tudo em memória e reescreve).
- Deleção: `File.delete()` (IO) e `Files.delete()` (NIO2).

Medidas:
- Temporização simples usando `System.currentTimeMillis()` (duração em ms), impressa no console.
- Para benchmarks de coleções: warmup + 3 iterações medidas; entre execuções é chamado `System.gc()` e há pequena espera (50 ms) — cuidado: isso pode introduzir ruído nas medições.

Observação técnica: `Files.readAllLines()` não é adequado para arquivos muito grandes (pode estourar memória / criar overhead de GC). Estratégias baseadas em blocos (`FileChannel`) tendem a ser mais estáveis para grandes volumes, desde que agreguem escrita em buffers maiores para reduzir chamadas de I/O pequenas.

## Detalhes técnicos — abordagem dos benchmarks

Abaixo há uma descrição mais técnica e direta do que cada benchmark faz, quais decisões de implementação foram tomadas e as implicações para medições.

### Benchmark de arquivo (classe `FileBenchmark`)

- Leitura:
  - `javaIOBenchmark()` usa `BufferedReader.readLine()` para consumir o arquivo linha a linha (streaming). Essa estratégia cria objetos `String` por linha e faz parsing do separador de linha — é eficiente em memória quando usamos pouco conteúdo por vez.
  - `javaNIOBenchmark()` abre um `FileChannel` em modo `READ` e faz leituras em blocos de 8 KB (`ByteBuffer.allocate(8192)`). O loop lê blocos: `channel.read(buffer)`, `buffer.flip()` e `buffer.clear()`. Nesta implementação não há decodificação explícita dos bytes para `String`, pois o objetivo é medir throughput bruto de leitura em blocos (I/O por blocos). O tamanho do buffer (8 KB) é um parâmetro que afeta latência e throughput; buffers maiores tendem a reduzir chamadas de I/O.
  - `javaNIO2Benchmark()` usa `Files.readAllLines(path)` que carrega todas as linhas em memória como `List<String>`. Isso é simples, porém consome muita memória com arquivos grandes e aciona GC intensamente.

- Escrita:
  - `javaIOWriteBenchmark()` usa `BufferedWriter` e escreve linha a linha. O `BufferedWriter` agrega pequenos writes em um buffer nativo antes de gravar no disco.
  - `javaNIOWriteBenchmark()` escreve repetidamente um pequeno `ByteBuffer` para um `FileChannel`. O buffer é alocado com o tamanho exato da linha (`bytes.length`) e reaproveitado via `buffer.rewind()` em cada iteração. Apesar de reaproveitar o buffer, escrever muitas vezes chama `channel.write()` repetidamente; agrupar muitas linhas em um buffer maior antes de escrever costuma melhorar a performance.
  - `javaNIO2WriteBenchmark()` monta um `List<String>` com todas as linhas e chama `Files.write()` — a API delega ao sistema para gravar eficientemente, mas exige construir a lista em memória.

- Edição:
  - A versão IO faz leitura streaming e escreve em um arquivo temporário linha a linha (bom para mudanças parciais e menor uso de memória).
  - As versões NIO/NIO2 neste projeto usam `Files.readAllLines()` seguido de transformações (`replaceAll`) e `Files.write()` — ou seja, carregam tudo em memória, modificam e reescrevem. Essa abordagem é mais direta, porém não escalável para arquivos muito grandes.

- Deleção:
  - Implementada via `File.delete()` (apenas delega para o método da `File`) e `Files.delete()` (NIO). Diferenças observadas nas medições são pequenas e geralmente ruído.

Observações e melhorias possíveis para o benchmark de arquivo:
- Para medir mais fielmente o impacto do parsing (transformar bytes em String) seria interessante fazer a decodificação explícita em `javaNIOBenchmark()` usando um `CharsetDecoder` e tratar limites entre buffers (partial characters).
- Para escrita com `FileChannel` agrupar várias linhas em um `ByteBuffer` maior reduz chamadas de sistema e costuma ser mais rápido.
- Evitar `Files.readAllLines()` para arquivos grandes; usar streaming ou `Files.lines()` (stream lazily) pode ser melhor.
- Usar medição com `System.nanoTime()` proporciona maior resolução.

### Benchmark de coleções (classe `CollectionBenchmark`)

- Parâmetros principais:
  - `sizes = {10_000, 100_000}` — tamanhos de teste.
  - `iterations = 3` — número de execuções medidas por operação.
  - `warmups = 1` — número de execuções de aquecimento antes das medições.

- Estratégia geral:
  - Para cada tamanho, o benchmark executa cenários de inserção, leitura, atualização e deleção para `ArrayList`, `HashSet` e `HashMap`.
  - Cada operação (`insertList`, `readList`, etc.) cria a coleção localmente dentro do método. Isso significa que cada execução é isolada (coleções novas), evitando contaminação entre execuções.

- Como funciona o `timedBatch(Runnable op, int warmups, int iterations)`:
  1. Warmup: se `warmups > 0`, executa `op.run()` `warmups` vezes. Após cada warmup chama `System.gc()` e `Thread.sleep(50)` para tentar reduzir estado residual entre execuções.
  2. Medição: para cada uma das `iterations` repetições faz:
     - registra `t0 = System.currentTimeMillis()`;
     - executa `op.run()` (a operação medida);
     - registra `t1 = System.currentTimeMillis()` e calcula `elapsed = t1 - t0`;
     - imprime o tempo da execução atual (`execução N: X ms`) e acumula em `total`;
     - chama `System.gc()` e `Thread.sleep(50)` antes da próxima iteração.
  3. Retorna a média aritmética `total / iterations` como tempo médio em ms.

- Motivações por trás das escolhas:
  - Warmup tenta estimular o JIT a compilar rotinas quentes antes das medições.
  - `System.gc()` e `Thread.sleep(50)` são tentativas de reduzir o impacto de GC entre execuções e dar um pequeno intervalo para o sistema estabilizar.
  - Criar e operar em coleções novas em cada execução evita efeitos de estado residual (por exemplo, um `ArrayList` já populado).
  - Impressão de cada execução ajuda a avaliar variabilidade (ruído) entre tentativas.

- Limitações e pontos de atenção:
  - `System.currentTimeMillis()` tem resolução e precisão limitadas; `System.nanoTime()` é preferível para medições curtíssimas.
  - Chamar `System.gc()` explicitamente não garante que o GC ocorrerá e pode introduzir ruído — é melhor evitar ou usar outros mecanismos de controle de memória/isolamento.
  - Um único warmup (`warmups = 1`) pode não ser suficiente para estabilizar o JIT; recomenda-se aumentar para várias iterações em cenários reais.
  - As operações alocam estruturas grandes repetidamente, o que aciona GC e pode afetar tempos; separar alocação e operação pode isolar melhor o comportamento desejado.
  - Para medições confiáveis em Java recomenda-se usar JMH (Java Microbenchmark Harness), que trata de aquecimento, amostragem, isolamento e estatísticas de forma robusta.

- Observações de código:
  - Para evitar que o compilador elimine loops como dead code, algumas funções acumulam valores (`sum`) e fazem verificações triviais (`if (sum < 0) ...`) — isso força o uso dos resultados e reduz otimizações indesejadas.

## Cenário de execução

Os resultados abaixo foram obtidos na execução cujo log foi compartilhado. Informações do ambiente reportado:

- Máquina: MacBook Pro 2020 (Intel i5)
- Memória: 16 GB RAM
- Sistema operacional: macOS Sequoia
- JVM: Zulu JDK 25.0.1 (conforme caminho no log)
- Execução iniciada via IntelliJ (o comando contém `-javaagent` do IDE)
- Aplicativos em segundo plano durante a execução: Spotify e Safari (podem gerar ruído nas medidas)

Comando registrado no log que gerou os resultados:

/Users/luiz/.sdkman/candidates/java/25.0.1-zulu/zulu-25.jdk/Contents/Home/bin/java -javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=50721 -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath /Users/luiz/Documents/GitHub/JBenchmark/target/classes com.luiz.Main

> Observação: por ser uma máquina de desenvolvimento com apps abertos, pequenas variações entre execuções são esperadas.

## Resumo dos resultados

Abaixo segue um resumo direto dos valores registrados no log e uma interpretação breve por seção.

1) Leitura de arquivo (tempos em ms)

- Java IO (BufferedReader): 792 ms
- Java NIO (FileChannel): 81 ms
- Java NIO2 (Files.readAllLines): 991 ms (linhas: 3.554.462)

Interpretação: Nesta execução o `FileChannel` mostrou melhor desempenho para leitura por blocos. `BufferedReader` faz parsing por linhas e cria `String`s gradualmente; `Files.readAllLines()` foi o mais lento devido ao custo de alocar milhões de `String`s e pressão sobre o GC.

2) Escrita (inserção)

- Java IO (BufferedWriter): 8 ms
- Java NIO (FileChannel): 228 ms (bytes escritos: 1.500.000)
- Java NIO2 (Files.write): 14 ms

Interpretação: `BufferedWriter` e `Files.write` foram muito rápidos neste teste; a versão com `FileChannel` foi mais lenta provavelmente por fazer muitas escritas pequenas (overhead por chamada). Para `FileChannel` agrupar linhas em buffers maiores normalmente melhora performance.

3) Edição

- Java IO (stream linha-a-linha): 1824 ms
- Java NIO (readAllLines fallback): 2233 ms e 2419 ms (duas execuções reportadas)
- Java NIO2 (reaproveitou NIO): tempo consistente com NIO

Interpretação: A edição via streaming (IO) foi mais rápida que a estratégia que carrega tudo em memória e reescreve. Carregar milhões de linhas na memória aumenta custo de GC e tempo total de escrita.

4) Deleção

- Java IO `File.delete()`: sucesso = true, tempo = 0 ms
- Java NIO `Files.delete()`: arquivo existia = true, tempo = 14 ms
- Segunda chamada `Files.delete()`: arquivo existia = false, tempo = 0 ms

Interpretação: Deleção é geralmente rápida; pequenas variações são ruído e custos de verificação do sistema de arquivos.

5) Benchmarks de coleções (valores médios extraídos do log)

Para tamanho = 10_000 (médias)

- ArrayList:
  - Inserção (3 execuções): média = 2 ms
  - Leitura: média = 2 ms
  - Atualização: média = 4 ms
  - Deleção: média = 3 ms
- HashSet:
  - Inserção: média = 2 ms
  - Leitura (`contains`): média = 5 ms
  - Atualização (remove+add): média = 6 ms
  - Deleção: média = 4 ms
- HashMap:
  - Inserção: média = 3 ms
  - Leitura (`get`): média = 5 ms
  - Atualização (`put`): média = 7 ms
  - Deleção: média = 4 ms

Para tamanho = 100_000 (médias)

- ArrayList:
  - Inserção: média = 2 ms
  - Leitura: média = 1 ms
  - Atualização: média = 10 ms
  - Deleção: média = 2 ms
- HashSet:
  - Inserção: média = 20 ms
  - Leitura (`contains`): média = 25 ms
  - Atualização (remove+add): média = 30 ms
  - Deleção: média = 24 ms
- HashMap:
  - Inserção: média = 21 ms
  - Leitura (`get`): média = 30 ms
  - Atualização (`put`): média = 35 ms
  - Deleção: média = 22 ms

Interpretação geral das coleções:
- `ArrayList` se mantém extremamente rápido para inserção no final e acesso sequencial; atualizações indexadas e operações que forçam deslocamento podem aumentar custo, mas aqui permanecem baixas comparadas às estruturas baseadas em hashing.
- `HashSet` e `HashMap` apresentam overhead adicional por hashing e possíveis colisões/re-hashing — isso fica mais evidente com 100k elementos, onde os tempos aumentam significativamente.
- Os tempos são curtos (ms) e suscetíveis a ruído (JVM, GC, outros processos), portanto devem ser usados como indicativo e não prova definitiva.

## Interpretação geral e recomendações

- Para leitura de grandes arquivos: preferir abordagens baseadas em blocos (`FileChannel`) ou streaming (`BufferedReader`) em vez de `Files.readAllLines()` quando houver risco de esgotamento de memória.
- Para escrita: evitar muitos writes pequenos com `FileChannel`; agrupar em buffers reduz chamadas de I/O e melhora throughput.
- Edição: streaming linha-a-linha costuma ser mais eficiente quando o arquivo é grande e apenas parte do conteúdo muda. Evitar carregar tudo em memória quando possível.
- Coleções: escolha baseada no padrão de acesso — `ArrayList` para acesso sequencial/append rápido; `HashMap`/`HashSet` para buscas/uniquidade com custo de hash.

## Como reproduzir

1. Compilar:

```bash
mvn -q package
```

2. Rodar teste rápido (gera ~10k linhas; indicado para validar sem usar muito espaço):

```bash
java -cp target/classes com.luiz.TestFileRunner
```

3. Rodar benchmark completo (gera ~200MB — verifique espaço em disco):

```bash
java -cp target/classes com.luiz.Main
```

## Nota final

Os números apresentados são um retrato de uma execução específica (hardware, JVM, carga do sistema). Para comparações robustas recomenda-se rodar múltiplas execuções em um ambiente controlado (sem outros processos pesados), usar ferramentas de benchmark específicas (JMH) e coletar estatísticas de distribuição (médias, desvios, percentis).
