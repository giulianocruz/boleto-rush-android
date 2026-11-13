# Boleto Rush 2.3.15-QA — anúncio da etapa e retomada segura

## Problema identificado no código

O vídeo recompensado da tela de conclusão de etapa tinha o crédito condicionado a `screen == Screen.CHAPTER_BONUS` no instante do callback do Google. Se o usuário saísse desse painel antes de o anúncio finalizar (por exemplo, no intervalo da navegação de retorno), podia assistir até o fim e não receber o prêmio de notas cosméticas.

Além disso, a pausa automática em segundo plano observava só `foreground`: um callback que restaurasse `Screen.PLAY` **depois** que o aplicativo já estava em background não disparava nova tentativa de pausa. O relógio não avançava invisivelmente (já protegido desde 2.3.9), mas a partida podia voltar diretamente à ação no próximo primeiro plano sem confirmação do usuário.

## Correção aplicada

- O prêmio da etapa é vinculado ao **ID da partida e ao número da etapa** no momento da solicitação. Ao receber o callback de anúncio assistido, o sistema verifica partida correspondente, etapa concluída, não encerrada e ainda não premiada. O estado atual da tela pode ser o painel da etapa, jogo ou pausa.
- O crédito da etapa continua **no máximo uma vez por etapa**, sempre em **notas virtuais de visuais**, e não vai para outra partida nem para uma partida encerrada.
- Se a partida atual está no painel de bônus quando o vídeo termina, a navegação segue para JOGAR se o app estiver em primeiro plano, ou PAUSA se estiver em segundo plano.
- A pausa automática agora observa **tanto o ciclo de vida quanto o estado da tela**. Mesmo que outro callback coloque o jogo em PLAY fora do primeiro plano, a tela é devolvida à PAUSA.
- Uma falha do vídeo que pertence a uma rodada anterior não coloca erro de publicidade na nova rodada.
- O bônus segue opcional, sem anúncios intersticiais forçados. Os valores, moedas, progresso permanente, UMP e persistência de recompensas de fim de rodada não foram alterados.

## Cinco testes novos

1. Prêmio pode ser creditado após o jogador deixar o painel da etapa, desde que permaneça na mesma partida ativa.
2. Callback antigo não dá notas em uma nova rodada, rodada já finalizada, menu abandonado ou ID vazio.
3. Uma etapa já premiada não pode receber crédito repetido.
4. Etapa ainda não completada ou inválida não é elegível.
5. Estado PLAY em segundo plano exige pausa automática mesmo se foi provocado pelo callback após o Activity ter passado para background.

## Validação física pendente

O teste unitário valida as condições da lógica, mas **não simula o SDK real do Google AdMob nem os eventos do Android**. Em aparelho autorizado, testar vídeo recompensado completo e interrompido, navegação ao fechar, retorno ao primeiro plano e não duplicação da recompensa. Não registrar ou monetizar anúncio de produção em QA.

**Versão QA prevista:** `br.com.proximaera.boletorush.qa`, versionCode 52, versionName `2.3.15-qa.debug`; minSdk 26 e targetSdk 36. A futura variante comercial seria `br.com.proximaera.boletorush / 2.3.15`, ainda bloqueada sem homologação e assinatura.

## Evidências da compilação — Insano, 12/11/2026

- `gradlew.bat --offline :app:verifyPlayIdentity :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2 --console=plain` → **BUILD SUCCESSFUL**.
- **144 testes JUnit, 0 falhas, 0 erros**, 17 suítes, incluindo cinco testes novos sobre callbacks e tela em segundo plano.
- **Android Lint: 0 erros e 34 avisos**.
- APK QA: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.15-VIDEO-SEGURO-QA.apk`, **17.458.807 bytes**.
- SHA-256: `80095ADCB4CB435BF537D1A18FDA4761851D64F0A7466D557D826D9232DECB91`.
- `aapt`: pacote `br.com.proximaera.boletorush.qa`, code 52, versionName `2.3.15-qa.debug`, minSdk 26, targetSdk 36.
- `apksigner verify`: assinatura **debug v2** validada, um signatário. Não é AAB comercial.
- Atalhos `TESTAR-BOLETO-RUSH-ANDROID.cmd` e `COLETAR-EVIDENCIAS-BOLETO-RUSH.cmd` atualizados no Insano, com quatro backups de 2.3.14.
- Parser PowerShell sem erros. Pré-verificação real `PREFLIGHT_EXIT=0`; APK/versão/hash reconhecidos, `READY_DEVICES=0`, nenhuma instalação.
- O jogo e SDK de anúncios **ainda não foram testados fisicamente em Android**. A confirmação automatizada não equivale ao fluxo AdMob/UMP real.
