# Boleto Rush 2.3.0-QA — segurança do AdMob e preparação da Play Store

## Objetivo desta rodada
Congelar novas mecânicas até homologar o jogo em aparelho físico. Foco em consentimento, persistência do fluxo de anúncios recompensados e prevenção de envio acidental de AAB de demonstração.

## Melhorias
- Em `RewardedReviveAds.kt`, o botão **Opções de privacidade** invalida o anúncio carregado anteriormente. Assim, se a preferência de publicidade mudar, a próxima publicidade precisa ser solicitada sob o novo estado.
- Requisições iniciadas **antes** da mudança de consentimento recebem um token de geração. Quando a resposta chega após a alteração, o anúncio antigo é descartado e um novo só é carregado se a UMP ainda permitir.
- No instante de exibir o vídeo recompensado, são verificados estado UMP (`canRequestAds()`), consentimento resolvido, disponibilidade do anúncio e situação da Activity. Se a autorização estiver indisponível, não há exibição nem entrega de recompensa.
- `GameAdConsentGate.kt` isola as regras de elegibilidade em lógica pura, coberta por testes unitários.
- `app/build.gradle.kts`: comandos `bundleRelease` e `assembleRelease` passam a depender de `verifyPlayConfiguration`. Sem upload key e IDs próprios do AdMob, o Gradle precisa **bloquear** a geração de pacote comercial.
- Identificadores de anúncio atuais permanecem **apenas de teste**; nenhuma integração de cobrança real foi ativada.

## QA físico ainda obrigatório
1. Instalar APK QA paralelo no Android e verificar abertura sem travamento.
2. Abrir Opções de privacidade. Confirmar que não aparece anúncio enquanto o formulário UMP estiver na tela.
3. Alterar escolha da UMP; um vídeo antigo não deve ser usado depois da mudança.
4. Se a UMP negar solicitação de anúncio, o botão deve informar indisponibilidade **sem dar recompensa**.
5. Caso a UMP permita publicidade limitada/não personalizada, os anúncios de teste continuam respeitando o SDK e a escolha da pessoa.
6. Concluir um anúncio de teste depois de vencer, conferir que bônus de moedas ou fragmentos só ocorre após a confirmação de recompensa e uma vez por partida.
7. Fechar o anúncio antes da recompensa: nenhuma moeda/fragmento extra deve ser concedida.
8. Jogar Clássico/Maratona, conferir todas as ondas do Fechamento do Mês, álbum, cofre e interface em tela pequena.
9. Validar ausência de anúncios que interrompem as ondas do chefe ou o tutorial.
10. Revisar formulário de segurança de dados e privacidade da Play Console antes de solicitar teste fechado.

## Limites
- Os testes unitários não substituem a validação real do SDK de publicidade e da UMP em Android.
- Nunca enviar APK debug ou AAB com os IDs de demonstração à Play Store.
- Não divulgar, salvar no Git ou pedir por chat senhas e a keystore; a assinatura comercial deve ser configurada em cofre de segredos autorizado.

## Resultado automatizado — Insano, 12/11/2026

- `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`: **BUILD SUCCESSFUL**.
- **96 testes, zero falhas e zero erros** em 16 classes JUnit (toda a suíte anterior permanece aprovada).
- `bundleRelease --dry-run`: a lista de dependências contém `:app:verifyPlayConfiguration`.
- Execução da verificação sem credenciais comerciais: **falha intencional e esperada** com mensagem `Upload key not configured. An unsigned AAB is QA-only.`. A guarda impede que se gere um AAB comercial com credenciais ausentes; não foi criado AAB para submissão.
- APK debug QA: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.0-ADMOB-SEGURO-QA.apk`
- Arquivo: 17.440.803 bytes, SHA-256: `F55CEB45BAE4DFA562F87DD9B7960CAC391BF73C9001888257FE601C20348CCB`.
- Metadados extraídos do APK: `br.com.proximaera.boletorush.qa`, versionCode 38, versionName `2.3.0-qa.debug`, minSdk 26, targetSdk 36.
- ADB retornou **nenhum aparelho conectado**. O emulador não foi iniciado por memória RAM limitada; não há validação física de UMP/AdMob ou FPS.
- Alterações exclusivas da versão de QA; nenhum dado da Play Console nem anúncios comerciais foram modificados.
