# Boleto Rush! 1.5.4 QA — estabilidade na inicialização

Data: 2026-10-29. Versão Android: versionCode 12, versionName 1.5.4-qa.

## Alterações

- A tela inicial não instancia nem solicita consentimento ao SDK opcional de anúncios. O carregamento de recompensas acontece somente na tela de segunda chance (revive), após ação do jogador.
- Handler de exceções não tratadas registra, em armazenamento privado do app, um diagnóstico resumido. Na próxima inicialização, o usuário pode compartilhar o relato usando o menu do Android ou fechá-lo.
- Preserva a jogabilidade, o pacote Android e as opções existentes.

## Validação realizada

- Gradle `:app:assembleDebug --no-daemon --console=plain` aprovado em 2026-10-29.
- APK: `C:\Users\Pessoinha\Desktop\BoletoRush-1.5.4-QA-CORRECAO-INICIALIZACAO.apk`.
- SHA-256 do APK: `2E933EE52E3A53BCBBB2A50F839BA99383C5C66CA53CE55C81EA6766E7B2BDC4`.
- AAPT confirmou pacote `br.com.proximaera.boletorush`, versionCode 12 e targetSdk 36.
- Na tentativa com Android API 37 o emulador teve ANR do processo System UI; o processo do jogo permaneceu vivo no momento verificado, mas isso não valida a experiência do jogo nem substitui um teste real.

## Pendente antes de publicar

- Instalar a versão 1.5.4 em um dispositivo físico, preferencialmente o aparelho que fecha imediatamente; testar menu, tutorial, jogo e retorno ao início.
- Se voltar a fechar, abrir novamente e usar o botão COMPARTILHAR do diagnóstico para obter o stack trace.
- Testar o fluxo de consentimento e anúncio recompensado com IDs reais antes da produção.
- Não promover para produção sem teste físico, Play Console e verificação de política de dados.

Esta versão é apenas um build QA; a causa exata da falha no aparelho físico continua pendente de logcat ou diagnóstico capturado no dispositivo.
