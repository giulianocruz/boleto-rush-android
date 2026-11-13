# Boleto Rush 1.7.8 QA — moedas visíveis, progressão lenta e cenários variados

## Feedback real que motivou esta rodada

O jogador relatou períodos sem moedas visíveis, com impressão de ganhar pontos sem perceber a coleta, e pediu mais variedade financeira e aumento de dificuldade bem lento. O jogo já tinha 10 tipos de itens, mas parte deles não era evidente na build antiga instalada no aparelho.

## Mudanças

- Moedas ganham **contorno dourado contínuo**, brilho suave e fallback em forma de moeda se um sprite estiver ausente ou o frame lateral da rotação ficar pouco visível. Não há mais quadrado genérico como fallback de moeda.
- `GamePickup.coinContact()`: a pontuação exige moeda já visível na área da partida, cruzamento com a carteira ou chegada efetiva trazida pelo ímã. Nunca há pontos apenas pelo spawn ou pela ativação do ímã.
- Cada moeda coletada gera **flash dourado independente** do texto de feedback, inclusive quando a mensagem é substituída por super combo ou PIX.
- Primeira queda da partida é garantidamente uma moeda. Depois, não surgem mais de **dois itens não-moeda consecutivos**. Isso diminui os períodos de ausência de moedas, mantendo diversidade de itens.
- Progressão de dificuldade mais suave e proporcional à duração da partida: intervalo entre itens reduz no máximo de 0,98s para 0,85s (considerando 20 partidas), risco-base de boleto ~6,5% para no máximo 18,5%, e velocidade de queda com incremento máximo de 0,045 na mesma rodada (em vez de crescer proporcionalmente aos segundos, penalizando muito a maratona).
- **Quatro ambientes distintos** com arquitetura de fundo desaturada: Centro (ponto de ônibus), Banco (colunas e fachada), Comércio (lojas), Cobrança (prédio de faturas). Elementos atrás dos pickups e do personagem, com pouco contraste.
- Preservados os itens financeiros: moeda (pontuação e upgrades escassos), notas R$ e salário (cosméticos), cartão (limite de crédito pendente), PIX Turbo (dobro de pontos), ímã, saldo extra, coração, boleto e cheque especial (-60).
- `versionCode=26`, `versionName=1.7.8-qa`, APK debug separado da versão pública.

## Verificação automatizada

- [x] Gradle `:app:testDebugUnitTest :app:assembleDebug`: BUILD SUCCESSFUL em 01/11/2026, duração 5m07s; o Gradle precisou iniciar um daemon novo por limite de memória.
- [x] JUnit: 8 suítes, **43 testes, 0 erros, 0 falhas** — incluindo coleta visível, velocidade equivalente em 45s/90s, cap de 2 itens sem moedas, separação das economias e teto de dificuldade.
- [x] Teste Gradle de 200 rodadas simuladas (100 clássicas e 100 maratonas) passou sem falhas nas invariantes de geração.
- [x] APK salvo no Insano em `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-1.7.8-QA.apk` (17.375.267 bytes; SHA256 `A25B7B27D4A23DC316C2FB366E07CD901430BB041E19D29BC99C08F9AD719CC0`).
- [ ] Instalar na variante QA do Galaxy A55 via ADB.
- [ ] No celular: observar 45s e 90s até o fim, flash ao coletar, PIX+ímã, cenas, legibilidade das moedas, cheque especial, loja e ganho de notas.

**Limite conhecido:** testes unitários e simulação de geração não substituem QA visual nem prova de que houve zero falso positivo no aparelho. Não publicar na Play Store sem validação em dispositivo. A diferença em relação à versão já instalada depende de qual APK o jogador está usando.
