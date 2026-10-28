# Boleto Rush! — Próxima Era Games

**Android v1.5.0 RC1 (28/10/2026)** — versão candidata de testes, **não homologada para produção na Google Play**.

Arcade casual brasileiro, com o mascote carteira caramelo, arte original premium, controles por toque, moedas, boletos, escudo, vida extra e placar. **Clássico (45s)** e **Maratona (90s)**, recordes independentes, tutorial e som opcional.

## Monetização e privacidade
Após perder todas as vidas, o jogador pode **voluntariamente** assistir a um anúncio recompensado e voltar com uma vida, uma vez por partida. Sem anúncios intersticiais obrigatórios. O jogo funciona offline e só tenta inicializar anúncios na tela de segunda chance. O SDK Google Mobile Ads e UMP usam rede e podem processar identificadores e diagnósticos. A política de privacidade está linkada no menu do aplicativo.

**IMPORTANTE:** o código usa IDs oficiais **de teste** do Google até o titular fornecer os IDs reais via configuração de build; essa versão não monetiza.

## Compilação automatizada — duas versões
O workflow [Android RC (APK + AAB)](https://github.com/giulianocruz/boleto-rush-android/actions/workflows/android-release-candidate.yml) produz:
- `BoletoRush-1.5.0-rc1-APK-TESTE`: APK debug para instalar e testar.
- `BoletoRush-1.5.0-rc1-AAB-NAO-ASSINADO`: AAB de QA, **não pode ser submetido ao Play Console**.
- `BoletoRush-1.5.0-rc1-AAB-ASSINADO`: gerado somente quando a conta tiver chave de upload e IDs AdMob reais registrados como segredos de Actions, e as verificações de pré-publicação passarem.
- `BoletoRush-1.5.0-rc1-KIT-LOJA`: arte e guia.

O workflow [Teste de abertura no emulador](https://github.com/giulianocruz/boleto-rush-android/actions/workflows/android-smoke.yml) instala e abre o app, captura imagens **reais** de menu/gameplay e verifica encerramento inesperado.

## Construir localmente
Requisitos: JDK 21, Android SDK 36, Gradle 9.3.1 (wrapper) e Android Gradle Plugin 9.1.1.

```bash
bash ./gradlew :app:assembleDebug :app:bundleRelease
```

Para preparar produção, configurar `BOLETO_ADMOB_APP_ID`, `BOLETO_REWARDED_UNIT_ID` e a chave de upload em ambiente privado; `:app:verifyPlayConfiguration` bloqueia IDs demonstrativos e ausência de chave. Não colocar `.jks`, senhas ou credenciais no GitHub.

## Google Play Console
**[GUIA PASSO A PASSO — PLAY_CONSOLE_GUIA.md](PLAY_CONSOLE_GUIA.md)**

- [Texto e checklist da loja](PLAY_STORE_LISTING.md)
- [Política de privacidade](PRIVACIDADE.md)
- [Configuração de AdMob](ADMOB_E_PUBLICACAO.md)
- [Histórico de testes v1.4](TESTES_V14.md)

Arte oficial para a Play:
- Ícone transparente correto: `store/play-icon-512.png`.
- Banner 1024×500: `store/feature-graphic-1024x500.jpg`.
- Capturas: atualizar para capturas reais da **RC atual** após validação no emulador/celular.

### Pendências reais
Verificar abertura em celular físico (houve reclamação de travamento na beta antiga), final de partida nos dois modos, anúncio recompensado e consentimento; preparar uploads/assinatura do titular e cumprir testes exigidos pelo Play Console. As futuras edições políticas 13 e 22 ficam em projetos separados; **não são parte desta versão**.
