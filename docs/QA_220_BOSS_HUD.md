# Boleto Rush 2.2.0-QA — Polimento do Fechamento do Mês

## Melhorias implementadas
- **Aviso de preparação antecipado** a partir de cinco segundos antes da primeira onda do Fechamento, para não surpreender o jogador.
- **Aviso de pistas acessível**: três indicadores com texto, ✓ para faixa segura e ✕ para faixas perigosas, em contraste de verde e vermelho; a distinção não depende apenas de cor.
- **Sem interrupção comercial no clímax**: os anúncios voluntários de duplicar recompensa de capítulo ficam suprimidos desde a preparação até o final do confronto, inclusive na transição da etapa 3 para 4 em partidas de 45/90 segundos. Recompensas de missão regular continuam disponíveis normalmente e os demais anúncios recompensados opcionais permanecem intactos fora desse trecho.
- **Avisos de capítulo não competem com avisos do chefe**, reduzindo sobreposição visual.
- Não foram adicionados novos anúncios ou restrições de conteúdo; não há resgate em dinheiro real.

## Testes de regressão
- `GameMonthChallengeTest`: limites do aviso de cinco segundos em partidas 45s e 90s; validação de tempo inválido e fronteiras do bloqueio de propagandas.
- `GameChapterBonusOfferTest`: não interromper capítulos quando o chefe está próximo ou ativo.
- Testes anteriores continuam cobrindo ondas, selo único por mês, consistência transacional, álbum e compatibilidade de recibos antigos.

## Verificação manual recomendada
1. Jogar Clássico e confirmar o aviso de contagem regressiva dos 22 aos 27 segundos, quando inicia a primeira onda; Maratona dos 67 aos 72 segundos.
2. Confirmar que as três faixas exibem texto legível, mesmo em telefone pequeno e com acessibilidade/escala de fonte aumentada.
3. Confirmar que os caminhos seguros correspondem às moedas; testar a travessia usando dedo e toques.
4. Concluir cada modo com e sem acertos e confirmar integridade de selos e notas virtuais.
5. Com AdMob de teste habilitado, garantir que o jogo não pede anúncio no meio do desafio; vídeos continuam opcionais fora da janela protegida.
6. Testar pausa/retomada, turbo, câmera lenta, ímã e armadura durante as ondas.
7. Repetir partida e verificar que o selo só é pago uma vez por mês.

## Publicação
- A versão 2.2 é uma **QA de Android**, não um bundle assinado da Google Play.
- AdMob continua com identificadores de teste e o lançamento exige QA físico, IDs próprios, consentimento e assinatura de upload.

## Compilação validada no Insano — 12/11/2026

- `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain` → **BUILD SUCCESSFUL**.
- Relatórios JUnit: **93 testes / 0 falhas / 0 erros** em 15 classes.
- APK QA independente em `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.2.0-POLIMENTO-QA.apk`.
- Arquivo 17.440.803 bytes, SHA-256: `3C86231D5128EDF600536515C1B5EC0EA194194FE91DC19165017C684138B20F`.
- Pacote confirmado com aapt: `br.com.proximaera.boletorush.qa`, `versionCode=37`, `versionName=2.2.0-qa.debug`, `minSdk=26`, `targetSdk=36`.
- ADB: nenhum dispositivo físico conectado. O visual do indicador de pista e o ritmo das ondas **não foram homologados no aparelho**.
