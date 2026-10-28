# Boleto Rush! 1.5 RC1 — AdMob e publicação

**Estado em 28/10/2026:** SDK Google Mobile Ads + UMP instalados. O projeto separa IDs demonstrativos de IDs do titular, inclui gate de assinatura de upload e possui automação para APK e AAB. **Nenhuma unidade real do AdMob foi fornecida e nenhuma assinatura de publicação foi configurada.**

## Funcionamento
- O jogador perde as três vidas e recebe opção de **anúncio recompensado**.
- Com consentimento e internet, o SDK procura um anúncio.
- Se a recompensa for conquistada, volta com uma vida e invulnerabilidade inicial.
- Apenas uma segunda chance por partida. O jogador pode recusar a publicidade e encerrar a partida.
- SDK/UMP iniciam somente na tela de revive; o menu inicial abre offline.
- Não existem intersticiais obrigatórios nem compras internas.

## IDs demonstrativos — QA apenas
App ID: `ca-app-pub-3940256099942544~3347511713`

Rewarded Unit ID: `ca-app-pub-3940256099942544/5224354917`

**Esses IDs pertencem ao Google e não monetizam. Não enviar build com esses IDs à Play Console.**

## IDs do titular
Configure `BOLETO_ADMOB_APP_ID` e `BOLETO_REWARDED_UNIT_ID` como variáveis de ambiente/Secrets de compilação. O Gradle também aceita propriedades locais `boletoAdmobAppId` e `boletoRewardedUnitId` fora do repositório. Nunca cadastrar senha ou arquivo de assinatura no Git.

O código lê os IDs gerados em `R.string.admob_app_id` e `R.string.admob_rewarded_unit_id`. Verifique o conteúdo resultante na versão final.

## Pré-publicação
O task `./gradlew :app:verifyPlayConfiguration` impede a entrega com IDs demonstrativos ou sem a chave de upload. Quando há credenciais em GitHub Secrets, o workflow verifica a assinatura JAR do AAB com `jarsigner -verify` antes de disponibilizá-lo como **assinado**.

Criar/upload da própria chave no Android Studio é o caminho mais simples caso prefira não usar GitHub Secrets. Guardar senhas e backups de forma segura, fora do ChatGPT e fora do Git.

## Política, segurança de dados e público
Declarar anúncio = SIM, público-alvo real, permissão de ID publicitário se integrada, processamento de identificadores, interações, diagnósticos e IP conforme a declaração do SDK. Disponibilizar política de privacidade no app e na ficha. UMP requer configurar mensagens e consentimento no AdMob conforme os mercados e o público escolhido.

Referências:
- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/rewarded
- https://developers.google.com/admob/android/privacy/play-data-disclosure
- https://support.google.com/googleplay/android-developer/answer/10787469

## Instruções completas
Veja [PLAY_CONSOLE_GUIA.md](PLAY_CONSOLE_GUIA.md).
