# Boleto Rush 2.3.4-QA — escudo de relíquia acumulativo

## Problema identificado

A loja oferecia `3s DE SALDO EXTRA NA PRÓXIMA PARTIDA` por 5 fragmentos, mas o início da partida usava `maxOf(escudoPermanente, bônusDaRelíquia)`. Nos níveis 2 e 3 da melhoria permanente de escudo, que oferecem respectivamente 4s e 6s, a relíquia era consumida sem adicionar **nenhum segundo** de proteção. Mesmo no nível 1 (2s), a compra acrescentava só 1s.

## Correção

- Agora a proteção inicial é **escudo permanente + 3 segundos comprados**, limitado a 9s. Portanto:
  - Nível 0 (0s): 3s no total
  - Nível 1 (2s): 5s no total
  - Nível 2 (4s): 7s no total
  - Nível 3 (6s): 9s no total
- `GameRelics.totalStartShieldSeconds` concentra a regra e rejeita NaN/infinito; mantém limites com preferências corrompidas.
- A compra continua valendo apenas para a **próxima** partida; o consumo é único e requer commit bem-sucedido em SharedPreferences.
- Aviso de início mostra o benefício ativado, incluindo uma mensagem combinada caso escudo e ímã de relíquias tenham sido preparados simultaneamente.
- O bônus de ímã continua exatamente 6s, como prometido pela loja.
- Não houve alteração de saldos existentes, recuos de dados, anúncios, recibos ou regras de monetização.

## Testes adicionados

- Três testes de regressão novos: acumulação real nos 4 níveis de melhoria permanente, sanidade de entradas inválidas e persistência/consumo único mesmo após falha de commit.
- A suíte completa também cobre o fechamento transacional, a coleção e missão semanal com Cofre cheio, AdMob, o desafio mensal e compatibilidade de recibos.
- A validação via Gradle offline e geração de APK devem ser registradas separadamente após conclusão.

## Tentativa controlada de emulador

- Havia aproximadamente 3,1 GB livres no Insano, e foi iniciada uma sessão **sem janela** do AVD Android 37 com memória alvo reduzida a 1 GB.
- O dispositivo apareceu como `emulator-5554` no ADB, mas **não completou a inicialização** enquanto a memória livre do computador caiu para aproximadamente 0,97 GB.
- A sessão foi encerrada com `adb -s emulator-5554 emu kill`, sem reiniciar o PC, tocar em apps de produção, modificar DNS/AdMob, instalar o Boleto Rush ou limpar dados.
- Essa tentativa não comprova desempenho, estabilidade, acessibilidade ou comportamento do AdMob em um Android real. O teste físico com o kit Windows permanece necessário.

## Publicação

- Versão de QA independente da Play: `2.3.4-qa.debug`, versionCode 41, pacote `br.com.proximaera.boletorush.qa`.
- Anúncios permanecem em modo de demonstração e o AAB comercial continua bloqueado por `verifyPlayConfiguration`.
- Não houve envio à Google Play.

## Resultado de validação — Insano, 12/11/2026

- `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`: **BUILD SUCCESSFUL**.
- **105 testes unitários**, zero falhas, zero erros, em 16 classes.
- APK QA independente: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.4-ESCUDO-ACUMULATIVO-QA.apk`.
- 17.440.803 bytes; SHA-256 `B3CE53B6443D39C4C53F4C300EBBA9DFFBFF2126BB48A40A33455ED568D39E8F`.
- Metadados com `aapt`: pacote `br.com.proximaera.boletorush.qa`, versionCode 41, versionName `2.3.4-qa.debug`, minSdk 26, targetSdk 36.
- `apksigner verify`: esquema de assinatura v2 válido e um signatário de **depuração**.
- Atalho `TESTAR-BOLETO-RUSH-ANDROID.cmd` da Área de Trabalho atualizado para a 2.3.4. Backup do atalho anterior: `TESTAR-BOLETO-RUSH-ANDROID-ANTERIOR-233.cmd`.
- Pré-verificação do atalho concluída com sucesso: `PREFLIGHT_EXIT=0`, APK e SHA-256 reconhecidos, `READY_DEVICES=0`, sem instalação ou alterações no Android.
- **Não homologado em aparelho físico**: ainda faltam partida completa, escudo efetivo sob colisão e teste real de AdMob/UMP.
