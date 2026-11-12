# Boleto Rush! — QA 2.0.0 (Álbum + Missão Semanal)
Data: 12/11/2026

## Novos sistemas
- O álbum registra separadamente quantas relíquias BRONZE, PRATA e OURO já foram capturadas, mesmo após gastar os fragmentos na loja.
- Metas permanentes: 5 bronzes, 3 pratas e 1 ouro, para desbloquear o visual exclusivo GUARDIÃO DAS RELÍQUIAS.
- O novo visual não pode ser comprado por R$0 enquanto a coleção estiver incompleta; depois de conquistado, pode ser equipado no álbum ou na loja.
- Missão semanal offline: acumular 8 fragmentos realmente salvos na semana ISO local e receber 16 notas R$ virtuais. Apenas uma concessão por semana; relógio retrocedendo não reabre semana antiga.
- Novas telas/atalhos no menu, cofre e resultado; visual da carteira ganha brilho exclusivo.
- A missão considera apenas fragmentos bancados pelo salvamento. O bônus de AdMob continua voluntário e não multiplica a recompensa semanal.
- Atualização transacional do álbum, carteira e missão no editor único de GameSettlement. Recibo novo V3 preserva leitura de recibos V1 e V2, evitando créditos duplicados após reinícios.
- Nenhuma recompensa é resgatável por dinheiro real, prêmio físico ou compra externa.

## Monetização AdMob
- Mantidos os fluxos de anúncios recompensados opcionais para revive, bônus de capítulo, dobrar ganhos e +1 relíquia após coleta válida.
- O bônus AdMob é liberado apenas após conclusão do vídeo e passa pelo registro único da partida. Recusar consentimento ou ficar offline não impede o jogo.
- IDs de publicidade de TESTE continuam ativos. Nenhuma monetização real foi liberada. Produção requer IDs próprios, revisão UMP, chave de upload e Play Console.

## QA automatizado (Insano)
- Comando: `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`
- Resultado: BUILD SUCCESSFUL. JUnit: **82/82 testes aprovados**, 0 falhas e 0 erros, incluindo:
  - Migração compatível dos recibos V1/V2 → V3.
  - Contagens permanentes e visual grátis somente ao completar a coleção.
  - Missão semanal única com bônus que não é dobrado por anúncio.
  - Persistência atômica e retry após falha de gravação.
  - ISO week na virada de ano e bloqueio da data anterior.
  - Economia de relíquias, trocas, dívidas e recompensa AdMob preservadas.

## Roteiro de teste manual no Android
1. Abrir o menu e acessar `ÁLBUM DE RELÍQUIAS`: coleção inicialmente incompleta, semana visível e progresso 0/8.
2. Jogar Clássico e Maratona; encontrar bronze, prata e ouro. Confirmar no álbum que cada peça coletada é registrada após o resultado salvo.
3. Gastar fragmentos no Cofre: contagens do álbum devem permanecer iguais, saldo de fragmentos deve diminuir.
4. Juntar 5 bronze, 3 prata, 1 ouro (ao longo de várias partidas). Equipar GUARDIÃO DAS RELÍQUIAS; não deve consumir notas virtuais.
5. Juntar 8 fragmentos em uma semana: conferir uma única recompensa de R$16 virtual. Partidas adicionais não devem repetir o prêmio.
6. Reabrir o aplicativo e conferir o álbum, a semana, as moedas/notas, o visual equipado e o último resultado.
7. Confirmar que os anúncios recompensados são opcionais, exibem IDs de teste e não duplicam relíquias.
8. Testar desempenho em celular físico, botões, menu, tela longa do álbum e interface em tela menor.

## Limitações de homologação
- Sem aparelho conectado por ADB nesta rodada e com RAM livre insuficiente para iniciar emulador com segurança.
- APK assinado **debug** e pacote paralelo `br.com.proximaera.boletorush.qa`, não AAB de publicação.
- Não publicar na Google Play nem mesclar esta PR na release estável antes de teste físico, IDs AdMob próprios, política de dados e chave de upload.

## APK disponível no desktop do Insano
- Caminho: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.0.0-ALBUM-SEMANAL-QA.apk`
- Pacote `br.com.proximaera.boletorush.qa`; `versionCode=35`; `versionName=2.0.0-qa.debug`.
- Tamanho: 17.424.419 bytes.
- SHA-256: `BBB86975267F301FFC36AD5F14E640DE5897B891247CC2D860F7E5D1E3242754`.
- API mínima 26, destino 36. Assinatura debug, **não publicar como release Play**.
