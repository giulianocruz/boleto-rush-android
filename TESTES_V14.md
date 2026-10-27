# Registro de validação — Boleto Rush! v1.4

Data: 27/10/2026.

## Build
- Kotlin/Compose + AdMob 25.5.0 + UMP 4.0.0 com targetSdk 36 e minSdk 26.
- Cache incremental Kotlin corrompido foi desativado; compilador passou a executar no processo (kotlin.compiler.execution.strategy=in-process).
- `assembleDebug` retornou **BUILD SUCCESSFUL**.
- APK gerado com versionCode 5 / versionName 1.4.

## Escopo implementado
- Sete novas imagens arquivadas e categorizadas.
- Nova cena de ação de menu de resultados/revive, em imagem otimizada.
- Anúncio de TESTE AdMob recompensado após game over, opt-in, uma chance por rodada, sem recompensa caso vídeo não seja concluído.
- Política UMP de consentimento, com botão de privacidade quando exigido.

## Pendente antes de habilitar anúncios comerciais
- Teste de toque em aparelho físico de resolução variada.
- Validar rede/UMP/AdMob no emulador Google Play e em celular conectado.
- Testar morte por perda de vidas, exibição de vídeo e validação de retorno com 1 vida; se não carregar, deve ser possível encerrar.
- Testar retomada de aplicativo após vídeo/tela de consentimento.
- AAB de produção com chave do titular, classificação e segurança de dados da Play Store.

## Tentativa real no emulador em 27/10/2026
- Emulador Android API 37 iniciou (sys.boot_completed=1).
- APK instalado no pacote br.com.proximaera.boletorush; confirmado com `adb shell pm path`.
- O Android virtual apresentou a caixa `Application Not Responding: com.android.systemui`. O app não ficou disponível para inspeção por UI Automator.
- Não apareceu uma `FATAL EXCEPTION` na amostra consultada do AndroidRuntime, mas não é suficiente para aprovar o app.
- Teste funcional do AdMob, tela de revive, recordes e estabilidade em aparelho físico: **ainda pendente**.

Este documento não equivale à aprovação da Play. O GitHub Release é uma versão beta para testes manuais.
