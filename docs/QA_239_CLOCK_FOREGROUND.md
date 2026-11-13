# Boleto Rush 2.3.9-QA — relógio visível e anúncios recompensados

## Problema confirmado pela leitura do fluxo

O loop de jogo estava sob `LaunchedEffect(round, screen)`, sem incluir o estado `foreground` na chave e nas condições. Durante um anúncio recompensado em tela cheia, o Android coloca a Activity em segundo plano. Se o callback de recompensa mudar a tela para `PLAY` antes de o anúncio fechar, a coroutine pode iniciar o relógio **enquanto o jogador não enxerga a partida**. O efeito de pausa automática dependia apenas de uma mudança em `foreground` e podia já ter sido executado quando o callback ocorresse.

## Correção mínima

- O relógio só avança quando `GameRunClock.shouldAdvance(foreground, playing)` retorna verdadeiro.
- O efeito agora usa `LaunchedEffect(round, screen, foreground)`. Quando a Activity sai de primeiro plano, a coroutine é cancelada; na volta, um novo `withFrameNanos` define a referência para o próximo quadro.
- O loop também verifica `foreground` em cada iteração, impedindo que mudanças de tela disparadas por anúncios consumam tempo fora da tela.
- `GameRunClock.frameSeconds` evita tempos negativos e limita o avanço a **50 ms por quadro**, reduzindo efeitos de travamento e retorno de aplicativos.
- Permanecem inalterados: a pausa manual, duração das partidas 45/90 segundos, colisões, recompensas, Cofre, UMP e os IDs de teste do AdMob.

## Testes automatizados novos

1. O relógio pode avançar somente com tela de partida ativa **e** aplicativo visível.
2. Se um callback de vídeo retorna à tela de jogo ainda em segundo plano, o relógio não deve iniciar.
3. Após intervalos longos, o passo de tempo é limitado a 50 ms.
4. Carimbos de tempo iguais ou não monotônicos não geram delta negativo.

## Limitações para o lançamento

Estes testes unitários provam a regra do relógio isoladamente. Não substituem a validação no Android físico com um vídeo completo, um vídeo cancelado e o ato de voltar de outro aplicativo. Continuam pendentes: homologação real, acessibilidade, desempenho, capturas da Play, IDs comerciais e assinatura de upload protegida fora do Git.

**Candidato:** `2.3.9-qa.debug`, `versionCode=46`, API mínima 26, API alvo 36, APK de teste independente `br.com.proximaera.boletorush.qa`.

## Evidências finais — Insano, 12/11/2026

- `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2 --console=plain`: **BUILD SUCCESSFUL**.
- **118 testes JUnit aprovados, zero falhas e zero erros**, 17 suítes.
- **Android Lint: 0 erros e 34 avisos**; nenhum aviso novo relativo ao controle do relógio.
- APK de QA paralelo: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.9-RELOGIO-SEGURO-QA.apk`.
- **17.458.807 bytes**; SHA-256: `DB6EEB2352DCA3ABBB7601DBE8511D33C7B76AAD839A8B09930F81FF9FB28E07`.
- `aapt`: pacote `br.com.proximaera.boletorush.qa`, `versionCode=46`, `versionName=2.3.9-qa.debug`, `minSdk=26`, `targetSdk=36`.
- `apksigner verify`: assinatura **debug** válida no esquema APK v2; não é assinatura comercial para Google Play.
- O atalho `TESTAR-BOLETO-RUSH-ANDROID.cmd` do Insano agora aponta para a 2.3.9. Cópia anterior preservada em `TESTAR-BOLETO-RUSH-ANDROID-ANTERIOR-238.cmd`.
- `--preflight`: **PREFLIGHT_EXIT=0**, SHA-256 e versão reconhecidos, `READY_DEVICES=0`; sem instalação, inicialização ou alteração de dados no Android.
- **Pendente:** teste no celular real com anúncios opcionais, alternância de aplicativos e leitura do relógio. A validação automatizada não comprova FPS, aparência ou fluxo real do SDK AdMob.
