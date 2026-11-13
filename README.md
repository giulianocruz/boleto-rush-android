# Boleto Rush! — Próxima Era Games

[**Site do jogo**](https://giulianocruz.github.io/boleto-rush-android/) · [**Baixar a prévia Windows**](https://github.com/giulianocruz/boleto-rush-android/releases/tag/v1.5.3-qa) · [**Política de Privacidade**](https://giulianocruz.github.io/boleto-rush-android/privacy.html) · [**Reportar problema**](https://github.com/giulianocruz/boleto-rush-android/issues)

Boleto Rush! é um jogo arcade casual brasileiro: mova a carteira, colete moedas, evite boletos e tente bater seu recorde. Oferece **Clássico (45 segundos)** e **Maratona (90 segundos)**, tutorial interativo, combos, escudo, vidas extras, placar, vibração e som configuráveis.

## Status de distribuição — 12/11/2026

| Plataforma | Versão | Situação |
| --- | --- | --- |
| **Windows** | v1.4 portátil | **Prévia histórica** na release pública v1.5.3-qa; ainda sem assinatura digital de editor |
| **Android** | **2.3.11-QA**, versionCode **48**, API alvo **36** | **Candidato integrado em revisão**; APK de depuração validado no Insano, **126 testes unitários** aprovados; sem homologação física |
| **Google Play — produção** | — | **NÃO PUBLICAR**: falta celular físico, IDs reais de AdMob, assinatura de upload, capturas reais atualizadas, revisão das declarações de privacidade e testes da Play |

A versão Android atual reúne colisões, Raspão, relíquias, Cofre, coleção permanente, missão semanal, Fechamento do Mês e anúncios recompensados opcionais com consentimento UMP. Os IDs de anúncios ainda são de **teste**. O teste automatizado local de 111 casos **não substitui** uma sessão completa em Android físico.

**Branch integrada para revisão:** `release/boleto-rush-play-qa-20261112` (proposta direcionada à `main`; não mesclada). [Checklist de lançamento e bloqueios](docs/LANCAMENTO_236_CONSOLIDADO.md). As PRs anteriores foram mantidas como histórico de QA.

## Desenvolvimento Android

Requisitos: JDK 21, SDK Android 36 e Gradle wrapper do projeto.

```bash
./gradlew :app:assembleDebug
./gradlew :app:bundleRelease
```

No Windows, use `gradlew.bat` no lugar de `./gradlew`. **A variante debug/QA usa sempre os IDs oficiais de demonstração do AdMob**, inclusive quando variáveis de ambiente comerciais estiverem presentes. A variante `release` exige `BOLETO_ADMOB_APP_ID`, `BOLETO_REWARDED_UNIT_ID` e a **chave de upload do titular** configurados fora do repositório. Sem isso, `bundleRelease` é bloqueado intencionalmente. O workflow GitHub Actions compila somente o APK QA caso não existam segredos comerciais completos; não gera AAB não assinado. Nunca comite chaves, senhas ou segredos.

## Desenvolvimento Windows

O código e os scripts da versão nativa Windows estão em [`windows-port/`](windows-port/). Essa versão é uma prévia histórica: não inclui necessariamente todas as melhorias recentes do Android e pode provocar avisos do SmartScreen por não estar assinada digitalmente.

## Publicação e suporte

- [Guia de publicação Play Console](PLAY_CONSOLE_GUIA.md)
- [Descrição da loja](PLAY_STORE_LISTING.md)
- [Monetização e AdMob](ADMOB_E_PUBLICACAO.md)
- [Política de Privacidade](PRIVACIDADE.md)
- [Versões de teste e código-fonte](https://github.com/giulianocruz/boleto-rush-android/releases)

**Próximo passo do Android:** instalar a **2.3.11-QA** em um celular conectado ao Insano, verificar partidas completas de 45/90 segundos e todos os recursos, testar no app o link da política de privacidade já acessível publicamente, configurar os IDs reais do AdMob e a assinatura de upload do titular. Somente depois gerar AAB comercial e começar os testes internos/fechados na Play. [Plano de homologação consolidado](docs/LANCAMENTO_236_CONSOLIDADO.md).

**Auditoria de compatibilidade da QA 2.3.7:** as regras de backup/transferência e o ícone monocromático foram atualizados para versões modernas do Android. Leia [QA Android 2.3.7](docs/QA_237_ANDROID_COMPAT.md). O progresso continua local e não é sincronizado automaticamente entre aparelhos.

**Ajuste da QA 2.3.8:** o bônus opcional por vídeo só aparece com recompensa efetivamente disponível, mostrando exatamente os valores de notas e moedas que cabem na carteira. [Roteiro de validação](docs/QA_238_AD_REWARD_FAIRNESS.md).

**Correção da QA 2.3.9:** o relógio de jogo só avança se a Activity estiver visível e a partida ativa. Isso evita que um vídeo recompensado consuma tempo da rodada enquanto a tela ainda está coberta pelo anúncio. [Testes e limitações](docs/QA_239_CLOCK_FOREGROUND.md).

**Identidade de release validada:** a variante comercial está preparada com `br.com.proximaera.boletorush / 2.3.9`, e a QA preserva `br.com.proximaera.boletorush.qa / 2.3.9-qa.debug`. O Gradle testa automaticamente essa separação por `:app:verifyPlayIdentity`, sem liberar publicação. [Auditoria da ficha e do manifesto](docs/QA_240_PLAY_IDENTITY.md).

**Kit de homologação ampliado:** além do instalador de QA, o Insano dispõe do atalho `COLETAR-EVIDENCIAS-BOLETO-RUSH.cmd`. Ele salva uma captura da tela e logs de processo **após** as partidas, sem reinstalar ou reiniciar o jogo, e só captura se o pacote QA estiver visível. [Como usar](docs/QA_239_POS_PARTIDA_WINDOWS.md).

**QA 2.3.10 — recibo correto:** o fechamento da partida agora informa apenas moedas e notas realmente creditadas, inclusive nas recompensas semanais/mensais, respeitando o limite de carteira. A identidade comercial e a QA continuam separadas. [Testes e limitações](docs/QA_2310_RECEIPTS.md).

**Novo na QA 2.3.11:** o Fechamento do Mês ganhou indicador de progresso dos seis perigos, aviso de selo em risco e confirmação ao concluir cada onda. [Roteiro de testes](docs/QA_2311_BOSS_PROGRESS.md).
