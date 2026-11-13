# Boleto Rush 1.8.1 QA — poderes e vídeos recompensados

## Objetivo
Evolução incremental sobre a 1.8.0, preservando a linha pública de produção. Aplicativo QA paralelo (br.com.proximaera.boletorush.qa) com mídias e dados independentes.

## Efeitos incorporados
- Turbo: por 5 segundos a carteira aproxima-se do destino com resposta 26/s, antes 17/s.
- Câmera lenta: por 5 segundos a queda dos drops ocorre com 62% da velocidade habitual; o cronômetro da partida permanece normal.
- Armadura: 12 segundos ou 2 impactos de boleto/cheque especial; não concede moedas.
- Limpa-tela: remove somente boleto e cheque especial já visíveis, sem destruir moedas, bônus ou corações.
- Spawns raros, sujeitos à regra já existente de no máximo dois objetos não-moeda consecutivos. Disponibilidade gradual após os segundos iniciais.
- UI com símbolos desenhados diretamente no Canvas e timers de efeitos no HUD.

## Monetização (sempre opt-in)
- Ao cumprir uma missão de capítulo e avançar, pausa em um convite opcional: assistir vídeo para ganhar **uma segunda cópia** das notas virtuais da meta daquela etapa; ou continuar imediatamente.
- No resultado de derrota, vitória ou resgate, opção para assistir **um único vídeo** e duplicar as moedas raras e notas realmente depositadas na partida; os ganhos básicos já estão salvos e nunca dependem de publicidade.
- O bônus final é commitado atomicamente no SharedPreferences, identificado por rodada. Não duplica score, cartão, dívida ou conquistas; não pode ser repetido.
- Se o anúncio não estiver carregado, o jogador prossegue sem penalidade.
- O prêmio só é concedido após callback de recompensa real do SDK, e não ao abrir/fechar o anúncio.
- IDs de demonstração do Google são usados por padrão; **não representam publicidade remunerada**. Para receita real é necessário vincular ID AdMob próprio, consentimento compatível e validação Play Console.

## Gate de publicação
1. Conferir build Gradle e todas as suítes JUnit.
2. Testar instalação QA paralela no celular, abertura, 45 s e 90 s e ambos os universos.
3. Reproduzir coleta dos quatro poderes; verificar duração, animação, física e imunitários.
4. Verificar armadura com 2 impactos, timer vencido e acúmulo com escudo existente.
5. Confirmar que limpa-tela nunca apaga moeda/PIX/coração e não gera pontos artificialmente.
6. Passar por 3 etapas, completar metas e aceitar/recusar anúncios em sequência.
7. Testar morte, revive, vitória, resgate, mudança de app e reentrada; impedir bônus duplo.
8. Testar sem internet, anúncio não disponível, sem consentimento e retorno do anúncio.
9. Medir FPS, memória, consumo de bateria, crash e integridade da loja, saldos e dívida.
10. Realizar revisão de arte e usabilidade antes de merge/publicação.

## Não incluído nesta rodada
Não publicar Play Console automaticamente nem adicionar intersticiais obrigatórios. Artes próprias do Godot Brasil em Apuros ainda não foram importadas para o Android.
