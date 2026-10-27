# Boleto Rush! 🪙 — Próxima Era Games

Minigame Android de reflexos, com partidas rápidas e humor brasileiro. Jogue offline, sem login, anúncios ou compras.

## Versão 1.2 — dois modos e melhor feedback
- **Clássico:** sobreviva 45 segundos.
- **Maratona:** sobreviva 90 segundos, com dificuldade progressiva.
- **Como jogar:** tutorial acessível a partir do menu.
- **Novos indicadores:** feedback de coleta, acertos e poderes; estatísticas ao término.
- **Recordes independentes:** o recorde do Clássico anterior é preservado.
- **Pausa segura:** permite retomar ou reiniciar; o app pausa ao ir para segundo plano.
- **Efeitos sonoros opcionais e compartilhamento** da pontuação.

## Como jogar
Toque ou arraste para mover a carteira. Pegue moedas para aumentar o combo e desvie dos boletos que tiram vidas. O escudo protege contra um boleto; o coração recupera uma vida (máximo de 3).

## Compilação e requisitos
Abra a pasta no Android Studio com JDK 21, Gradle 9.3.1, AGP 9.1.1, Kotlin e Jetpack Compose. SDK de compilação e target: 36; minSdk: 26.

```
gradlew.bat :app:assembleDebug
gradlew.bat :app:bundleRelease
```

APK de testes: `app/build/outputs/apk/debug/app-debug.apk`. AAB local: `app/build/outputs/bundle/release/app-release.aab`. **O bundle precisa estar assinado com a chave de upload do desenvolvedor antes de ser submetido à Google Play.** Nunca publique senhas ou chaves no Git.

## Publicação
- `PLAY_STORE_LISTING.md` — descrição e checklist Google Play.
- `PRIVACIDADE.md` — política de privacidade.
- `store/` — artes e capturas de tela (validar novamente as capturas para a v1.2).
- `TESTES.md` — registro de QA.

## Próxima etapa: identidade visual premium
O pacote de produção está em [`arte_premium/`](arte_premium/README.md), incluindo uma bíblia visual, 22 prompts individuais para o Flow, um prompt único para Flow Agent, prompts de vídeo/som e inventário de integração. **As novas artes ainda precisam ser geradas, aprovadas e importadas no aplicativo**; a v1.2 atual permanece a beta jogável.

Feito no Brasil pela Próxima Era Games.
