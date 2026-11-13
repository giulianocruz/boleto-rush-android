# Boleto Rush 2.3.14-QA — conquistas com crédito e abatimento reais

## Falha encontrada na tela de resultados

As conquistas PRIMEIRA RESERVA, BOLSO CHEIO e FATURA VENCIDA liberam 6, 8 e 12 moedas virtuais. A transação de fechamento já aplicava corretamente os limites da carteira e o abatimento de dívida, mas a tela anunciava **todas as moedas nominais como recebidas**, mesmo quando parte pagava dívida ou era descartada no limite do saldo. Isso poderia enganar o jogador sobre o benefício conquistado.

## Correção

- O fechamento armazena separadamente `milestoneCoinsCredited` (**moedas de conquista efetivamente depositadas**) e `milestoneDebtRepaid` (**moedas de conquista realmente usadas para quitar dívida de cartão**).
- A tela anuncia uma **nova conquista desbloqueada**, seguida do detalhamento correspondente. Se a carteira já estava no limite, informa quantas moedas não puderam ser depositadas, sem inflar o saldo.
- O prêmio nominal e os requisitos das três conquistas **não mudaram**; o pagamento continua atômico e limitado, sem anúncio forçado.
- O recibo persistido foi evoluído para **formato 5**, acrescentando os dois novos valores. O decodificador continua lendo formatos **1, 2, 3 e 4**; recibos anteriores não recebem informação inventada.
- Para recibos anteriores ao formato 5, a interface mostra **detalhamento de crédito indisponível no recibo anterior** em vez de afirmar que as moedas foram perdidas.
- Não altera o registro da conquista, o depósito das moedas normais, o Cofre, as relíquias, o álbum, o desafio mensal, UMP, AdMob ou o pacote comercial.

## Testes novos de regressão

1. Três conquistas desbloqueadas com espaço na carteira: depósito integral das 26 moedas, salvo e lido no mesmo formato após reinício lógico.
2. Carteira com dívida: as moedas normais abatem dívida primeiro e as 26 moedas de conquistas também são contabilizadas como quitação, não crédito em carteira.
3. Carteira quase cheia: somente a quantidade que coube é identificada como crédito real de conquista, mesmo após reabrir o recibo ou repetir o fechamento.
4. Carteira completamente cheia: nenhuma moeda de conquista é indicada como depositada; conquista é registrada normalmente.
5. Um recibo antigo de formato 4 ainda é lido, com a informação nova marcada como indisponível.

A suíte antiga preserva testes de migração v1–v3, idempotência, falha de commit e outras economias do jogo.

## Homologação pendente

Validar visualmente em celular Android o resultado com conquistas novas, notas/carteira e crédito de cartão. Não saturar a carteira real de um jogador para reproduzir casos extremos: os testes unitários usam preferências isoladas de memória.

**Candidato QA:** `br.com.proximaera.boletorush.qa`, versionCode 51, versionName `2.3.14-qa.debug`, API mínima 26, API alvo 36. A release comercial prevista é `br.com.proximaera.boletorush / 2.3.14`, mas continua bloqueada sem assinatura de upload e configuração comercial.

## Resultado de build no Insano — 12/11/2026

- `gradlew.bat --offline :app:verifyPlayIdentity :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2 --console=plain`: **BUILD SUCCESSFUL** após a verificação adicional de recibos anteriores.
- **139 testes JUnit, 0 falhas e 0 erros**, 17 suítes. Cinco testes novos verificam créditos, dívida, limite e migração do recibo 4 para 5.
- Android Lint: **0 erros e 34 avisos**, igual ao candidato anterior.
- APK QA validado: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.14-CONQUISTAS-REAIS-QA.apk`; **17.693.973 bytes**.
- SHA-256: `1A826E7E8F8F2D4FD72A4BA9B15DC96F33406886C0BF63AC72B8316FCFD0B066`.
- `aapt`: `br.com.proximaera.boletorush.qa`, versionCode 51, versionName `2.3.14-qa.debug`, minSdk 26 e targetSdk 36.
- `apksigner verify`: APK de depuração assinado com esquema v2, um signatário. Não é um AAB de produção.
- Atalhos `TESTAR-BOLETO-RUSH-ANDROID.cmd` e `COLETAR-EVIDENCIAS-BOLETO-RUSH.cmd` atualizados no Insano, com quatro cópias da versão anterior preservadas.
- Script PowerShell sem erros de sintaxe; pré-verificação aprovada: `QA_PREFLIGHT_EXIT=0`, pacote e hash reconhecidos, `READY_DEVICES=0`. Nenhuma instalação Android, screenshot, exclusão de dados ou upload.
- **Pendente:** homologação em Android físico e fluxo completo de UMP/AdMob recompensado com IDs de teste. Nenhum AAB comercial foi gerado.
