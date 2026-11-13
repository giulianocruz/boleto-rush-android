# Boleto Rush 2.3.3-QA — Cofre cheio não bloqueia conquistas

## Correção de progressão

Antes, `GameSettlement.settle()` calculava o avanço da missão semanal usando os fragmentos **depositados** no Cofre (limitados pelo espaço disponível). Assim, um jogador com Cofre no limite 999/999 podia coletar relíquias, mas não evoluir a missão de descoberta.

A partir da 2.3.3, os dois valores são independentes:

- **Fragmentos coletados:** até 12 por partida. Contribuem para a coleção permanente e para a missão semanal, mesmo com o Cofre cheio.
- **Fragmentos depositados:** continuam limitados à capacidade máxima de **999**. Não é possível aumentar o saldo acima do teto.
- **Tela de resultados:** explica quando relíquias ficaram sem espaço, informando que ainda foram registradas no álbum e na missão.
- **Persistência:** coleções, missão, notas e saldo seguem na mesma transação de fechamento de rodada, com proteção contra duplicação e compatibilidade de recibos V1/V2/V3/V4.
- **Anúncios:** o bônus AdMob de +1 relíquia mantém a regra de não conceder fragmentos quando a carteira está cheia. Nenhuma configuração de anúncio foi alterada.

## Cobertura de regressão

Novos testes verificam:
1. Cofre 998/999: cinco fragmentos coletados, só um depositado; a missão recebe os cinco.
2. Cofre 999/999: três fragmentos coletados, nenhum depositado; a missão chega a 8/8 e premia R$16 em notas *virtuais* uma vez.
3. Ouro coletado com Cofre cheio: álbum registra a raridade e libera o visual exclusivo, sem criar fragmentos extras.
4. Falha de salvamento com Cofre cheio: coleção, prêmio e saldo não são alterados até a retentativa completar.
5. Repetição do ID da rodada: nem saldo nem prêmio semanal são concedidos duas vezes.

## Antes da publicação

QA física segue obrigatória: tela de resultado, Cofre, álbum, missões, vídeos AdMob de teste, UMP, partidas 45 e 90 segundos e desempenho. Não gerar AAB comercial sem upload key e IDs reais de monetização.

**Status:** a publicação na Google Play não foi alterada. Esta revisão permanece separada da versão de produção.

## Resultado verificado no Insano — 12/11/2026

- Comando: `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`.
- **BUILD SUCCESSFUL**; **102 testes unitários aprovados**, 0 falhas, 0 erros (16 classes).
- APK QA isolado: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.3-COFRE-E-MISSOES-QA.apk`
- 17.440.803 bytes, SHA-256 `9E742A23630104B9ACA78B62381B8565CCDCA353A06E6FE88C1E3BF08D38C50C`.
- `aapt`: pacote `br.com.proximaera.boletorush.qa`, versionCode 40, versionName `2.3.3-qa.debug`, minSdk 26, targetSdk 36.
- `apksigner verify --verbose`: assinatura de depuração v2 válida, um signatário. Não é AAB de publicação.
- Atalho do Insano `TESTAR-BOLETO-RUSH-ANDROID.cmd` atualizado para a 2.3.3. Backup preservado como `TESTAR-BOLETO-RUSH-ANDROID-ANTERIOR-232.cmd`.
- Pré-verificação do atalho: **sucesso**, pacote e hash corretos, `READY_DEVICES=0`, nenhum dado ou aplicativo modificado.
- O inventário textual de arquivos Kotlin/XML/JSON/TXT do `app/src/main` não apresentou caracteres de controle indevidos nem U+FFFD. Esta análise é estática e não substitui o teste visual do jogo.

**Pendente:** instalar e jogar a 2.3.3 no Android físico, confirmar recompensas e persistência com o Cofre cheio, revisar UMP/AdMob de teste e gerar AAB assinado comercial com IDs próprios antes da Play.
