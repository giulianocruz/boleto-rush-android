# Boleto Rush! 1.9.0-QA — Relíquias, cofre e bônus AdMob

## Experiência implementada

- **Relíquias raras**: bronze (+1 fragmento), prata (+2), ouro (+3). Elas começam a aparecer depois do capítulo inicial (ouro depois do segundo). Limite de 12 fragmentos coletáveis por partida para controlar a economia.
- **Coleta ativa**: é preciso pegar a relíquia com a carteira. Não existe conversão para dinheiro verdadeiro, compra financeira, aposta ou prêmio material.
- **Cofre persistente**: relíquias coletadas ficam pendentes até concluir/salvar a rodada (inclusive encerramento voluntário permitido). O GameSettlement grava saldo e recibo com a mesma transação de pontuação, moedas e notas; replay da rodada não concede fragmentos duas vezes. Recibos antigos v1 continuam legíveis.
- **Loja de trocas**: 2 relíquias = 18 notas cosméticas; 3 = 5 moedas de melhoria (quita dívidas de cartão primeiro); 5 = 3 s de escudo na próxima rodada; 7 = 6 s de ímã na próxima rodada. Transações atômicas e bloqueio de dupla compra de poderes já preparados.
- **Interface**: coletáveis com cores distintas, halo, brilho, valores na tela, saldo no HUD, botão Cofre, resumo da partida, instruções na loja.
- **Polimento Raspão** herdado da versão 1.8.6: pulso radial com recompensa visual de sequência e remoção de três efeitos desenhados em duplicidade por quadro.

## AdMob

- Mantidas as recompensas voluntárias de reviver, missões e dobrar ganhos.
- Novo anúncio recompensado opcional para +1 relíquia no resultado de uma rodada que efetivamente salvou pelo menos uma relíquia.
- Recompensa só após callback `onUserEarnedReward` do fluxo atual e uma transação única por ID de rodada. Fechar sem completar não concede relíquias.
- Mantido UMP (consentimento, `canRequestAds()`, opções de privacidade no menu) e Google Mobile Ads SDK já integrado.
- **APENAS IDs DE DEMONSTRAÇÃO**: `verifyPlayConfiguration` bloqueia release sem chave de upload e IDs AdMob próprios. Não usar credenciais nem chaves no GitHub.

## Validação desta rodada

- Gradle offline: `:app:testDebugUnitTest :app:assembleDebug` — BUILD SUCCESSFUL.
- JUnit: 76 testes, 0 falhas e 0 erros, incluindo idempotência do recibo, rollback de falha, loja, quitação de dívidas, boosts únicos, ad reward claim único e recibo legado.
- APK QA paralelo `br.com.proximaera.boletorush.qa`, versionCode 34, versionName 1.9.0-qa.debug, targetSdk 36.
- Dispositivo físico Android não conectado via ADB. Não declarar Play-ready sem validação de 45 e 90 segundos, travamentos, menus, relíquias e vídeo recompensado com anúncio de TESTE.

## Roteiro de teste em celular

1. Instale a versão QA em paralelo à Play; abra e jogue Clássico e Maratona.
2. Colete bronze/prata/ouro, confirme os valores; deixe passar alguns sem coletar para testar que não geram fragmentos.
3. Saia da partida pelo fluxo de guardar ganhos e confirme que o cofre aumenta apenas depois do resultado salvo.
4. Troque 2 relíquias por notas, 3 por moedas; confirme não ser possível comprar com saldo insuficiente nem duplicar poderes já preparados.
5. Compre escudo ou ímã para a próxima partida, inicie, verifique o efeito e confira que foi consumido.
6. Complete um anúncio recompensado de teste após coletar relíquias: +1 fragmento. Reabra o app: o bônus não deve repetir na mesma partida.
7. Feche um anúncio sem completar: não deve creditar. Sem conexão/consentimento, jogo e loja devem continuar funcionando.
8. Teste Raspão e os efeitos simultâneos de Escudo/Ímã/Turbo/Câmera Lenta em Android físico (desempenho e legibilidade).

## Próximas versões — propostas, ainda NÃO implementadas

- Álbum de relíquias, coleção por cidade e aparências exclusivas.
- Missões semanais offline com progressão e recompensas cosméticas.
- Eventos temáticos e chefes de fim do mês com padrões de boleto.
- Melhorias de acessibilidade, tutorial de coleção e testes físicos de balanceamento.

## APK gerado e verificado no Insano

- Arquivo: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-1.9.0-RELIQUIAS-QA.apk`
- Pacote: `br.com.proximaera.boletorush.qa`; `versionCode=34`; `versionName=1.9.0-qa.debug`; `minSdk=26`; `targetSdk=36`.
- Tamanho: **17.408.035 bytes**.
- SHA-256: `C0DE52925D2C99F0B2C6DC3A77587DE8A4B9D475303965D5DDF9D90DF89FA42B`.
- Build: **76 testes / 0 falhas / 0 erros**. Não é AAB Play assinado, AdMob ainda com IDs de teste.
