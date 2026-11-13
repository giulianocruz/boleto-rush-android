# Boleto Rush! — QA 1.8.5: Raspão / Desvio Perfeito

## Implementação

- Mecânica de **Raspão**: quando um boleto ou cheque especial cruza a linha da carteira a uma distância segura, mas próxima, após movimento intencional recente do jogador, concede pontos extras.
- Regras encapsuladas em `GameNearMiss.kt`, com testes de colisão, valores de limite, proximidade, proteção e valores inválidos.
- Bônus pontua apenas o placar temporário: 7 pontos no primeiro raspão, 9 no segundo, até 15 pontos na sequência (não concede moedas, notas, XP nem recompensas de anúncio).
- A sequência é quebrada ao tocar um perigo ou deixar um perigo passar sem raspão. Nenhum crédito automático com o jogador parado ou com escudo, armadura e invulnerabilidade.
- Feedback na partida, contagem no HUD e registro no resultado.
- Mantidos os modos 45s e 90s, monetização opcional e pacote QA separado `br.com.proximaera.boletorush.qa`.
- Base: branch `fix/boleto-rush-184-collision-frame-budget` (PR #26), sem merge na produção; versão `1.8.5-qa`, versionCode `32`.

## Verificações recomendadas no Android físico

1. Inicie Clássico e Maratona. Arraste a carteira no momento em que o boleto vai passar muito próximo, mas sem bater.
2. Confirme `RASPÃO!` e o acréscimo moderado de pontos; repita para testar sequência, e provoque uma colisão para confirmar o reset.
3. Deixe boletos passarem sem mover: **não** deve gerar pontos extras.
4. Colete escudo/armadura, tente raspão: não deve gerar pontos extras enquanto protegido.
5. Pause/retome, conclua uma rodada e confirme o contador em resultado. Teste revive/segunda chance opcional.
6. Confira FPS em celular intermediário e ênfase na área clicável em 390px; o benefício não depende de anúncio ou rede.

## Critérios antes da publicação na Play

- APK de QA não equivale a AAB de produção; pacote de QA é instalado em paralelo.
- Não enviar à Play sem regressão em Android físico, assinatura de upload segura, AdMob próprio, política de privacidade e ficha/declarações corretas.
- Preservar os dados do aplicativo já instalado e **não** atualizar o pacote de release antes da aprovação.

## Evidência de compilação — 12/11/2026

- Insano, worktree separado, base PR #26: Gradle offline `:app:testDebugUnitTest :app:assembleDebug` — **BUILD SUCCESSFUL**.
- Suite JUnit XML: **69 testes, zero falhas, zero erros**, distribuídos em 14 arquivos de relatório.
- APK QA para teste físico: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-1.8.5-RASPAO-QA.apk`.
- Tamanho: **17.408.035 bytes**.
- SHA-256: `E28992BBD004DDA68C18E47735831FD802ECB2F169A6E804DBAA035B54DDE281`.
- Sem celular ADB conectado nesta rodada. Não considerar homologado para Play Store até testes de partidas completas em Android físico.
