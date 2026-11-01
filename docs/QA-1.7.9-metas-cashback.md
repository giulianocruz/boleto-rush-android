# Boleto Rush 1.7.9 QA — metas financeiras e cashback

## Motivação
O feedback real pede mais variedade, progressão interessante e dificuldade muito lenta, sem pontos inexplicáveis ou inflação da economia. A 1.7.8 já consolidou moedas visíveis, ritmo suave, cenários por capítulo e duas moedas.

## Novas mecânicas
- Quatro **metas curtas por capítulo**, exibidas no HUD, sempre contabilizadas apenas depois de uma coleta REAL de moeda:
  - Capítulo 1 Centro: coletar 2 moedas, receber R$ 3 em notas de cosméticos.
  - Capítulo 2 Banco: coletar 3 moedas, receber R$ 4.
  - Capítulo 3 Comércio: coletar 3 moedas, receber R$ 5.
  - Capítulo 4 Cobrança: coletar 4 moedas, receber R$ 6.
- Máximo de R$ 18 em notas por conjunto de metas concluídas; o **score e as moedas raras** não são aumentados por metas.
- Os objetivos são opcionais e recompensados só uma vez por capítulo. Contagem do capítulo é preservada na pausa e reinicia na transição de etapa.
- Novo item raro `Kind.CASHBACK`, gerado somente nos capítulos 3 e 4 com uma faixa de 2,5% da rolagem, respeitando a garantia de moedas. Coleta concede R$ 6 em notas para cosméticos, **zero pontos e zero moedas raras**.
- Item cashback com arte procedural legível (cupom CB +6), brilho verde e contraste adequado.
- Tela Como Jogar explica metas/cashback; resultado da rodada mostra quantas metas foram atingidas e notas ganhas.
- A consolidação continua sendo feita em `GameSettlement` no encerramento: abandonar uma rodada não deve salvar recompensas pendentes.
- Versão QA `1.7.9-qa`, versionCode 27, instalável em paralelo à versão da Play Store.

## Checklist de verificação
- [x] Compilação `:app:testDebugUnitTest :app:assembleDebug` aprovada (BUILD SUCCESSFUL em 2m02s, modo offline com paralelismo reduzido, 01/11/2026).
- [x] JUnit: 9 suítes, 48 testes, 0 erros e 0 falhas, incluindo as verificações financeiras anteriores.
- [x] Testes verificam cada meta paga uma vez, mesmo com várias moedas coletadas depois de concluída.
- [x] Cashback é classificado como dinheiro cosmético, sem pontuação automática; regras de pontuação e renda da loja preservadas.
- [x] Cashback só é gerado nos capítulos 3/4 (faixa de rolagem testada).
- [x] Simulador existente de 200 rodadas e teste de distribuição passaram, mantendo máximo de dois itens consecutivos sem moedas.
- [ ] **QA visual ainda pendente**: arte de cashback e texto das metas foram implementados e compilaram; legibilidade em aparelho físico não foi verificada.
- [ ] Samsung Galaxy A55: sem dispositivos detectados via ADB em 01/11/2026; testar 45s e 90s, cashback, metas, itens negativos, loja, encerramento e reinício.
- [ ] AdMob e crash logs avaliados antes de publicar.

**Sem mudança na Play Store até validação física.** Os resultados de simulações e unit tests não substituem testes do aplicativo Android em execução.

## Artefato QA validado
- `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-1.7.9-QA.apk`
- 17.375.267 bytes; SHA256 `6D51CC6345B2E6D2CAE65B8E93033B4C799F4E205108B4AF7C824497C2D02B86`
- `aapt dump badging`: pacote `br.com.proximaera.boletorush.qa`, versionCode 27, versionName `1.7.9-qa.debug`, atividade `br.com.proximaera.boletorush.MainActivity`.
- Publicação Play Store não modificada.
