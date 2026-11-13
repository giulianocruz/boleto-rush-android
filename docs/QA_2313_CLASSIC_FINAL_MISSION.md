# Boleto Rush 2.3.13-QA — última missão do Clássico agora é alcançável

## Falha confirmada por análise determinística do gameplay

O Clássico dura **45 segundos** e o quarto capítulo começa aos **33,75s**. O desafio Fechamento do Mês começa aos **27s** e substitui os lançamentos comuns por três ondas (27, 33 e 39 segundos). Cada onda fornece somente **uma moeda na pista segura**.

As moedas da segunda e terceira ondas atravessam a linha da carteira depois que o quarto capítulo começa. A moeda da primeira onda cruza antes disso. Consequentemente, o jogador pode coletar **no máximo duas moedas nesse capítulo** (sem novos spawns comuns), mas a missão exigia **quatro**. O objetivo era impossível no Clássico.

A terceira onda ainda tem tempo de chegar ao jogador antes do fim da partida, mesmo com câmera lenta: velocidade 0,32 × 0,62; distância normalizada do spawn à carteira 0,865; queda aproximada de 4,36 segundos, deixando cerca de 1,64 segundo até os 45s. **O problema não era a velocidade do chefe — era a meta do capítulo.**

## Ajuste de balanceamento

- O quarto capítulo do **Clássico (45s)** passa a exigir **duas moedas** para concluir a meta cosmética.
- A **Maratona (90s)** conserva o objetivo original de **quatro moedas** e as metas dos três capítulos anteriores não mudam.
- O HUD da meta usa o objetivo da duração selecionada, com a mesma função que define o prêmio. Não há diferença entre o valor mostrado ao jogador e a regra aplicada.
- O prêmio por conclusão do quarto capítulo permanece em **R$ 6 em notas virtuais para visuais**, uma vez por partida; não foi criado anúncio obrigatório nem aumento de recompensa.
- As posições da pista segura, a quantidade de obstáculos, o valor das moedas, o tempo, a dificuldade de sobrevivência e a conquista mensal continuam iguais.
- As constantes de início e velocidade dos objetos das ondas foram centralizadas em `GameMonthChallenge`, para que os testes protejam a regra real do jogo.

## Quatro testes de regressão adicionais

1. Meta do quarto capítulo é 2 no Clássico e 4 na Maratona, sem alteração dos outros três capítulos.
2. Clássico concede as notas cosméticas apenas após a segunda moeda e nunca paga novamente após a meta concluída.
3. Maratona continua exigindo quatro moedas antes de conceder o mesmo prêmio.
4. Simulação determinística das três ondas em duração 45s, nas velocidades normal e câmera lenta, confirma que somente duas moedas chegam ao jogador depois do início do quarto capítulo e antes do fim dos 45 segundos.

## Homologação física necessária

Jogar o Clássico até o quarto capítulo, seguir as pistas verdes nas ondas 2 e 3 e confirmar `META CONCLUÍDA +R$ 6` ao pegar as duas moedas. Repetir a Maratona para confirmar a meta 4/4. Conferir também a conquista do selo mensal e a ausência de falhas de som, UI ou colisão. Os testes automatizados não substituem o teste no celular.

**QA prevista:** `br.com.proximaera.boletorush.qa`, versionCode 50, versionName `2.3.13-qa.debug`, minSdk 26, targetSdk 36. A variante comercial é `br.com.proximaera.boletorush / 2.3.13`, mas sua assinatura e publicação continuam bloqueadas.

## Resultado validado no Insano — 12/11/2026

- `gradlew.bat --offline :app:verifyPlayIdentity :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2 --console=plain`: **BUILD SUCCESSFUL**.
- **134 testes JUnit / 0 falhas / 0 erros**, 17 suítes. Quatro novos testes de missão e simulação do cronômetro foram adicionados.
- Android Lint: **0 erros / 34 avisos**.
- APK QA local: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.13-META-FINAL-JUSTA-QA.apk` (17.458.803 bytes).
- SHA-256: `2DB343AEAE9DD651EA516B22AC2D0B4DE1D08CCB4BCBEF4A2CC06C44E5B1ABB0`.
- `aapt`: pacote `br.com.proximaera.boletorush.qa`, `versionCode=50`, `versionName=2.3.13-qa.debug`, minSdk 26 e targetSdk 36.
- `apksigner verify`: assinatura debug v2 válida, um signatário. Não equivale a AAB comercial de publicação.
- Atalhos de instalação/execução e coleta pós-partida atualizados para 2.3.13; backups 2.3.12 preservados.
- Script PowerShell sem erros de sintaxe; pré-verificação do Insano: `QA_PREFLIGHT_EXIT=0`, pacote e hash corretos, `READY_DEVICES=0`. Nenhuma instalação ou coleta em aparelho.
- **Limitações:** nenhuma homologação física Android, nem teste de anúncios recompensados e consentimento UMP. PR integrada #39 continua em rascunho.
