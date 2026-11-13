# Boleto Rush 2.3 — Homologação Android com Windows e ADB

Criado o roteiro `scripts/qa_android_windows.ps1` (PowerShell 5.1+), um teste seguro para usar no computador Insano quando um Android autorizado estiver conectado.

- **Pré-verificação:** confere ADB, aapt, pacote exato `br.com.proximaera.boletorush.qa`, versão e SHA-256, e lista dispositivos. Sem parâmetros de execução não instala nem abre aplicativos.
- **Execução voluntária:** `-Install -Run -Capture` instala ou atualiza apenas o APK de QA mantendo dados do aplicativo, abre MainActivity, aguarda e confirma o processo vivo.
- **Evidências locais:** screenshot PNG por ADB stream binário sem criar arquivos no celular, logs do processo e resumo JSON com verificações manuais pendentes. Não modifica resolução, limpa dados, reinicia, publica ou envia dados.
- **Segurança:** recusa pacote que não seja `.qa`, rejeita múltiplos dispositivos não selecionados, não executa sem depuração USB autorizada. Credenciais Play/AdMob nunca são solicitadas.
- **Limitação:** abertura e processo vivo **não substituem** prova de estabilidade em 45/90s, UX, UMP e vídeos recompensados no aparelho.
- **Compatibilidade:** o antigo `scripts/smoke_android.sh` continua presente para workflows legados e não deve ser confundido com o novo roteiro Windows. Não usa `wm size` nem o pacote comercial.

## Resultado do pré-teste 12/11/2026

- Parser nativo PowerShell: nenhum erro de sintaxe.
- Leitura do APK 2.3.0-QA: `br.com.proximaera.boletorush.qa`, versionCode 38, SHA-256 `F55CEB45BAE4DFA562F87DD9B7960CAC391BF73C9001888257FE601C20348CCB`.
- Pré-verificação real no Insano: `READY_DEVICES=0` e confirmação de que nada foi instalado/aberto/carregado.
- Memória livre no Insano de aproximadamente 0,5 GB: emulador não iniciado.
- Launcher na Área de Trabalho: `01 - Arquivos de Jogos/Boleto Rush/TESTAR-BOLETO-RUSH-ANDROID.cmd`.

## Verificação final do kit

- O launcher `TESTAR-BOLETO-RUSH-ANDROID.cmd --preflight` foi executado na Área de Trabalho do Insano: `PREFLIGHT_EXIT=0`, pacote QA e hash reconhecidos, `READY_DEVICES=0`, sem instalar, abrir ou enviar dados.
- Compatibilidade PowerShell 5.1: parser sem erros, arquivo UTF-8 com BOM para conservar acentos na interface de terminal.
- Android `apksigner verify --verbose` no APK 2.3.0 QA: sucesso, assinatura APK Signature Scheme v2 válida, um signatário. **É assinatura de DEBUG, não de upload Play**.
- O clique duplo no atalho (sem `--preflight`) somente tentará instalar no celular se houver exatamente um dispositivo autorizado. Sem aparelho, falha de modo seguro e mostra a orientação de conexão.
- Captura de screenshot e leitura de logs via ADB, depois que um celular for conectado, permanecem pendentes de teste efetivo.
