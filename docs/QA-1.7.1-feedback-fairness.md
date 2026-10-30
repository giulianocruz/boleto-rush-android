# Boleto Rush 1.7.1 QA — clareza, PIX Turbo e justiça da progressão

## Motivação

Feedback real da versão 1.7: em alguns momentos o placar subia sem moedas; o jogo também precisa de novos itens financeiros e progressão lenta, interessante e compreensível.

## Alterações

1. Pontos positivos passam a ser gerados apenas pela coleta de moedas. Notas dão exclusivamente saldo cosmético; cartão dá limite; escudo protege; vida cura; ímã atrai; PIX multiplica a pontuação das moedas. O cheque especial ainda pode reduzir a pontuação.
2. Ímã segue impedido de capturar moedas ainda ocultas no topo da tela (correção v1.7 mantida).
3. Novo pickup PIX Turbo: moeda vale o dobro por 6 segundos (não duplica saldo de loja). Feedback e halo visual verde, contador no HUD.
4. Gerador de drops isolado em GameDrops.kt e testado: uma moeda garantida após três pickups não-moeda; não gera dois perigos consecutivos; sem boletos nos primeiros 5 segundos.
5. Quatro capítulos agora têm nome fixo na HUD, aviso temporário de mudança, tons de céu e placas de bairro discretas: Centro, Banco, Comércio e Cobrança.
6. Tutorial menciona saldo extra, tela Como Jogar rolável e textos da loja esclarecem moedas visíveis e economia.
7. Mantido app QA paralelo, com identificador br.com.proximaera.boletorush.qa; versão 1.7.1-qa.debug (code 19).

## Plano de QA

- [x] Gradle testDebugUnitTest + assembleDebug: BUILD SUCCESSFUL (build-171-qa.log local)
- [x] Atualização QA via adb install -r sem limpar dados nem mexer no app principal
- [x] Samsung Galaxy A55: versão 1.7.1-qa.debug, versionCode 19; PID ativo e nenhum erro AndroidRuntime na abertura inicial
- [x] Smoke test físico: abrir tutorial, pular treino, entrar no jogo, verificar HUD, contador e placar (3 moedas = 36 pontos), sem crash; botão Voltar pausou corretamente
- [x] Como Jogar e loja navegáveis no Galaxy A55
- [ ] Jogo completo no modo clássico e maratona
- [ ] Verificar que nota/escudo/cartão/vida/ímã/PIX não aumentam pontuação diretamente
- [ ] Testar PIX + ímã simultaneamente; duração, halo e contador
- [ ] Conferir economia: moeda rara, notas, bônus e dívida do cartão
- [ ] Validar visual dos capítulos e controles em aparelho físico; não publicar Play antes disso

## Limitações

Sprites dos novos pickups e placas são desenhos procedurais temporários. O gerador evita perigos em sequência temporal, mas não garante ausência de colisões difíceis quando os objetos de velocidades diferentes chegam juntos; isso precisa de avaliação visual durante as partidas de QA.

Nenhuma moeda do jogo representa crédito ou dinheiro real. Anúncios continuam opcionais e AdMob definitivo precisa de configuração própria para uma release publicável.
