# Boleto Rush 1.7.3 — coleta visual do ímã e pausa justa

## Objetivo

Corrigir o problema percebido pelo jogador: moedas desaparecendo repentinamente, enquanto a pontuação sobe. O ímã já tinha sido limitado à área visível, mas ainda creditava no instante em que a moeda atingia Y = 0,40, distante da carteira.

## Implementação

- Adicionado `GameMagnet.kt`: rotina pura de atração frame a frame, sem dependências de Compose.
- A moeda só passa a ser puxada depois que é visível no espaço de jogo (Y >= 0,40). O movimento horizontal é suave e aproxima-se do personagem; o vertical dirige-se para a altura da carteira.
- O placar só sobe com coleta efetiva, por colisão na linha da carteira ou entrada na área de captura de `GameMagnet.reachedWallet()`. Nenhum ponto é creditado por ímã quando a moeda está distante.
- Um fio visual discreto liga moedas atraídas à carteira, reforçando o motivo do movimento.
- O ímã continua afetando **somente moedas**, e não notas, salário, cartão, boleto ou PIX.
- Intervalo acumulado para novos itens, contador de itens que não são moedas, histórico de perigo e ID de drops sobrevivem a pausa/retomada. Reiniciam somente ao começar uma nova rodada.
- Versão `1.7.3-qa`, versionCode 21; APK debug usa pacote paralelo `br.com.proximaera.boletorush.qa`.

## Testes

- `GameMagnetTest`: nenhuma atração antes de Y=0,40, nenhuma atração sem poder, ausência de teleporte, deslocamento progressivo, coleta perto da carteira, zero delta.
- Testes financeiros e balanceamento de 200 rodadas simuladas preservados.
- [x] Gradle `testDebugUnitTest` e `assembleDebug`: **BUILD SUCCESSFUL in 50s** (log local `build-173-magnet-visible.log`).
- [ ] Instalação no Samsung A55: ADB verificou **0 dispositivos conectados** em 30/10, 16h; APK QA preparado, sem alterar o app público.
- [ ] Jogar com ímã e PIX simultâneos, observar moedas atraídas até a carteira.
- [ ] Pausar e retomar com ímã ativo e sem ímã; validar que o gerador não reinicia suas garantias.
- [ ] Jogar 45 e 90 segundos completos; testar crédito, loja e anúncios recompensados.
- [ ] Revisar arte premium final, FPS em aparelho real, assets e assinatura para eventual release.

**Artefato QA:** `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-1.7.3-QA.apk`, 17.358.883 bytes, SHA256 `64E1B623D6730F36C1D7EDFA05F643F88802A8F355FB18877252241B8278B429`.

**Não publicar versão pública até QA visual.** O build de debug não usa os mesmos dados/assinatura da Play Store.
