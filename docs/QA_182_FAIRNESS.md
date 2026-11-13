# Boleto Rush 1.8.2 QA — justiça e usabilidade

## Alterações nesta rodada
- Coletas favoráveis do mesmo frame são processadas antes de Boleto / Cheque Especial. Ordem de prioridade: Limpa-tela, Armadura/Escudo/Coração, recompensas/PIX e outros, perigos.
- Limpa-tela evita dano de perigos que cruzaram o jogador no mesmo frame; moedas e outros itens bons permanecem disponíveis.
- Convite opcional de vídeo ao mudar de capítulo só interrompe a rodada se a missão tiver sido concluída **e um anúncio já estiver carregado**. Sem vídeo pronto, o jogo segue sem interrupção.
- AdMob: após falha temporária de rede ou preenchimento, tenta carregar de novo ao voltar a uma tela elegível; nunca em laço contínuo. Fechar anúncio sem obter recompensa retorna aviso, sem contabilizar prêmios.
- HUD: poderes simultâneos agrupados em duas colunas textuais (máx. dois rótulos por linha) para melhorar leitura em telas pequenas.
- Efeitos leves no Canvas: trilhas douradas no Turbo, anel azul na Câmera Lenta, anel e contagem de impactos da Armadura.
- Incremento QA versionCode 30/versionName 1.8.2-qa; instalável paralelamente ao app publicado.

## Restrições e gate
- Nenhuma mudança na branch main ou envio ao Google Play Console.
- Não usar IDs AdMob de testes para alegar receita real.
- Testes de unidade verificam regras e compilação, não substituem testes de controle e anúncios em aparelho.
- Verificar em A55 físico: colisões simultâneas, armadura/escudo, moeda +5/+10/+20, pausa, duas durações, modos Brasil em Apuros e Boleto Rush; inícios sem internet, vídeos carregados/tardios, recusa e recompensas; renderização, FPS, RAM e crash.
- Nunca substituir o APK publicado enquanto QA físico estiver pendente.
