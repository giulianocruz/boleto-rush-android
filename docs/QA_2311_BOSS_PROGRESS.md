# Boleto Rush 2.3.11-QA — feedback do desafio Fechamento do Mês

## Melhoria de jogabilidade

O desafio mensal já tinha três ondas com corredores seguros e seis perigos totais. Contudo, o jogador não acompanhava claramente **quantos perigos já foram resolvidos** e só descobria no resultado se havia perdido a chance de conquistar o selo por contato com os perigos.

Agora, durante o desafio:

- O HUD exibe **PERIGOS X/6** atualizado conforme obstáculos são desviados ou removidos legitimamente pelo poder limpa-tela.
- A mensagem **SEM CONTATOS** confirma que o jogador ainda mantém a possibilidade de conquistar o selo; **SELO EM RISCO** aparece imediatamente após tocar em um dos perigos do chefe.
- Cada onda resolvida produz um feedback breve, sem recompensa monetária automática e sem interromper a partida. O aviso é disparado no máximo uma vez por onda.
- O painel da pista verde e a tela de resultado também mostram a contagem. Isso deixa as condições do selo explícitas.
- O contador é limitado a seis, e mensagens de onda só são liberadas depois de dois perigos resolvidos **e** de a onda correspondente ter sido iniciada.

Nenhuma alteração em: valores de moedas, loja, ganhos de anúncios, UMP, formatos de dados salvos, duração do Clássico/Maratona, critérios de concessão de selos e prêmios do mês.

## Testes novos

Os quatro testes de regressão em `GameMonthChallengeTest` cobrem:

1. Texto correto para 0/6, progresso parcial, conclusão e selo em risco.
2. Nenhum aviso de onda antes de resolvidos os dois perigos correspondentes; ondas não lançadas não podem aparecer concluídas.
3. Contagens negativas ou maiores que seis nunca escapam dos limites.
4. Mensagens distinguem vitória limpa e contato com perigos, sem sugerir prêmio de dinheiro.

## Teste visual pendente

No celular: jogar Clássico 45s e Maratona 90s, observar as três ondas, manter-se na pista verde, verificar o contador, e testar um contato intencional com escudo em uma rodada separada. O selo exige **zero contatos**, independentemente da proteção. Confirmar legibilidade do HUD em telas menores, localização do aviso e que o resumo final corresponde às seis resoluções.

**Versão candidata de QA:** `2.3.11-qa.debug`, versionCode 48, pacote paralelo `br.com.proximaera.boletorush.qa`, minSdk 26 e targetSdk 36. A release comercial continua bloqueada até homologação física e credenciais do titular.

## Resultado efetivo no Insano — 12/11/2026

- Comando: `gradlew.bat --offline :app:verifyPlayIdentity :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2 --console=plain`: **BUILD SUCCESSFUL**.
- **126 testes JUnit aprovados, zero falhas, zero erros**, 17 suítes. Quatro testes novos validam textos e limites das ondas.
- **Android Lint: 0 erros, 34 avisos**.
- APK QA debug: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.11-FECHAMENTO-MENSAL-QA.apk`; tamanho 17.458.803 bytes.
- SHA-256: `CCF499F2048700D573A621AF69B0A0C2A3D4A7CCC1D6265E1544E17465ABD02A`.
- `aapt`: `br.com.proximaera.boletorush.qa`, versionCode 48, versionName `2.3.11-qa.debug`, minSdk 26, targetSdk 36.
- `apksigner`: assinatura debug v2 validada, um signatário. Não é bundle ou chave comercial para a Google Play.
- Atalhos `TESTAR-BOLETO-RUSH-ANDROID.cmd` e `COLETAR-EVIDENCIAS-BOLETO-RUSH.cmd` da Área de Trabalho atualizados. Backups 2.3.10 preservados.
- Pré-verificação: `QA_PREFLIGHT_EXIT=0`; pacote, versão e hash corretos; `READY_DEVICES=0`, portanto nenhum teste físico, screenshot ou instalação foi realizado.
- A PR integrada #39 continua em rascunho contra a `main`.
