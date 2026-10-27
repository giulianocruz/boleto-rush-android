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

Este documento não equivale à aprovação da Play. Registros de execução em emulador devem ser adicionados conforme concluídos.
