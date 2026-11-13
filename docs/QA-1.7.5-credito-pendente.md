# Boleto Rush 1.7.5 QA — cartões pendentes e consistência financeira

## Erro encontrado

Desde 1.7.0, a coleta do item CARTÃO persistia imediatamente +10 de limite no SharedPreferences, enquanto notas e moedas só eram consolidadas ao finalizar a partida. O jogador poderia coletar cartão e escolher MENU/DESCARTAR ou RECOMEÇAR/DESCARTAR, preservando o limite apesar de perder as outras recompensas.

## Correção

- Na rodada, cartões aumentam apenas `pendingCardCredit` (estado volátil). O crédito persistido na loja não muda enquanto o usuário joga.
- `GameEconomy.reserveCardCredit(prefs, pending, amount)` respeita o teto de exposição de 25 créditos, considerando limite já disponível e dívida.
- Encerramento por vitória, derrota ou resgate voluntário permitido consolida o crédito pendente **uma só vez**, com o restante das receitas da partida.
- Abandono/reinício sem resgate não persiste crédito.
- Texto de coleta: `CARTÃO! +N LIMITE PENDENTE`. O HUD identifica o bônus temporário e o resultado mostra quanto foi confirmado.
- Regra de amortização segue como antes: futuras moedas obtidas nas partidas pagam primeiro a dívida criada por compras de melhorias.
- VersionCode 23, versão `1.7.5-qa` e aplicativo QA separado `br.com.proximaera.boletorush.qa`.

## Regressões adicionadas

- 3 cartões em uma mesma rodada: limite pendente máximo de 25, sem persistência antes do resultado.
- Dívida de 18 e limite de 2 já disponível: só 5 de crédito adicional podem ser reservados e consolidados.
- Todo o conjunto existente continua: 200 partidas simuladas, ímã visual, resgate de metade da partida, cosméticos, milestones, amortização de dívida e PIX.

## QA

- [x] Compilação Gradle testDebugUnitTest + assembleDebug: BUILD SUCCESSFUL em 1m15s, log build-175-credit-consistency.log
- [x] Contagem de testes JUnit: 7 suítes, 32 testes, 0 falhas e 0 erros
- [x] APK salvo no Insano: Desktop/01 - Arquivos de Jogos/Boleto Rush/BoletoRush-1.7.5-QA.apk (17.358.883 bytes; SHA256 ABFE91D162224C4C9ECD829F849E2160F41A96573404155F0DBDD753B4F5C2A6). ADB não detectou aparelho.
- [ ] Validar presencialmente coleta de cartão → abandonar/reiniciar → sem crédito novo
- [ ] Validar coleta de cartão → resgatar, perder ou vencer → saldo correto na loja
- [ ] Testar simultaneamente PIX + ímã, pausa, 45 e 90 segundos
- [ ] Testar aquisição de melhoria com crédito e amortização após partidas posteriores
- [ ] Confirmar assinatura e AdMob antes de publicar

**Sem publicação na Play Store e sem merge direto na branch pública.** Todos os números são saldos fictícios do jogo.
