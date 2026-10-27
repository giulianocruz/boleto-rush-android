# Boleto Rush! — Próxima Era Games

**Versão 1.4 para testes (27/10/2026).** Arcade casual Android original, controle por toque ou arraste, 45 segundos no Clássico ou 90 segundos na Maratona.

## O que há nesta versão
- Mascote carteira caramelo, ícones, moedas, boletos e escudos em sprites (arte enviada).
- Capa e cena nova de ação, com paleta turquesa, dourado e coral.
- Tutorial, pontuação, combos, recordes independentes, escudos, vidas extras e pausa.
- Quando as vidas chegam a zero, surge **uma oferta opcional de anúncio recompensado**: se o usuário assistir e ganhar recompensa, revive uma única vez com 1 vida e invulnerabilidade breve.
- Sem anúncios intersticiais obrigatórios e sem compras.
- **AdMob de TESTE:** IDs oficiais de demonstração, não geram receita. A conexão é opcional; o jogo funciona offline, mas não oferece anúncio sem conexão.
- UMP para consentimento e opção de privacidade quando exigida.

## Como compilar
Android Studio, JDK 21, Gradle 9.3.1, AGP 9.1.1, Compose e Kotlin. applicationId `br.com.proximaera.boletorush`; minSdk 26; targetSdk 36.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:bundleRelease
```

O APK de teste está em `app/build/outputs/apk/debug/app-debug.apk`. Esse APK usa a chave de **debug** e não é um pacote assinado para Google Play. O AAB comercial depende da assinatura do titular.

## Documentos
- `ADMOB_E_PUBLICACAO.md` — substituição de IDs e checklist de publicação.
- `PRIVACIDADE.md` — dados locais e SDK de anúncios.
- `TESTES_V14.md` — validação da versão.
- `arte_premium/NOVAS_ARTES_20261027.md` — inventário do novo lote.
- `PLAY_STORE_LISTING.md` — textos de divulgação e pendências de Play Console.

**Status:** distribuição beta pública por GitHub, ainda sem lançamento oficial na Google Play.
