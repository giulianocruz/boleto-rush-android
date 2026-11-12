# Boleto Rush! — Próxima Era Games

[**Site do jogo**](https://giulianocruz.github.io/boleto-rush-android/) · [**Baixar a prévia Windows**](https://github.com/giulianocruz/boleto-rush-android/releases/tag/v1.5.3-qa) · [**Política de Privacidade**](https://giulianocruz.github.io/boleto-rush-android/privacy.html) · [**Reportar problema**](https://github.com/giulianocruz/boleto-rush-android/issues)

Boleto Rush! é um jogo arcade casual brasileiro: mova a carteira, colete moedas, evite boletos e tente bater seu recorde. Oferece **Clássico (45 segundos)** e **Maratona (90 segundos)**, tutorial interativo, combos, escudo, vidas extras, placar, vibração e som configuráveis.

## Status de distribuição — 12/11/2026

| Plataforma | Versão | Situação |
| --- | --- | --- |
| **Windows** | v1.4 portátil | **Prévia histórica** na release pública v1.5.3-qa; ainda sem assinatura digital de editor |
| **Android** | **2.3.6-QA**, versionCode **43**, API alvo **36** | **Candidato integrado em revisão**; APK de depuração validado no Insano, **111 testes unitários** aprovados; sem homologação física |
| **Google Play — produção** | — | **NÃO PUBLICAR**: falta celular físico, IDs reais de AdMob, assinatura de upload, capturas reais atualizadas, validação de política de privacidade e testes da Play |

A versão Android atual reúne colisões, Raspão, relíquias, Cofre, coleção permanente, missão semanal, Fechamento do Mês e anúncios recompensados opcionais com consentimento UMP. Os IDs de anúncios ainda são de **teste**. O teste automatizado local de 111 casos **não substitui** uma sessão completa em Android físico.

**Branch integrada para revisão:** `release/boleto-rush-play-qa-20261112` (proposta direcionada à `main`; não mesclada). [Checklist de lançamento e bloqueios](docs/LANCAMENTO_236_CONSOLIDADO.md). As PRs anteriores foram mantidas como histórico de QA.

## Desenvolvimento Android

Requisitos: JDK 21, SDK Android 36 e Gradle wrapper do projeto.

```bash
./gradlew :app:assembleDebug
./gradlew :app:bundleRelease
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`. O projeto utiliza `BOLETO_ADMOB_APP_ID` e `BOLETO_REWARDED_UNIT_ID` para publicidade; sem IDs próprios, os builds QA usam identificadores de teste, que não devem ser publicados comercialmente. Para distribuição pela Play, é necessário gerar o **AAB com a chave de upload original**, mantida fora do repositório. Nunca comite chaves, senhas ou segredos.

## Desenvolvimento Windows

O código e os scripts da versão nativa Windows estão em [`windows-port/`](windows-port/). Essa versão é uma prévia histórica: não inclui necessariamente todas as melhorias recentes do Android e pode provocar avisos do SmartScreen por não estar assinada digitalmente.

## Publicação e suporte

- [Guia de publicação Play Console](PLAY_CONSOLE_GUIA.md)
- [Descrição da loja](PLAY_STORE_LISTING.md)
- [Monetização e AdMob](ADMOB_E_PUBLICACAO.md)
- [Política de Privacidade](PRIVACIDADE.md)
- [Versões de teste e código-fonte](https://github.com/giulianocruz/boleto-rush-android/releases)

**Próximo passo do Android:** instalar a **2.3.6-QA** em um celular conectado ao Insano, verificar partidas completas de 45/90 segundos e todos os recursos, confirmar publicamente a URL de política de privacidade, configurar os IDs reais do AdMob e a assinatura de upload do titular. Somente depois gerar AAB comercial e começar os testes internos/fechados na Play. [Plano de homologação consolidado](docs/LANCAMENTO_236_CONSOLIDADO.md).
