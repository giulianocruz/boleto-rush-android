# Boleto Rush — simulação offline de quedas

Este teste roda fora do Android Studio e do GitHub Actions. Requer apenas **JDK 17+** e um compilador Kotlin JVM compatível com o projeto.

A partir da raiz do repositório:

```bash
kotlinc \
  app/src/main/java/br/com/proximaera/boletorush/GamePacing.kt \
  app/src/main/java/br/com/proximaera/boletorush/GameDrops.kt \
  tools/qa/DropSimulation.kt \
  -include-runtime -d drop-simulation.jar
java -jar drop-simulation.jar
```

No PowerShell, use uma única linha `kotlinc <arquivo1> <arquivo2> <arquivo3> -include-runtime -d drop-simulation.jar` para evitar diferenças de continuação de linha.

O processo deve encerrar com `PASS: spawn fairness and pickup variety (200 complete rounds)`.

## O que verifica

- 100 rodadas determinísticas de 45 segundos e 100 de 90 segundos
- Quatro capítulos por rodada, frequência mínima de moedas e diversidade de itens
- Nenhuma sequência de quatro itens não-moeda
- Nenhum boleto ou cheque especial consecutivo
- Nenhum perigo gerado nos cinco segundos iniciais
- Perigos gerados fora da linha vertical imediata do jogador
- Spawns dentro dos limites horizontais do campo
- Presença real dos bônus PIX e salário nas amostras

## Limites

As rodadas simulam **somente geração e posicionamento dos itens**. Não modelam colisões, reações humanas, FPS, memória, áudio, anúncios, performance ou interfaces Android. O teste não produz APK e não substitui `gradlew testDebugUnitTest` ou validação física.

Quando houver acesso ao aparelho, validar novamente os dois modos, testar compras/limite/dívida e revisar se os obstáculos são percebidos como justos.
