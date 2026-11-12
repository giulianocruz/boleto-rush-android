# Boleto Rush 2.3.2-QA — validação do chefe mensal

## Correções realizadas

- **Selo mensal seguro:** nas versões 2.1/2.2, a elegibilidade dependia apenas de 3 ondas iniciadas, vitória e nenhum contato registrado. Agora é preciso que os **6 perigos das três ondas** realmente cruzem a linha da carteira ou sejam removidos legitimamente por Limpa-Tela antes de tocar.
- **Sem créditos duplicados:** um perigo que já cruzou a carteira não é contado novamente se um Limpa-Tela for usado depois, e os valores são limitados aos seis obstáculos reais.
- **Poderes preservados:** Câmera Lenta continua reduzindo a velocidade de queda; Escudo e Armadura preservam a proteção normal, mas o contato com um boleto do chefe continua impedindo o selo. Limpa-Tela pode resolver perigos à distância.
- **Rótulo de interface corrigido:** a mensagem do Fechamento do Mês continha um caractere de controle (BEL), que podia causar exibição incorreta. Agora o título textual e testável é `FECHAMENTO | ONDA 1/3` até `3/3`.
- **Resultado explícito:** caso o relógio finalize antes de todos os perigos terem sido resolvidos, o jogador recebe a explicação correta no lugar de um selo indevido.
- **Compatibilidade:** a API de `GameSettlement.settle` usa parâmetro adicional opcional `monthlyBossResolved=0` (padrão seguro). Não houve alteração do formato de recibo V4 ou das recompensas já salvas; progressões antigas permanecem intactas.
- **Versão:** `2.3.2-qa`, `versionCode=39`, pacote paralelo `br.com.proximaera.boletorush.qa`, targetSdk 36.

## Qualidade e critérios

- Gradle offline: `:app:testDebugUnitTest :app:assembleDebug`.
- Testes de nova regra: 0, 5, 6 ou 7 perigos resolvidos; início/fim das ondas; Limpa-Tela antes/depois de cruzar a carteira; coordenadas inválidas; cabeçalho imprimível em Android; persistência idempotente do selo após salvamento da partida.
- Confirmar o resultado final da suíte e o SHA-256 do APK antes de distribuir a QA.
- **Teste manual obrigatório:** partidas completas de 45 e 90 segundos, poderes e colisão com o chefe. Usar o roteiro de Android via USB do pacote de homologação `scripts/qa_android_windows.ps1`.

## Monetização e publicação

- O AdMob permanece com identificadores demonstrativos, consentimento UMP e vídeos opcionais. O trabalho não altera IDs nem concede recompensa de anúncio sem callback real.
- O procedimento de release continua bloqueado até obter assinatura de upload do titular e IDs AdMob comerciais.
- **Não publicar na Google Play** até o aparelho físico confirmar estabilidade, recompensa única, UI legível e opções de consentimento.

## Evidências finais — Insano, 12/11/2026

- `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`: **BUILD SUCCESSFUL**.
- **100/100 testes unitários aprovados**, 0 erros, 0 falhas (16 classes JUnit).
- APK gerado em `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.2-FECHAMENTO-CORRIGIDO-QA.apk`, 17.440.803 bytes.
- SHA-256: `BD669D7090BA4CFA89436D4A9639A6E08D550A00C82A5186F32906BE9B5BDE18`.
- `aapt`: pacote `br.com.proximaera.boletorush.qa`, versionCode 39, versionName `2.3.2-qa.debug`, minSdk 26, targetSdk 36.
- `apksigner verify`: APK assinado e validado sob esquema v2, assinatura **de depuração, não release Play**.
- Atalho do Insano `TESTAR-BOLETO-RUSH-ANDROID.cmd` atualizado para apontar à versão 2.3.2. Cópia do atalho antigo: `TESTAR-BOLETO-RUSH-ANDROID-ANTERIOR-230.cmd`.
- Atalho executado com `--preflight`: saída 0, pacote e SHA-256 reconhecidos, `READY_DEVICES=0`, sem instalação nem alterações no aparelho.
- **Pendente:** teste 45/90 segundos no Android real, anúncios UMP recompensados, desempenho gráfico e aprovação da Play Console.
