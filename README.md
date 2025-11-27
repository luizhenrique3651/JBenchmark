# JBenchmark

Repositório de benchmarks simples em Java para comparar estratégias de I/O (API clássica Java IO, NIO e NIO2) e medir operações básicas em coleções (`ArrayList`, `HashSet`, `HashMap`). O objetivo é educacional: mostrar diferenças de implementação e desempenho em cenários práticos.

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

- Arquivo de entrada: o `FileGenerator` cria `benchmark.txt` (aprox. 200 MB) com linhas repetidas.
- Leitura:
  - IO: `BufferedReader.readLine()` (linha a linha).
  - NIO: `FileChannel` + `ByteBuffer` (leitura por blocos de bytes, buffer 8KB).
  - NIO2: `Files.readAllLines()` (carrega todas as linhas em memória).
- Escrita:
  - IO: `BufferedWriter` escrevendo linha a linha.
  - NIO: `FileChannel` escrevendo `ByteBuffer` por linha.
  - NIO2: `Files.write()` com um `List<String>` de conteúdo.
- Edição:
  - IO: leitura com `BufferedReader` e escrita para arquivo temporário linha a linha.
  - NIO/NIO2: readAllLines + replaceAll + Files.write (carrega tudo em memória e reescreve).
- Deleção: `File.delete()` e `Files.delete()`.

Medidas:
- Temporização simples usando `System.currentTimeMillis()` (duração em ms), impressa no console.
- Para benchmarks de coleções usa-se um esquema com warmup (1 execução) e 3 iterações medidas; entre execuções o código chama `System.gc()` e dorme 50 ms (nota: isso pode introduzir ruído).

Observação técnica: `Files.readAllLines()` carrega todo o conteúdo em memória — não é adequado para arquivos muito grandes. `FileChannel` tende a ser eficiente em leituras por blocos, mas implementações que realizam muitos writes pequenos podem sofrer com overhead.

## Ambiente / cenário de execução

Os resultados que seguem foram obtidos na execução cujo log foi fornecido pelo autor; aqui estão os detalhes do ambiente reportado:

- Máquina: MacBook Pro 2020 (Intel i5)
- Memória: 16 GB RAM
- Sistema operacional: macOS Sequoia
- JVM: Zulu 25.0.1 (caminho mostrado no log)
- Execução iniciada via IntelliJ (observa-se javaagent do IDE no comando)
- Aplicativos em segundo plano durante a execução: Spotify e Safari (podem gerar ruído nas medidas)

-- comando de execução que gerou o log:

/Users/luiz/.sdkman/candidates/java/25.0.1-zulu/zulu-25.jdk/Contents/Home/bin/java -javaagent:/Applications/IntelliJ IDEA.app/Contents/lib/idea_rt.jar=50721 -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -classpath /Users/luiz/Documents/GitHub/JBenchmark/target/classes com.luiz.Main

## Resumo dos resultados

A seguir está um resumo das partes relevantes do log e uma curta interpretação técnica para cada seção.

1) Leitura de arquivo

- Java IO clássico (BufferedReader): 792 ms
- Java NIO (FileChannel): 81 ms
- Java NIO2 (Files.readAllLines): 991 ms (linhas: 3.554.462)

Interpretação: o `FileChannel` foi muito mais rápido nesta execução para leitura em bloco. `BufferedReader` faz parsing por linhas e aloca `String`s, o que pode aumentar o custo. `Files.readAllLines()` foi o mais lento devido ao custo de alocar milhões de Strings na memória e ao overhead do GC.

2) Escrita (inserção)

- Java IO (BufferedWriter): 8 ms
- Java NIO (FileChannel): 228 ms (bytes escritos: 1.500.000)
- Java NIO2 (Files.write): 14 ms

Interpretação: `BufferedWriter` e `Files.write` foram muito rápidos; a implementação do `FileChannel` mostrou ser mais lenta porque realiza muitos writes pequenos (uma chamada de escrita por linha), gerando overhead. Escrever em buffer maior (agrupar linhas) normalmente melhora performance para `FileChannel`.

3) Edição

- Java IO (stream): 1824 ms
- Java NIO (readAllLines fallback): 2233 ms e 2419 ms (duas execuções reportadas)
- Java NIO2: (reaproveitou NIO — tempo consistente)

Interpretação: editar via leitura/stream linha a linha foi mais rápido que a estratégia que carrega tudo na memória e reescreve (readAllLines + write), provavelmente porque a alocação massiva de Strings e a escrita de volta disparam muito GC.

4) Deleção

- Java IO `File.delete()`: sucesso = true, tempo = 0 ms
- Java NIO `Files.delete()`: arquivo existia = true, tempo = 14 ms
- Segunda chamada a `Files.delete()`: arquivo existia = false, tempo = 0 ms

Interpretação: deleção de arquivos é geralmente muito rápida; pequenas variações (0 ms vs 14 ms) podem ser ruído do sistema ou custo de verificação adicional.

5) Benchmarks de coleções (resumo)

Para tamanho = 10.000 (médias):
- ArrayList: inserção = 2 ms, leitura = 2 ms, atualização = 4 ms, deleção = 3 ms
- HashSet: inserção = 2 ms, contains = 5 ms, atualização (remove+add) = 6 ms, deleção = 4 ms
- HashMap: inserção = 3 ms, get = 5 ms, put = 7 ms, deleção = 4 ms

Para tamanho = 100.000 (médias):
- ArrayList: inserção = 2 ms, leitura = 1 ms, atualização = 10 ms, deleção = 2 ms
- HashSet: inserção = 20 ms, contains = 25 ms, atualização = 30 ms, deleção = 24 ms
- HashMap: inserção = 21 ms, get = 30 ms, put = 35 ms, deleção = 22 ms

Interpretação: os resultados estão coerentes com expectativas gerais — `ArrayList` é eficiente para acesso indexado sequencial; `HashSet` e `HashMap` têm overhead de hashing e operações de rehash/colisões que aumentam custo com tamanho maior. Note que as durações são pequenas e sujeitas a ruído.

## Como reproduzir

1. Compilar:

```bash
mvn -q package
```

2. Rodar teste rápido (gera 10k linhas; indicado para validar sem usar muito espaço):

```bash
java -cp target/classes com.luiz.TestFileRunner
```

3. Rodar benchmark completo (gera ~200MB — verifique espaço em disco):

```bash
java -cp target/classes com.luiz.Main
```

