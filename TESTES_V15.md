# Boleto Rush! 1.5.0 RC1 — QA automatizado e limites

Data: 28/10/2026. Build Android Gradle Plugin 9.1.1, Kotlin 2.2.10, Gradle 9.3.1; targetSdk 36, minSdk 26, versionCode 8.

## Compilação: APROVADA
Workflow https://github.com/giulianocruz/boleto-rush-android/actions/runs/40696938406
- `:app:assembleDebug` e `:app:bundleRelease` concluídos com êxito.
- Artefatos: APK debug (assinado com chave de teste), AAB release **não assinado** (somente QA) e kit da loja.
- Arquivos ZIP de APK/AAB conferidos por integridade. **Não significa que o AAB possa ser enviado à Play sem assinatura.**

## Smoke test Android: APROVADO
Workflow https://github.com/giulianocruz/boleto-rush-android/actions/runs/40698310814
- Emulador Android API 35, imagem Google APIs x86_64, perfil Pixel 5.
- Instalou `br.com.proximaera.boletorush` com adb.
- `am start -W` abriu `MainActivity` e o processo permaneceu vivo.
- Fechou o diálogo de sistema de primeiro uso do modo imersivo ("Viewing full screen" / "Got it") antes de validar.
- Localizou e acionou o botão **JOGAR AGORA**; confirmou processo vivo depois da entrada no gameplay.
- Capturou screenshots **reais** de menu e gameplay em 1080×1920; logs do AndroidRuntime não apresentaram `FATAL EXCEPTION` na amostra.
- Arquivos disponíveis no artefato `BoletoRush-1.5.0-rc1-SCREENSHOTS-REAIS` da execução. A cópia final para Play deve ser PNG/JPG 24 bits sem alfa.

## Não validado
- Partida completa de 45 s e 90 s em aparelho físico.
- Anúncio recompensado real, consentimento em diferentes regiões e retomada após vídeo.
- Ciclo completo de instalação Google Play por AAB assinado; produção, desempenho de aparelhos modestos, revisões de política.
- Conta de Play Console e AdMob com IDs próprios ainda não foram configuradas.

## Lançamento
Não liberar produção antes de assinar o AAB com chave de upload guardada pelo titular, configurar AdMob real, testar anúncio opcional e concluir cadastro/testes exigidos na Play Console. O APK desta RC é uma distribuição **para testes**.
