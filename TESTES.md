# Boleto Rush! — QA v1.2 (26/10/2026)

## Compilação
- **PASSOU** — `assembleDebug` e `bundleRelease`, Gradle 9.3.1 e Android SDK 36.
- Version code 3; version name 1.2; package `br.com.proximaera.boletorush`.
- APK de debug gerado e atualizado no emulador. O AAB local ainda precisa de assinatura de upload.

## Testes funcionais no emulador Android API 37
- **PASSOU** — menu de abertura e botão de jogar.
- **PASSOU** — tutorial 'Como jogar' e retorno ao menu.
- **PASSOU** — iniciar partida, exibição do HUD.
- **PASSOU** — pausar, retomar e pausar com botão Voltar.
- **PASSOU** — retornar ao menu durante a pausa.
- **PASSOU** — partida Clássica chega à tela final de resultado e mostra moedas/boletos/combo.
- **PASSOU** — app mantém recorde anterior no Clássico (97 pontos no emulador).
- **PASSOU** — seletor Clássico → Maratona 90 segundos.
- **PASSOU** — Maratona inicia com aproximadamente 90 segundos no HUD.
- **PASSOU** — selecionado Maratona, a preferência permanece ao voltar ao menu.
- **PASSOU** — seletor Maratona → Clássico restaura o modo e recorde.
- **PASSOU** — nenhuma exceção fatal foi observada na janela de logcat consultada.

## Observação de diagnóstico
A primeira tentativa de QA falhou devido a uma caixa de diálogo 'System UI isn't responding' do emulador (equipamento com pouco RAM disponível). Após reinicialização, fechamento do diálogo e espera pelo carregamento, os fluxos do app funcionaram.
Na primeira rodada estável, o script registrou 12 passes e dois falsos negativos no seletor por codificação de caracteres acentuados. Um teste separado usando o próprio texto retornado pela UI confirmou a troca de modos, 90 segundos e retorno ao Clássico. Não se observou defeito nesses recursos.

## Pendente antes de publicar na Google Play
- Testar em telefone Android real e diferentes tamanhos, sons, powerups e acessibilidade.
- Verificar funcionamento completo de uma partida de 90 segundos, não apenas a seleção e o HUD.
- Criar e proteger chave de upload e assinar corretamente AAB.
- Completar cadastro, classificação, privacidade e testes obrigatórios do Play Console.
