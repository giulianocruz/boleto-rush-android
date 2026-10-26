# Boleto Rush! 🪙 — Próxima Era Games

**v1.1 beta**: jogo arcade casual Android, 100% offline, com partidas de 45 segundos. A vida cobra. Você desvia!

## Jogar
Toque ou arraste para mover a carteira. Colete moedas douradas para pontuar e aumentar o combo. Evite boletos vermelhos para preservar suas três vidas. Escudos protegem temporariamente; corações recuperam vidas. Pausar, retomar e reiniciar estão disponíveis. O recorde e o total de partidas ficam salvos apenas no aparelho. Áudio é opcional.

## Desenvolver
Abra este repositório no Android Studio com JDK 21. O projeto usa Kotlin, Jetpack Compose, Android Gradle Plugin 9.1.1, Gradle 9.3.1, Android SDK 36 e minSdk 26. Não utiliza backend, anúncios, rastreadores, APIs de IA ou permissões especiais.

- Compilar teste: `gradlew.bat :app:assembleDebug`
- APK de teste: `app/build/outputs/apk/debug/app-debug.apk`
- Compilar AAB: `gradlew.bat :app:bundleRelease`
- **Importante:** para a Google Play, o AAB deve ser assinado por uma chave de upload de propriedade do desenvolvedor. O AAB local ainda não deve ser enviado sem validar a assinatura. Preserve a chave fora do Git e faça backup.

## Publicação
A pasta `store/` contém ícone, feature graphic e capturas reais do emulador. Consulte `PLAY_STORE_LISTING.md` para textos, informações e checklist da Play Console. Veja `TESTES.md` para o registro de validação. Veja `PRIVACIDADE.md` para a política de privacidade pública.

## Testes
Android Emulator: menu, iniciar jogo, pausa, retomada e tela final funcionaram. Testes em celular físico e publicação na Google Play ainda não foram realizados.

Feito no Brasil pela Próxima Era Games.
