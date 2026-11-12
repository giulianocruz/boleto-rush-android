# Boleto Rush 2.3.5-QA — coleta de proteção sem perda de tempo

## Erro corrigido

Quando a proteção comprada com relíquias e a melhoria permanente somavam até **9 segundos** de escudo, a coleta de um novo item `SHIELD` sobrescrevia o estado com **7 segundos**, reduzindo um benefício que o jogador já havia adquirido.

## Regra nova

- Um item de proteção comum concede **pelo menos 7 segundos de proteção restante**, nunca menos que o tempo já ativo.
- `GamePowerRules.refreshShieldSeconds(remaining, pickupSeconds)` aplica o maior valor, com normalização para o intervalo de 0 a 9 segundos. Valores negativos, NaN e infinito não vazam para o cronômetro.
- A tela agora informa `SALDO EXTRA MANTIDO! PROTEÇÃO NÃO DIMINUI.` quando a coleta não amplia o efeito, sem enganar o jogador dizendo que foram dados mais segundos.
- **A proteção comprada com relíquias segue acumulativa (3 segundos adicionais)** e a loja permanece com a mesma economia.
- Não modifica a duração de `ARMOR`, `SLOW`, `PIX`, `SPRINT`, ímã, saldos, recibos, banco de dados, IDs AdMob ou gestão de consentimento.

## Testes

- `GamePowerRulesTest` ganhou cenários para 9s → 9s, 8,5s → 8,5s, 6,5s → 7s, estado vazio e entradas inválidas.
- A suíte anterior já validava consumo único de relíquia, escudo permanente, recompensa do vídeo, desafio mensal e missão semanal.
- Gradle usado: `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`.
- Após compilação, validar assinatura debug, versão, hash SHA-256 e a pré-verificação do atalho do Insano.

## Homologação manual obrigatória

1. Equipar o escudo permanente no nível 3 e preparar o bônus de relíquia, iniciar partida e confirmar os 9s de proteção.
2. Pegar um item Saldo Extra nos primeiros 2s: o cronômetro NÃO deve retroceder para 7s.
3. Sem proteção inicial, pegar Saldo Extra e observar o cronômetro em 7s.
4. Repetir sob pausa, com ímã, Raspão e durante o Fechamento do Mês; confirmar que o escudo não impede contabilização de contato com chefe mensal.
5. Validar 45 e 90 segundos completos em celular físico, UMP/AdMob somente com IDs de teste, interface e desempenho.

## Estado

O APK é de **QA paralelo** `br.com.proximaera.boletorush.qa`, `versionCode=42`, `versionName=2.3.5-qa.debug`, API alvo 36. Ainda não homologado em Android físico e **não apto à Play Store** sem os testes manuais, assinatura e IDs de monetização comerciais. Nenhum anúncio real foi ativado.

## Resultado obtido no Insano — 12/11/2026

- `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`: **BUILD SUCCESSFUL**.
- **107 testes unitários, 0 falhas e 0 erros**, incluindo dois testes novos de temporizador do escudo.
- APK QA independente: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.5-ESCUDO-RENOVADO-QA.apk`.
- 17.440.803 bytes; SHA-256 `60969A8294D8D19DF28C077F0D22C631CAABC936ECCD95684199E3E3BC6373CA`.
- Metadados via `aapt`: `br.com.proximaera.boletorush.qa`, versionCode 42, versionName `2.3.5-qa.debug`, minSdk 26, targetSdk 36.
- `apksigner verify`: assinatura debug de APK v2 válida, 1 signatário. Não é assinatura comercial de upload da Play.
- Atalho `TESTAR-BOLETO-RUSH-ANDROID.cmd` atualizado para instalar apenas o APK QA 2.3.5; cópia anterior em `TESTAR-BOLETO-RUSH-ANDROID-ANTERIOR-234.cmd`.
- Pré-verificação real do atalho: `PREFLIGHT_EXIT=0`, pacote e SHA reconhecidos; **READY_DEVICES=0**, nenhum Android instalado, iniciado ou alterado.
- A compilação exibiu somente um aviso de uso de `TRIM_MEMORY_RUNNING_LOW` deprecated, presente antes desta correção.

**Pendência:** é preciso jogar no celular real e validar a renovação do escudo com 9s ativos. O AdMob ainda está com IDs de teste, e nada foi enviado à Play Console.
