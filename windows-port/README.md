# Boleto Rush! — Windows nativo (QA v1.4)

Este protótipo Windows usa Windows Forms e incorpora diretamente 18 artes originais do aplicativo Android. Não depende de emulador, APK, internet ou AdMob. O arquivo `BoletoRush-WINDOWS-JOGAVEL-v1.4.exe` é compilado como aplicativo único.

## Compilar

Windows com .NET Framework 4.x e PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\windows-port\make-icon.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\windows-port\build.ps1
```

O executável vai para a área de trabalho do usuário atual. O script não instala nada. Requer o código e os assets de `app/src/main/res/drawable-nodpi`.

## Controles

- Mouse (arrastar ou clicar na posição): movimenta a carteira
- A/D ou setas: movimentação contínua, independente do FPS
- P/Escape: pausa/retoma
- Enter/Espaço: inicia partida / recomeça
- M: liga/desliga os efeitos sonoros

## Recursos

Modos Clássico (45s) e Maratona (90s); moeda, boleto, vida extra, escudo, combo, dificuldade progressiva, pausa, segunda chance, final de partida, recorde e contagem de partidas. Configurações e recordes ficam em `%LOCALAPPDATA%\ProximaEra\BoletoRushWindows\scores.txt`.

**Diferença importante:** a segunda chance na versão Windows é gratuita e não possui anúncios. O Android possui integração de anúncio recompensado opcional e não deve ter a lógica do Windows aplicada diretamente.

## QA em 29/10/2026

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\windows-port\smoke.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\windows-port\qa_visual.ps1
```

- Compilação C# concluída.
- Smoke interno: entrada em partida, atualização de gameplay, recuperação, conclusão de 45s e conclusão de 90s; ExitCode 0. Rodadas simuladas com vidas ampliadas para testar o temporizador sem morrer; isso não equivale a teste manual de dificuldade.
- Captura automatizada: menu e partida capturados após clicar no botão **JOGAR AGORA**, com processo ativo. Quando duas execuções de smoke/captura foram feitas simultaneamente, uma captura falhou; nova execução isolada passou.
- Os testes internos não alteram recordes a partir desta versão. Os testes mais antigos podem ter deixado valores de laboratório no arquivo local.

## Pendências antes de publicar

- Testar manualmente gameplay completo e teclado em computador físico.
- Avaliar balanceamento e áudio dedicado; os efeitos atuais usam sons do sistema.
- Verificar segurança/assinatura e distribuição. Esta versão não é instalador, não tem atualização automática e não foi assinada para distribuição pública.
- Validar o APK Android separadamente. O sucesso do executável Windows não valida o APK.
