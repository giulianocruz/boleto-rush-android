# Boleto Rush 1.7.4 QA — resgate de ganhos e status de poderes simultâneos

## Motivação

No fluxo anterior o jogador podia perder todos os ganhos de uma partida ao abrir o menu ou recomeçar. O painel escondia dois dos poderes quando PIX Turbo, ímã e saldo extra funcionavam simultaneamente.

## Implementado

- Na pausa, após **50% do tempo** decorrido, existe **GUARDAR GANHOS E ENCERRAR**.
- Resgate voluntário guarda as notas e moedas coletadas da rodada e liquida a economia por meio de `finish(false, withdrawal=true)`. Conta a partida, porém **não registra vitória**; conquistas ordinárias já cumpridas continuam elegíveis.
- Antes de metade do tempo o painel mostra o cronômetro até a habilitação.
- `RECOMEÇAR • DESCARTAR` e `MENU • DESCARTAR` continuam descartando as coletas em curso, agora com descrição explícita.
- Tela de resultado mostra **SALDO RESGATADO!** e texto de encerramento voluntário; as receitas normais continuam disponíveis no extrato.
- HUD exibe **moedas** e **notas** coletadas nesta rodada, além de todos os efeitos ativos, mesmo simultâneos: PIX 2x, ímã e saldo extra com contagem restante.
- Novo utilitário puro `GameRoundExit.kt` e testes `GameRoundExitTest.kt`; teste de economia adicional garante que o resgate não concede conquista de sobreviver até o fim.
- Variante 1.7.4-qa, versionCode 22, package `br.com.proximaera.boletorush.qa` para instalar em paralelo à versão pública.

## Plano de validação

- [x] Gradle `:app:testDebugUnitTest :app:assembleDebug` aprovado; 30 testes, 0 falhas, 0 erros
- [x] APK BoletoRush-1.7.4-QA.apk gerado no Insano, 17.500.564 bytes. SHA256 ABBB85098D53D1B548752AF3A588CBDA416258686DB8C0EE8A0F133A5023F5FF
- [ ] Dispositivo físico conectado via ADB; em 30/10/2026, nenhum dispositivo detectado. Instalar `-r` apenas variante QA quando disponível.
- [ ] Em partida clássica: resgate bloqueado antes de 22,5s e disponível depois; notas e moedas preservadas, sem vitória
- [ ] Em maratona: resgate bloqueado antes de 45s e disponível depois
- [ ] Sair/voltar ao menu sem resgatar não credita itens
- [ ] PIX + ímã + saldo ativo: os três rótulos visíveis; verificar colisões e FPS
- [ ] Testar conquistas, cartão, dívida e AdMob recompensado
- [ ] Partidas completas nos dois modos, QA de UI e revisão de arte final

## Limites

Este comportamento guarda os itens apenas quando o jogador **escolhe o resgate a partir da pausa**; não implementa salvamento automático ao encerrar o processo Android. Nenhum valor é dinheiro real. Não publicar na Play Store antes de testar a variante no aparelho.
