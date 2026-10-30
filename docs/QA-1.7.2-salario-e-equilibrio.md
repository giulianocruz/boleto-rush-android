# Boleto Rush 1.7.2 QA — salário, distribuição e caminhos seguros

**Estado:** código preparado numa ramificação de QA. Ainda sem APK novo compilado/instalado; o computador Insano está offline. A edição 1.7.1 instalada anteriormente no celular permanece inalterada.

## Mudanças

- Novo item `PAYDAY` (Salário): +12 notas virtuais para a loja de cosméticos; sem pontos diretos e sem moedas raras.
- Surge depois do primeiro capítulo e é intencionalmente incomum.
- Moedas com tendência parcial a nascer perto da faixa de movimento atual, para reduzir a sensação de que passam muito longe.
- Boletos e cheque especial não surgem diretamente na mesma faixa vertical do jogador, sem eliminar a necessidade de desviar.
- Permanecem: máximo de três pickups não-moeda em sequência, nenhuma geração consecutiva de perigos, dificuldade crescente e lenta e quatro capítulos.
- O novo testador `tools/qa/DropSimulation.kt` roda sem Android SDK ou GitHub Actions.
- `versionCode=20` e `versionName=1.7.2-qa` no código, **sem publicação na Play Store**.

## Simulação preliminar (compilador Kotlin/JVM, 100 seeds por modo)

| Duração | Total de itens | Menor número de moedas em uma rodada | Salários | PIX |
| --- | ---: | ---: | ---: | ---: |
| Clássico 45 s | 5.300 | 26 | 95 | 270 |
| Maratona 90 s | 10.500 | 52 | 223 | 640 |

**PASS:** 200 cronogramas de geração simulados sem violar as invariantes. Esses números são de itens *gerados*, não necessariamente coletados pelo jogador. A cópia de código puro foi compilada em ambiente Kotlin/JVM; o projeto Android atualizado ainda não foi recompilado.

## Pendências antes de liberar

- [ ] Retomar a conexão com o Insano e sincronizar esta ramificação
- [ ] Executar `gradlew testDebugUnitTest assembleDebug` com o Android SDK
- [ ] Instalar a variante `br.com.proximaera.boletorush.qa` **sem desinstalar** a anterior
- [ ] Verificar telas de salário, pontuação, entrada em loja e renda virtual
- [ ] Jogar os dois modos até o fim, avaliar obstáculos e número de itens realmente coletados
- [ ] Preparar e integrar artes definitivas dos novos bônus e fundos
- [ ] Conferir segurança de anúncios, assinatura e consentimento antes de qualquer release pública

## Notas de design

É intencional que os bônus de salário usem apenas a economia cosmética. Assim o jogador ganha novidades sem acelerar demais os upgrades. O cálculo da rota segura considera a posição no momento do spawn; não garante que o jogador não mude para uma colisão depois, e por isso o teste de jogabilidade continua necessário.
