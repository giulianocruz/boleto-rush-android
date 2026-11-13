# Boleto Rush 2.3.8-QA — recompensa justa de anúncio opcional

## Falha encontrada

Ao concluir uma rodada, o bônus voluntário por vídeo oferecia ao jogador uma cópia integral das moedas e notas depositadas na última partida, mas a carteira é limitada a `Int.MAX_VALUE`. Em um saldo quase saturado, o aplicativo podia **prometer mais do que conseguiria creditar**; se ambas as carteiras estivessem cheias, poderia oferecer um anúncio por recompensa nula.

## Correção

- `RewardedRoundBonus.preview` calcula a **capacidade real de crédito** de notas e moedas, conferindo a identidade da rodada, recibo original e ausência de prêmio já resgatado.
- O botão de vídeo não aparece se nenhuma das duas moedas virtuais couber na carteira.
- Caso apenas parte da recompensa caiba, a mensagem informa a quantia exata e adapta o botão para **BÔNUS DISPONÍVEL** em vez de prometer uma duplicação completa.
- O callback de recompensa do anúncio recalcula os limites antes de gravar, preservando o crédito atômico por rodada, a proteção contra repetição e a impossibilidade de extrapolar o limite `Int.MAX_VALUE`.
- O bônus funciona também quando há espaço somente para notas e não para moedas.
- **Não são introduzidos anúncios forçados.** A chamada do AdMob continua opcional e sujeita à UMP. A variante QA mantém apenas IDs oficiais de teste.
- Sem alterações em conquistas, Cofre, escudo, missões, álbum, mensalidade ou dados anteriores, e sem mudança de formato de recibo.

## Testes

Quatro testes de regressão adicionados ao `EconomyTransactionsTest`:
1. Carteiras saturadas: sem oferta de anúncio nem marcação de recompensa recebida.
2. Carteiras quase saturadas: preview exato, crédito parcial correto e prevenção de duplicação.
3. Carteira de moedas cheia com espaço somente para notas: o anúncio é elegível apenas pelo prêmio em notas.
4. Saldos alterados após a prévia: crédito limitado à capacidade efetiva no callback.

Os testes anteriores também cobrem falha em `SharedPreferences.Editor.commit()`, prêmio apenas uma vez, transações do Cofre e progressão do desafio mensal.

## Homologação física pendente

- Jogar 45s e 90s em Android real; verificar a oferta de vídeo nos resultados.
- Validar UMP, anúncios de teste, recompensa somente após conclusão e nenhuma recompensa após cancelamento.
- Não tentar saturar carteiras de usuários reais para reproduzir a falha; usar somente testes unitários com preferências de memória.
- Confirmar que texto e botão continuam legíveis em telas menores.

**Release comercial:** permanece bloqueada sem chave de upload e configuração própria de AdMob. Não enviar APK debug para a Play.

## Resultado validado no Insano — 12/11/2026

- `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2 --console=plain`: **BUILD SUCCESSFUL**.
- **115 testes / 0 falhas / 0 erros**, 16 suítes, 4 casos novos de recompensa limitada à capacidade.
- **Android Lint: 0 erros / 34 avisos**, sem novos avisos em relação à 2.3.7.
- APK QA independente: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.8-BONUS-RECOMPENSAS-QA.apk`, **17.458.807 bytes**.
- SHA-256: `05DBBE155AAF5EDF7D67EBD3BBD64A5FDDB2E38130073015582D0FF1DC04D68E`.
- `aapt` validou `br.com.proximaera.boletorush.qa`, `versionCode=45`, `versionName=2.3.8-qa.debug`, `minSdk=26`, `targetSdk=36`.
- `apksigner verify`: assinatura debug válida em esquema v2, um signatário. Não é AAB assinado para envio à Play.
- Atalho de testes do Insano atualizado para a 2.3.8, com backup do anterior `TESTAR-BOLETO-RUSH-ANDROID-ANTERIOR-237.cmd`.
- `TESTAR-BOLETO-RUSH-ANDROID.cmd --preflight`: **QA_PREFLIGHT_EXIT=0**, versão, pacote e SHA-256 confirmados, **READY_DEVICES=0**. Nenhuma instalação, exclusão, execução de jogo ou upload.
- **Pendente:** teste completo e anúncios opcionais com UMP no celular real. Nenhum anúncio comercial ativado.
