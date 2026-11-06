# Boleto Rush 1.8.0 QA — Brasil em Apuros + progressão leve

## Escopo entregue (sem merge em main e sem publicação)
- Menu: alternar universo **Boleto Rush** / **Brasil em Apuros**, com preferência local salva.
- **Moedas de +5, +10 e +20 pontos** sinalizadas em cada objeto; raridade da moeda +20 aumenta discretamente após a metade da partida.
- O combo mantém +2 por moeda adicional (até 10 na sequência); PIX Turbo duplica a pontuação da coleta.
- A contagem de moedas coletadas continua independente da pontuação; moedas raras de upgrade e notas cosméticas não sofrem inflação.
- **Carreira RPG por rodada**: XP pelo valor nominal da moeda, seis níveis, nome de carreira diferente por universo. Marcos em 40, 110, 220, 360 e 550 XP. Subir de nível concede ao menos 2,5 s de escudo; XP e nível reiniciam com a partida.
- **Brasil em Apuros**: sátira financeira e política fictícia com capítulos (Promessas de Campanha, Chuva de Taxas, Guerra do Orçamento, Apagão das Contas), objetivos próprios, cenários Palácio / Plenário / Dívida e obstáculos Boleto rotulados como TAXA. Não há partidos, candidatos ou figuras reais.
- Corrigido retorno da tela de reviver: a navegação não marca a partida como concluída quando o commit de saldo falha.

## Checklist obrigatório de QA no celular Android
1. Abrir em paralelo ao app publicado e verificar que não substitui dados antigos.
2. Trocar universos; fechar/reabrir; confirmar preferência salva.
3. Coletar moedas +5, +10 e +20: texto legível, pontos corretos, XP e nível coerentes.
4. Conferir combo e PIX Turbo, inclusive coleta magnetizada.
5. Subir de nível, verificar a proteção breve e evitar recompensas repetidas.
6. Testar fases e objetivos temáticos nos modos 45 s e 90 s.
7. Testar pausa, reiniciar, resgatar saldo, segunda chance e persistência.
8. Testar loja, upgrades, notas, cartão, dívida e anúncios opcionais.
9. Conferir taxa de quadros, consumo de memória, travamento de inicialização e informações de diagnóstico.

## Restrições
Esta edição tem apenas programação/ilustração vetorial reaproveitada do projeto Android. Ainda não importa artes do jogo Godot Brasil em Apuros. A identidade final depende de revisão visual e de arte dedicada. Sem ADB ou celular conectado, os testes de unidade não comprovam qualidade visual e estabilidade em aparelho físico. Manter PR em rascunho até QA.
