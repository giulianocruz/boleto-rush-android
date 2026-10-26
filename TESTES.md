# Registro de validação — Boleto Rush! 1.1
**Data:** 26/10/2026

## Executado e aprovado
- Gradle assembleDebug + bundleRelease: BUILD SUCCESSFUL.
- APK instalado no emulador oficial Android, resolução 1080x2400.
- Aplicativo abre e mostra menu com opções de som e jogar.
- Botão JOGAR AGORA abre HUD com pontuação, cronômetro, vidas e PAUSAR.
- PAUSAR exibe menu com CONTINUAR e outros controles.
- CONTINUAR retorna para partida.
- Rodada executou até a tela de fim de jogo 'A FATURA CHEGOU!', pontuação 97, recorde atualizado.
- Capturas reais obtidas via ADB: menu e jogo.
- Nenhum crash fatal identificado na leitura anterior de logcat após a inicialização.

## Ainda pendente
- Teste no aparelho Android físico e avaliação de tamanho de UI e áudio.
- Testes de escudo, vida extra, compartilhamento, rotação/tela de sistema e retorno após segundo plano.
- Teste com diferentes resoluções e acessibilidade.
- Gerar bundle release com assinatura de upload guardada pelo responsável.
- Preencher cadastro, declarações e classificação no Google Play Console.
- Eventual teste fechado obrigatório por 14 dias e 12 testadores, conforme a conta.

## Artefatos
- app/build/outputs/apk/debug/app-debug.apk — para testes externos (assinado com chave debug).
- app/build/outputs/bundle/release/app-release.aab — candidato local, requer checagem/assinatura antes da Play.
- store/ — peças promocionais.
