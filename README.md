# 🪙 Boleto Rush! — Próxima Era Games

Minigame casual de reflexos feito no Brasil. **Pegue moedas, desvie dos boletos e sobreviva 45 segundos.**

## Controles
Toque ou arraste no cenário para guiar sua carteira digital. Cada boleto tira uma das três vidas; moedas rendem pontos e aumentam o combo. O recorde fica armazenado somente no dispositivo.

## Tecnologia
- Android nativo: Kotlin + Jetpack Compose
- Android 8.0 ou superior; alvo Android SDK 36
- 100% offline, sem API, login, publicidade ou compras
- Artes vetoriais originais geradas em tempo real, sem imagens externas

## Compilar no Android Studio
Abra a pasta do projeto. Espere a sincronização do Gradle e execute `app` no emulador ou celular.
No terminal da pasta principal: `gradle :app:assembleDebug`.
O APK de testes estará em `app/build/outputs/apk/debug/app-debug.apk`.

## Publicar
Para disponibilizar diretamente, use um APK assinado. Para Google Play, gere um AAB assinado em **Build > Generate Signed App Bundle / APK** e complete a ficha, política de privacidade, classificação e testes exigidos pelo Play Console. Preserve sua chave de assinatura em local seguro e nunca a coloque no repositório.

Versão inicial: 1.0 (26/10/2026).
