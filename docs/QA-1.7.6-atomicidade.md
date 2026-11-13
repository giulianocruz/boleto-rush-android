# Boleto Rush 1.7.6 QA — liquidação atômica

## Falha identificada

A partida era liquidada em operações distintas: moedas, notas, conquistas, cartão, contagem de partidas e recorde. Se o aplicativo fosse interrompido ou uma gravação falhasse, o usuário poderia receber apenas parte dos ganhos. Mesmo o controle de liquidação duplicada era mantido somente na memória do Compose.

## Alterações

- Introduzido `GameSettlement.settle()`, com apenas **um SharedPreferences.Editor.commit()** para moedas, notas, cartão, dívida, conquistas, número de partidas, recorde e ID da rodada liquidada.
- Cada rodada possui identificador UUID próprio, salvo com a transação; repetição do mesmo ID não paga novamente.
- Débitos de crédito são abatidos primeiro pelas moedas comuns, depois pelas recompensas de conquista; saldo líquido e novos limites são calculados antes de persistir.
- Nova interface `GameReceipt` informa o resultado efetivamente confirmado, evitando atualizar o HUD com dados não salvos. O resultado também informa quanto da dívida foi quitado nesta partida.
- Se `commit()` falhar, o encerramento permanece disponível para nova tentativa na pausa ou na tela de segunda chance. `settledRound` só é atualizado após confirmação.
- Conquistas antigas já obtidas continuam reconhecidas; não são pagas novamente.
- Mantidos saldo extra, ímã, PIX, salário e resgate voluntário da 1.7.5.
- `versionCode=24`, `versionName=1.7.6-qa`; a variante debug segue separada do aplicativo público.

## Testes adicionados

- Round normal: moeda, notas, cartão, duas conquistas e recorde liquidados juntos.
- Double-submit e repetição com o mesmo UUID: sem segundo crédito.
- Commit de preferência recusado: nenhuma recompensa parcial; segunda tentativa bem-sucedida após o armazenamento voltar.
- Dívida, cartão, salário e três conquistas coexistentes: teto respeitado e saldo sem duplicação.
- Permanecem testes das versões anteriores: ímã visual, progressão e 200 rodadas de geração, economia da loja, morte e resgate, PIX e créditos pendentes.

## QA da publicação

- [x] Gradle :app:testDebugUnitTest :app:assembleDebug — BUILD SUCCESSFUL, 52s na compilação final
- [x] JUnit: 7 suítes, 35 testes, 0 falhas, 0 erros; APK `BoletoRush-1.7.6-QA.apk` salvo na Área de Trabalho / 01 - Arquivos de Jogos / Boleto Rush (17.524.231 bytes; SHA256 B75C7F5F23C3E35D2A89EAC78964E5574BBA67CE54C4951CE3FFA244A5818F19)
- [ ] Conectar aparelho via ADB ou emulador seguro; em 30/10/2026 nenhum dispositivo ADB foi detectado. Emulador não iniciado por memória livre insuficiente (~1,9 GiB). Testar encerramento, morte, vitória e resgate.
- [ ] Confirmar que a mesma partida não remunera duas vezes
- [ ] Testar alternância entre salvar e descartar, cartão e compra de upgrades
- [ ] Anúncio recompensado, assinatura e arte final antes de qualquer envio para Play Console

**Nota:** SharedPreferences fornece persistência local com commit transacional para esses campos, mas não substitui backup do progresso, conta em nuvem ou testes reais em aparelho. A versão pública não será alterada nesta etapa.
