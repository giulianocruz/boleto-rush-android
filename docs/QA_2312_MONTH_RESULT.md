# Boleto Rush 2.3.12-QA — resultado mensal sem informação falsa

## Falha reproduzível na lógica anterior

Quando o jogador completava o Fechamento do Mês sem contatos e com os seis perigos resolvidos, mas **não recebia um selo**, a tela sempre afirmava que o selo mensal já tinha sido conquistado. Isso não era necessariamente verdade: a partida pode começar no último dia de um mês e terminar após a meia-noite, fazendo com que a chave de calendário `challengeMonth` da partida fique diferente do calendário no momento de salvar. A regra atual não concede o selo nesse caso, mas a mensagem anterior não distinguia as causas.

## Correção sem mudar prêmios

O novo método `GameMonthChallenge.noStampExplanation` gera uma explicação específica para:
- Contato com perigo do chefe (exige zero contatos para conquistar o selo);
- Perigos ou ondas não concluídos;
- Mudança do mês entre início e conclusão da partida;
- Selo do mês atual já recebido;
- Limite máximo de selos atingido;
- Relógio do aparelho anterior ao último selo salvo;
- Situação inesperada, sem afirmar falsamente que já existe selo.

`MainActivity.kt` usa esse resultado na tela de fim de jogo somente quando a partida venceu e entrou na última onda, **sem alterar** `GameMonthChallenge.plan`, `GameSettlement.settle`, o histórico, as moedas, o saldo, os anúncios ou a concessão do selo.

## Novos testes

Quatro cenários de regressão em `GameMonthChallengeTest`:
1. Mudança de novembro para dezembro não pode exibir **JÁ CONQUISTADO**;
2. **JÁ CONQUISTADO** só pode ser informado quando o último mês registrado coincide com o mês atual;
3. Falhas por contato ou perigos incompletos têm prioridade sobre informação de calendário;
4. Diferencia relógio do aparelho retrocedido e limite máximo de selos.

## Pendências da homologação física

A lógica pode ser verificada por testes locais, mas o texto ainda precisa de inspeção em tela pequena. O modo Clássico 45s e a Maratona 90s, consentimento UMP e AdMob opcional devem ser verificados em Android real. Não alterar o relógio do aparelho do usuário para forçar o cenário: os testes unitários usam parâmetros de mês controlados e não exigem essa manipulação.

**Candidata:** `br.com.proximaera.boletorush.qa`, `versionCode=49`, `versionName=2.3.12-qa.debug`, minSdk 26, targetSdk 36. A versão comercial correspondente seria `br.com.proximaera.boletorush / 2.3.12`, ainda **não assinada nem enviada à Google Play**.

## Resultado confirmado no Insano — 12/11/2026

- `gradlew.bat --offline :app:verifyPlayIdentity :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2 --console=plain`: **BUILD SUCCESSFUL**.
- **130 testes unitários aprovados, zero falhas, zero erros**, em 17 suítes. Quatro testes novos cobrem mensagens de resultado do selo mensal.
- **Android Lint: 0 erros e 34 avisos**, sem aumento de avisos.
- APK QA debug no Insano: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.12-RESULTADO-MENSAL-QA.apk`.
- 17.458.803 bytes; SHA-256 `E9DA690291B5DA79CB4DCBD11206A3A183C7DB1DC13D86622BEDFDA181CF3085`.
- `aapt`: `br.com.proximaera.boletorush.qa`, versionCode 49, versionName `2.3.12-qa.debug`, minSdk 26, targetSdk 36.
- `apksigner verify`: assinatura **debug** v2 válida, um signatário; não é AAB comercial assinado.
- Os dois atalhos no Insano (instalar/abrir e coletar evidências pós-partida) foram atualizados para 2.3.12. Backups da versão 2.3.11 preservados.
- Parser PowerShell sem erros; pré-verificação do atalho `PREFLIGHT_EXIT=0`, reconheceu pacote, versão, SHA-256 e ADB; `READY_DEVICES=0`, sem aparelho instalado ou alterado.
- Não foram gerados screenshots nem testados fluxos UMP/AdMob em dispositivo. A PR integrada permanece em rascunho.
