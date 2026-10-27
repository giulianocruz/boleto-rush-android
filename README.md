# Boleto Rush! 🪙 — Próxima Era Games

Arcade casual Android, original e offline. Pegue moedas, desvie dos boletos e sobreviva. Sem anúncios, login ou compras.

## v1.3 — Integração das artes enviadas
Integramos as três artes do pacote `out. 26 - 20_30.zip`: nova capa ilustrada no menu, ícone de carteira e **16 quadros de sprite** (personagem, boleto, moeda e escudo) separados e otimizados. Identidade visual oficial da v1.3: mascote **carteira marrom** em cidade brasileira noturna, moedas douradas e boletos corais. Os fundos de gameplay e botões ainda são nativos e leves.

## O que já funciona
- Clássico de 45s e Maratona de 90s com recordes independentes.
- Moedas, boletos, escudos, vida extra, combos e feedback.
- Tutorial, pausa/retomada, som opcional e compartilhamento.
- UI adaptada à orientação vertical, sem dependência de rede.

## Compilação
Projeto Kotlin + Jetpack Compose; AGP 9.1.1; Gradle 9.3.1; JDK 21. minSdk 26, targetSdk 36.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:bundleRelease
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`. AAB local: `app/build/outputs/bundle/release/app-release.aab`. **O AAB para Play precisa ser assinado com a chave de upload própria antes do envio.**

## Arte e publicação
- [Relatório de integração das artes](arte_premium/ARTES_IMPLEMENTADAS_V13.md)
- [Bíblia visual e prompts](arte_premium/README.md) — materiais preliminares; as referências efetivas da v1.3 são as artes recebidas.
- [Textos e checklist da loja](PLAY_STORE_LISTING.md)
- [Privacidade](PRIVACIDADE.md)
- [Registro de testes](TESTES.md)

Desenvolvido pela Próxima Era Games.
