# Boleto Rush 1.7.0 QA — fase financeira

## Problema reportado

O ímã capturava moedas quando Y >= 0,13 da altura da tela, mas o renderizador ocultava todos os drops acima de Y = 0,195 sob o HUD. Isso podia aumentar o placar sem moeda visível. Agora a captura magnética só acontece depois de Y >= 0,40, garantindo visualização anterior. O teste unitário inclui essa regressão.

## Alterações

- Ritmo progressivo, calculado por tempo normalizado e número limitado de partidas anteriores: menos boletos e queda mais espaçada no começo, com aumento contínuo e leve.
- Quatro capítulos de fundo por partida: começo do mês, pagamento, fim do mês e fechamento da fatura. Fundos escuros e dessaturados para não competir com sprites e pickups.
- Notas R$ = dinheiro virtual para visuais; moedas raras = upgrades. Progresso e saldos salvos localmente em chaves distintas.
- Três visuais adicionais de brilho no personagem (neon, dourado, bordô), comprados exclusivamente com notas.
- Novo cartão: +10 de crédito de loja até um teto de 25. Compras com crédito geram dívida; novos ganhos de moedas e bônus de conquistas amortizam dívida antes de entrar no saldo.
- Novo cheque especial: penalidade de 60 pontos, neutralizável por saldo extra (escudo).
- Escudo renomeado para saldo extra, mantendo seus assets temporários.
- Conquistas continuam sendo pagas uma vez, com bônus menores (6/8/12) para preservar escassez. Os ganhos antigos já creditados não são revertidos.
- Versão QA paralela `br.com.proximaera.boletorush.qa`; não usar como release Play.

## Testes

- [x] Regras automatizadas: dificuldade suave, capítulos, limiar magnético, escassez, cosméticos.
- [x] Compilação final Android `:app:assembleDebug` e `:app:testDebugUnitTest` aprovados (inclui regras de crédito, dívida, cosméticos e conquistas).
- [x] Instalação no Samsung A55 com `adb install -r` sem afetar o pacote principal `br.com.proximaera.boletorush`.
- [x] Versão `1.7.0-qa.debug` (versionCode 18), PID ativo após abertura e sem exceção AndroidRuntime registrada.
- [ ] Navegação automática: a atividade perdeu o foco para o launcher durante inspeção; PID permaneceu ativo. Ainda não comprova sessão completa de jogo.
- [ ] Navegar e testar loja, compra/equipar visual, cenário, crédito e dívida de ponta a ponta.
- [ ] Rebalancear spawn, moedas e preços com observação de 10 ou mais partidas reais.
- [ ] Criar artes próprias para cédula, cheque especial, cartão, saldo extra, efeitos de ímã e fundos dos capítulos; no QA atual são ilustrações procedurais.
- [ ] Validar anúncios, publicação e assinatura da release separadamente.

**Importante:** todos os valores e o cartão de crédito são moeda virtual de jogo; nenhuma operação bancária real.
