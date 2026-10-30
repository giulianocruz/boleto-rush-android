# Boleto Rush 1.6.0 QA — Progressão e poderes

**Estado:** desenvolvimento/testes, sem envio ao Google Play de produção. Versão interna 1.5.7 preservada.

## Mecânica: Ímã
- Novo power-up magenta em queda, ativado ao contato com a carteira, com chance de surgir após seis segundos.
- Duração de 6 segundos na base, +2s por nível permanente (até 12 segundos).
- Enquanto ativo, todas as moedas visíveis na área do jogo são coletadas automaticamente, sem atrair boletos, corações ou escudos.
- Sequência de 8 moedas consecutivas também libera um ímã se ele não estiver ativo.
- Aura magenta, HUD, vibração e som existentes; representação visual vetorial provisória.

## Economia / Loja
- Cada moeda coletada gera 1 moeda de cofre ao finalizar a rodada, separada da pontuação.
- Saldo e melhorias ficam nas preferências locais do Android. Sem compra real, cadastro ou servidor.
- Novos jogadores e instalações atualizadas começam com saldo zero; recordes antigos são mantidos.
- Ímã: nível 0–3, duração 6/8/10/12s; preços 12/48/108.
- Proteção inicial: nível 0–3, duração 0/2/4/6s; preços 18/72/162.
- Renda extra: nível 0–3, saldo de 1/2/3/4 moedas por moeda coletada; preços 24/96/216.
- Saldo insuficiente e melhorias máximas não gastam moedas.

## Próximas etapas
1. Missões diárias locais e conquistas.
2. Bônus por combo e skins cosméticas.
3. Teste de economia com partidas reais e balanceamento.
4. Monetização opcional somente após consentimento e IDs próprios do AdMob.

## Testes
- Android 1.6.0-qa / versionCode 16, sem liberar a produção.
- Smoke Android automatizado cobre navegação na loja, tutorial, partida e resultado.
- Validar assinatura e instalação no Samsung após regressão antes de nova versão de teste interno.
