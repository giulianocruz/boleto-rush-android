# Boleto Rush! — Próxima Era Games

[**Site do jogo**](https://giulianocruz.github.io/boleto-rush-android/) · [**Baixar a prévia Windows**](https://github.com/giulianocruz/boleto-rush-android/releases/tag/v1.5.3-qa) · [**Política de Privacidade**](https://giulianocruz.github.io/boleto-rush-android/privacy.html) · [**Reportar problema**](https://github.com/giulianocruz/boleto-rush-android/issues)

Boleto Rush! é um jogo arcade casual brasileiro: mova a carteira, colete moedas, evite boletos e tente bater seu recorde. Oferece **Clássico (45 segundos)** e **Maratona (90 segundos)**, tutorial interativo, combos, escudo, vidas extras, placar, vibração e som configuráveis.

## Status de distribuição — 29/10/2026

| Plataforma | Versão | Situação |
| --- | --- | --- |
| **Windows** | v1.4 portátil | **Disponível** na release pública v1.5.3-qa; não requer emulador nem Android Studio. Ainda sem assinatura digital de editor |
| **Android** | 1.5.3-QA, versionCode 11 | **Teste interno**: AAB assinado preparado no PC Insano, upload ao Play Console ainda não confirmado |
| **Google Play — produção** | — | **Não liberado**: faltam teste físico, IDs reais de AdMob, revisão final de consentimento/declarações e aprovação no Console |

Os testes automatizados da 1.5.3 passaram no **Android 8 / API 26** e **Android 15 / API 35** em [execução do GitHub Actions](https://github.com/giulianocruz/boleto-rush-android/actions/runs/40918581451). Os testes cobrem tutorial 3/3, alternância de vibração, partida completa e resultado, mas não substituem uma sessão manual em celular físico.

## Desenvolvimento Android

Requisitos: JDK 21, SDK Android 36 e Gradle wrapper do projeto.

```bash
./gradlew :app:assembleDebug
./gradlew :app:bundleRelease
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`. O projeto utiliza `BOLETO_ADMOB_APP_ID` e `BOLETO_REWARDED_UNIT_ID` para publicidade; sem IDs próprios, os builds QA usam identificadores de teste, que não devem ser publicados comercialmente. Para distribuição pela Play, é necessário gerar o **AAB com a chave de upload original**, mantida fora do repositório. Nunca comite chaves, senhas ou segredos.

## Desenvolvimento Windows

O código e os scripts da versão nativa Windows estão em [`windows-port/`](windows-port/). O executável público foi validado com smoke test local, mas ainda é uma versão de testes e pode provocar avisos do SmartScreen por não estar assinado digitalmente.

## Publicação e suporte

- [Guia de publicação Play Console](PLAY_CONSOLE_GUIA.md)
- [Descrição da loja](PLAY_STORE_LISTING.md)
- [Monetização e AdMob](ADMOB_E_PUBLICACAO.md)
- [Política de Privacidade](PRIVACIDADE.md)
- [Versões de teste e código-fonte](https://github.com/giulianocruz/boleto-rush-android/releases)

**Próximo passo do Android:** upload da versão 1.5.3 assinada para Teste interno, instalação pela Play em aparelho físico e revisão da monetização antes da abertura ao público.
