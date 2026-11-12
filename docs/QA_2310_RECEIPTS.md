# Boleto Rush 2.3.10-QA — recibos com valores realmente depositados

## Defeito corrigido

O fechamento da partida protegia as carteiras de moedas e notas contra valores superiores a `Int.MAX_VALUE`, mas ainda gravava no recibo **o valor teórico de moedas/notas**, não o saldo efetivamente creditado. Assim, o resultado podia mostrar um ganho inexistente quando a carteira atingia sua capacidade máxima. A oferta opcional de bônus por vídeo, que consulta o recibo, também podia tomar esse número incorreto como base.

## Nova regra — crédito real

- O recibo informa **apenas as moedas de corrida depositadas**, após pagar dívida e respeitar o espaço na carteira. As moedas extras de conquistas continuam contabilizadas separadamente do ganho principal e permanecem sujeitas ao teto do saldo.
- Para notas virtuais, o espaço disponível é distribuído na ordem: notas coletadas durante a partida, prêmio da missão semanal e prêmio do Fechamento do Mês. O recibo mostra **o valor efetivo de cada parcela**.
- Uma missão ou selo mensal que foi concluído continua registrado como conquista, mesmo se a carteira já estiver cheia e não couber o prêmio integral de notas. O recibo deixa de anunciar notas que não entraram.
- Os ganhos normais, quando há espaço suficiente na carteira, **não mudam**. A regra anterior de não permitir prêmio duplicado por vídeo ou reprocessamento de rodada também foi mantida.
- Não muda o formato serializado do recibo (versão 4), os IDs do AdMob, a UMP, a precificação da loja, o saldo pré-existente nem os dados de progresso.

## Testes de regressão

1. Carteiras de notas e moedas completamente cheias: fechamento registra ganho zero e não oferece vídeo por crédito inexistente.
2. Carteiras quase cheias: apenas o espaço real aparece nos campos `coinsBanked` e `notesBanked`; repetir a rodada não duplica crédito nem histórico.
3. Missão semanal com carteira quase no limite: recibo discrimina notas coletadas e apenas a parte do prêmio semanal que coube, preservando a conquista semanal.
4. Selo mensal com carteira quase no limite: selo é concedido uma única vez, mas recibo indica somente as notas efetivamente depositadas.

A suíte completa também testa compras, dívida, álbum, relíquias, o Cofre, anúncios opcionais, salvamento atômico e retorno do relógio após segundo plano.

## Limites antes da publicação

Esta é uma correção de integridade para condições extremas de progresso local; os testes automatizados não comprovam o fluxo visual no Android real. Permanecem pendentes: partidas completas 45/90s no celular, UMP/AdMob, capturas atuais, assinatura de upload e configurações comerciais.

**Candidato:** `br.com.proximaera.boletorush.qa`, `versionCode=47`, `versionName=2.3.10-qa.debug`, API mínima 26, API alvo 36. A configuração de release usa `br.com.proximaera.boletorush / 2.3.10` e continua bloqueada sem assinatura comercial.

## Resultado confirmado no Insano — 12/11/2026

- `gradlew.bat --offline :app:verifyPlayIdentity :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2 --console=plain` → **BUILD SUCCESSFUL**.
- **122 testes JUnit, 0 falhas, 0 erros**, em 17 suítes. São quatro regressões novas de depósitos efetivos.
- **Android Lint: 0 erros, 34 avisos**, sem regressões adicionais.
- APK QA independente no Insano: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.10-RECIBOS-CORRETOS-QA.apk`.
- 17.458.803 bytes; SHA-256 `13E922A670C3DEFA8FE943D5379F7E5E379C7F6E6A36F1EC050BEA2DF2CA97B7`.
- `aapt`: `br.com.proximaera.boletorush.qa`, versionCode 47, versionName `2.3.10-qa.debug`, minSdk 26, targetSdk 36.
- `apksigner verify --verbose`: assinatura debug válida com esquema v2 e um signatário. Não é um AAB comercial.
- Atalhos `TESTAR-BOLETO-RUSH-ANDROID.cmd` e `COLETAR-EVIDENCIAS-BOLETO-RUSH.cmd` da Área de Trabalho atualizados para o APK 2.3.10; backups de ambas as versões anteriores e do script QA preservados.
- Script PowerShell 5.1: parser retornou 0 erros; pré-verificação do atalho `PREFLIGHT_EXIT=0` e `READY_DEVICES=0`. Nada foi instalado, iniciado, apagado, capturado ou publicado no celular.
- A API comercial continua independente com `versionName=2.3.10`; release não foi gerada. AdMob continua com identificadores de teste na QA.
