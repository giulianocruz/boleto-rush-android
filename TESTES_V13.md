# Testes e entrega — Boleto Rush! v1.3 Premium
Data: 26/10/2026.

**COMPILAÇÃO APROVADA:** Gradle `assembleDebug` e `bundleRelease` concluíram com BUILD SUCCESSFUL. `aapt`: `br.com.proximaera.boletorush`, versionCode=4, versionName=1.3, targetSdk 36, minSdk 26. APK debug com aproximadamente 11 MB.

**ARTES:** 3 JPG originais extraídos do ZIP enviado, com a imagem de capa e ícone embutidos no projeto. 16 sprites gerados a partir da folha 4×4, importados como PNGs em drawable-nodpi. Menu e Canvas foram atualizados.

**TESTES NO EMULADOR:** Android API 37 inicializou e APK 1.3 instalou (ADB 'Success'). Primeira tentativa de abrir a UI foi interrompida por caixas de diálogo 'Pixel Launcher isn't responding' do próprio emulador. Após fechar e reiniciar launcher, não foi possível concluir o teste visual automatizado. O processo Android do jogo foi identificado na primeira tentativa. Na janela de logcat consultada não apareceu 'FATAL EXCEPTION' do app, mas isso **não comprova ausência de falhas**.

**NÃO VERIFICADO:** menu premium e sprites em execução estável, 45s completos, 90s completos, tela final, comportamento no celular físico e performance em aparelhos modestos. O ZIP original tinha JPEGs com fundo escuro, cujo recorte aproximado para PNG pode apresentar bordas residuais — requer revisão humana.

**PUBLICAÇÃO:** O APK debug pode ser distribuído como build de TESTES pelo GitHub e instalado manualmente no Android. O bundle AAB local ainda carece de assinatura de upload segura e aprovação nas exigências da Play Console. Não afirmar que o app está publicado oficialmente na Google Play.

**RECOMENDAÇÃO:** instalar APK em telefone real, avaliar menu e os objetos, capturar prints reais, corrigir qualquer artefato de transparência e só então gerar AAB de produção assinado.
