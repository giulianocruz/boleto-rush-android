# Boleto Rush 1.7.7 QA — comprovante persistente e resgate recuperável

## Diagnóstico

A versão 1.7.6 já atualizava moedas, notas, cartão, dívida e conquistas com um único commit. Entretanto, se o commit gravasse os dados e a Activity fosse interrompida antes de receber o resultado, a tentativa seguinte com o mesmo ID era rejeitada sem fornecer o comprovante já pago. Além disso, somente o último ID era protegido de repetição.

## Melhoria de engenharia

- `GameSettlement.settle()` grava `last_settlement_receipt_v2` juntamente com os saldos, as conquistas, o recorde e o UUID da partida, tudo em uma única transação.
- Retentar o **mesmo ID da última rodada** retorna o comprovante gravado, sem alterar saldos nem contagem de partidas.
- `settled_round_ids_v2` mantém um histórico de **até 128 IDs recentes**. ID antigo ainda nesse histórico não recebe novo crédito, mesmo após outras rodadas.
- Compatibilidade: a última rodada gravada pela 1.7.6 é incorporada ao histórico quando a primeira rodada da 1.7.7 é liquidada. Um comprovante antigo inexistente não é inventado.
- O comprovante possui formato interno versionado: partidas, recorde, moedas líquidas, notas, crédito, dívida quitada, saldos e nomes das conquistas recém-obtidas.
- O **menu inicial** exibe o último extrato salvo, inclusive após fechar e reabrir o aplicativo, mostrando moedas, notas, crédito confirmado e eventual dívida quitada. É uma consulta local, sem pagamento novo.
- O limite de 128 IDs mantém o tamanho do SharedPreferences controlado. **Não é um histórico permanente ilimitado**.
- A transação respeita `commit()`: se retornar falso, nem comprovante nem progressão são assumidos como concluídos.
- Versão Android `1.7.7-qa`, `versionCode 25`, pacote debug separado.

## Regressões

- Salvar, repetir exatamente a mesma rodada, recuperar comprovante original sem duplicar moedas.
- Completar segunda partida e tentar repetir a primeira: sem novo pagamento.
- Migrar uma última rodada do formato 1.7.6 e tentar reutilizar o ID: bloqueado.
- Completar 136 partidas fictícias e confirmar histórico limitado a 128 IDs e bloqueio de repetição recente.
- Revalidar falha de armazenamento e segunda tentativa de commit.
- Preservar testes de economia, PIX, ímã visual, resgate voluntário e quatro capítulos.

## QA antes da Play

- [x] `gradlew :app:testDebugUnitTest :app:assembleDebug` — BUILD SUCCESSFUL, compilação final em 1m48s (build-177-final.log)
- [x] JUnit 7 suítes, 39 testes, 0 falhas e 0 erros. APK `BoletoRush-1.7.7-QA.apk` salvo no Insano, Desktop/01 - Arquivos de Jogos/Boleto Rush (17.527.003 bytes; SHA256 `46297CE3619EF71858A6189A9DCC74D04CAC01F02771BD238FDA0E1BC3A72DDF`)
- [ ] Testar no Galaxy A55 com ADB quando o aparelho for conectado; em 30/10/2026 o ADB não apresentou dispositivos.
- [ ] Simular mudança de processo na tela de resultado e revisar UX
- [ ] Validar partidas de 45 e 90 segundos, anúncios recompensados, loja, cartão e dívida
- [ ] Confirmar assinatura de release, privacidade e AdMob antes de publicar

**Produção preservada.** O comprovante é local ao dispositivo; não há sincronização de nuvem.
