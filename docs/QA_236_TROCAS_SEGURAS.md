# Boleto Rush 2.3.6-QA — trocas justas no Cofre

## Correção de economia

A troca de fragmentos no Cofre permitia descontar a moeda colecionável mesmo quando a recompensa anunciada não cabia no saldo da carteira. Em condições extremas, era possível gastar relíquias por um benefício parcial ou até nulo.

Agora, `GameRelics.tradeHasCapacity` verifica **antes da compra** se a recompensa inteira pode ser concedida:

- **2 fragmentos por R$18 em notas virtuais:** a compra é bloqueada caso falte espaço para creditar todas as 18 notas, sem consumir fragmentos.
- **3 fragmentos por 5 moedas de melhoria:** a compra é bloqueada caso não seja possível creditar o equivalente a cinco moedas integralmente.
- **Crédito do cartão:** as moedas da troca continuam podendo amortizar dívidas. Se as cinco unidades forem suficientes para quitar uma dívida de cinco moedas, é possível fazer a troca mesmo com a carteira de moedas cheia. Se restar uma parcela a creditar, a compra só avança se houver espaço integral.
- **Bônus de proteção/ímã:** não foram modificados, permanecem de uso único para a partida seguinte.
- **Interface:** apresenta aviso explícito `SALDO NO LIMITE! NENHUMA RELÍQUIA FOI DESCONTADA.` caso não haja capacidade para entregar a recompensa.

## Testes de regressão incluídos

1. Saldo de notas próximo de `Int.MAX_VALUE`: troca bloqueada faltando espaço para apenas uma das 18 notas; liberada quando cabem todas as 18; sem desconto indevido.
2. Saldo de moedas próximo de `Int.MAX_VALUE`: bloqueio quando cabem só 3 das 5; compra normal quando cabem todas.
3. Dívida de cartão com carteira de moedas cheia: troca legítima se as cinco moedas pagarem a dívida inteira; bloqueio se haveria perda parcial do benefício.
4. Falha ao gravar a transação: não desconta fragmentos nem cria recompensa parcialmente; a tentativa seguinte pode funcionar.

## Integridade

- Saldo e débito de fragmentos continuam na **mesma transação de SharedPreferences** (`Editor.commit()`).
- A atualização não altera o formato de recibos de partida, as conquistas, missões, álbum, selos mensais, upgrades, custo de trocas ou a monetização por anúncio.
- Sem alteração de IDs do AdMob, UMP ou versão comercial. O processo de release continua bloqueado sem assinatura de upload e IDs de produção.

## QA manual antes da Play Store

Instalar o APK de QA em um celular com depuração USB autorizada e verificar as quatro trocas do Cofre, partidas completas de 45s e 90s, bônus de escudo, consentimento UMP e anúncios de teste. O aviso de saturação é uma proteção para saldos máximos, situação difícil de reproduzir em partidas normais. Não manipular dados de usuários reais para o teste.

**Versão:** `2.3.6-qa.debug`, `versionCode=43`, pacote independente `br.com.proximaera.boletorush.qa`, targetSdk 36.

## Resultado da execução no Insano — 12/11/2026

- Comando executado: `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`.
- **BUILD SUCCESSFUL**; **111 testes unitários, 0 falhas e 0 erros** em 16 classes JUnit. Compilação do APK debug concluída.
- Novo APK de QA independente: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.6-TROCAS-SEGURAS-QA.apk`.
- 17.457.187 bytes; SHA-256 `37F6F469C83B437DB0FDCB79A3614330430B89E77B162C35EA5F5695C0DD9610`.
- Pacote e versão validados via `aapt`: `br.com.proximaera.boletorush.qa`, versionCode 43, versionName `2.3.6-qa.debug`, minSdk 26, targetSdk 36.
- `apksigner verify`: assinatura debug válida em esquema v2, um signatário. **Não é uma release comercial assinada para a Play Store**.
- Atalho de testes do Insano `TESTAR-BOLETO-RUSH-ANDROID.cmd` agora usa a 2.3.6. Backup preservado em `TESTAR-BOLETO-RUSH-ANDROID-ANTERIOR-235.cmd`.
- Pré-verificação real do atalho: `PREFLIGHT_EXIT=0`, pacote e hash corretos, `READY_DEVICES=0`, sem instalar, apagar ou executar nada no Android.
- O AdMob segue com IDs de teste e o processo de geração de AAB comercial segue bloqueado sem chave de upload e configuração de monetização própria.

**Pendências:** teste físico Android das partidas 45/90s, economia do Cofre, mensagens de erro e fluxo consentimento/AdMob. Não enviar esta QA à Google Play.
