# Boleto Rush 1.8.3 QA — consentimento seguro e desempenho

## Problema auditado
- Na versão 1.8.2, a ativação do UMP na tela PLAY podia iniciar uma coleta de consentimento que ficaria visível durante a partida.
- O utilitário combinado de UMP carrega e apresenta o formulário assim que ele estiver pronto. Isso pode interromper a jogabilidade caso a rede esteja lenta.
- O processamento de todos os drops ordenava a lista em cada frame, mesmo quando nenhum item defensivo estava ativo.

## Correções incrementais
- Após o primeiro frame do menu (e fora de onCreate), iniciar a atualização do UMP uma vez por sessão.
- Exibir formulário somente com MENU em primeiro plano e sem diálogo de diagnóstico/crash ativo.
- Carregar o formulário separadamente e reavaliar a tela antes de chamar ConsentForm.show; se o usuário tiver começado partida, manter formulário pendente até retornar ao menu.
- Em caso de não haver consentimento concedido, bloquear a solicitação de anúncios via consentInformation.canRequestAds(). Continuar jogabilidade sem anúncios.
- Retry de anúncios somente em momentos elegíveis; nenhuma publicidade intersticial obrigatória.
- Evitar ordenação e alocação extra por frame quando não há power-up defensivo; manter prioridade se necessário.
- Teste puro das condições que autorizam a apresentação de consentimento.
- VersionCode 31, 1.8.3-qa, aplicação paralela br.com.proximaera.boletorush.qa.

## Gate antes de produção
- :app:testDebugUnitTest e :app:assembleDebug devem passar sem falhas.
- Verificar o UMP em dispositivo real com configurações de teste geográfico: menu seguro, clique rápido em JOGAR, troca de janela, aparelho offline e reabertura.
- Testar anúncios de resgate, revive e pós-missão, consentimento, anúncios indisponíveis e duplicidade de prêmio.
- Verificar FPS, colisões simultâneas, primeira abertura, RAM e restart do jogo.
- AdMob ainda possui IDs de testes: não gera faturamento. Não mexer na Play Store sem validação.
