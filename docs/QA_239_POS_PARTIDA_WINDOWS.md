# Boleto Rush 2.3.9 — coleta de evidências após jogar (Windows)

## Melhoria aplicada ao kit de homologação

Antes, `scripts/qa_android_windows.ps1` obtinha uma captura de tela e o logcat somente **na inicialização**. Isso não permitia documentar o resultado de partidas reais, o Cofre, o álbum e as opções de privacidade depois que o jogador interagisse com o aplicativo.

Foi acrescentado o modo **`-CollectOnly`**. Ele recolhe as evidências **sem reinstalar, abrir, fechar, reiniciar, limpar dados ou modificar a resolução** do Android.

### Utilização no Insano

1. Conectar um aparelho Android autorizado por USB e desbloqueá-lo.
2. Executar o atalho `TESTAR-BOLETO-RUSH-ANDROID.cmd` uma vez para instalar/abrir a QA 2.3.9.
3. Jogar manualmente uma partida Clássico (45s) e uma Maratona (90s); testar pausa, retorno de outro aplicativo, Cofre, relíquias e vídeo de demonstração. Capturar **cada tela desejada** voltando ao jogo e clicando em `COLETAR-EVIDENCIAS-BOLETO-RUSH.cmd`.
4. A coleta só prossegue quando o pacote QA `br.com.proximaera.boletorush.qa` está rodando e em primeiro plano. Deixar o app aberto antes de clicar.
5. Encontrar as evidências na pasta `QA-Evidencias\<data>-pos-partida`, ao lado do APK.

**Arquivos gerados por cada coleta:** `01-tela-atual.png`, `02-logcat-app.txt` (somente PID do jogo), `03-resultado.json` com SHA-256 da QA, versão e horário.

**Importante:** os prints e logs permanecem no computador Insano; podem mostrar progresso, dados de diagnóstico e informações do aparelho. Revisar antes de compartilhá-los. Nenhuma evidência é enviada automaticamente a serviços externos.

### Modo terminal

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\qa_android_windows.ps1 `
  -ApkPath "C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.9-RELOGIO-SEGURO-QA.apk" `
  -CollectOnly
```

O script recusa pacotes que não sejam **.qa**, mais de um telefone sem seleção explícita e aparelhos não autorizados. Também recusa capturar a tela caso o jogo não esteja em primeiro plano, evitando imagens inadvertidas de outros aplicativos.

## Validação feita em 12/11/2026

- Correção do script aplicada a partir do histórico íntegro do Git; parser **PowerShell 5.1 retornou 0 erros**.
- Pré-verificação real no Insano: `READY_DEVICES=0`, reconheceu `br.com.proximaera.boletorush.qa`, `versionCode=46`, `versionName=2.3.9-qa.debug` e SHA-256 `DB6EEB2352DCA3ABBB7601DBE8511D33C7B76AAD839A8B09930F81FF9FB28E07`. Saída 0 e nenhuma alteração ao telefone.
- Chamada de `-CollectOnly` sem Android conectado: **recusada como esperado** com retorno de erro e sem gerar diretório de evidências.
- Ainda **não** existe prova de uso do modo pós-partida em Android real; essa validação depende de um aparelho fisicamente conectado e autorizado.

## Critérios para homologação real

- Capturas reais de menu, tutorial, partida, resultado, Cofre/Loja, álbum e opções de privacidade.
- Partida completa 45s e 90s sem travar; confirmar que voltar do segundo plano e do AdMob não consome tempo indevido.
- Vídeo recompensado completo concede bônus uma vez; cancelado ou sem consentimento não concede.
- Testar salvamento ao fechar/reabrir, missão semanal e desafio mensal.
- Conferir escala de fonte, legibilidade e controles em celular real.

O APK debug permanece o mesmo: **2.3.9-QA**, sem nova compilação e sem envio à Play Store. O trabalho prepara a homologação, não a substitui.
