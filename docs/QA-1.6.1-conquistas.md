# Boleto Rush 1.6.1 QA — conquistas e progressão

Branch: `feat/boleto-rush-161-milestones` (base 1.6.0 QA).
Release instalada em testes internos: manter 1.5.7 estável; **não fazer upload à Play sem revisão**.

## Implementado

- Conquistas permanentes e locais, concedidas automaticamente no fim da partida:
  - Primeira reserva: 15 moedas coletadas numa partida, bônus de 20 moedas;
  - Bolso cheio: 350 pontos numa partida, bônus de 25 moedas;
  - Fatura vencida: sobreviver até o final, bônus de 40 moedas.
- Estado e prêmio gravados juntos em SharedPreferences. Conquistas só pagam uma vez.
- Resultado mostra novas conquistas e bônus recebidos.
- Loja mostra progresso e condições das três conquistas.
- Idempotência do encerramento de partida (evita pagar a mesma rodada mais de uma vez).
- Tela de resultados rolável e aviso sobre perda de moedas não contabilizadas ao abandonar uma partida.
- Nenhuma compra real, login ou novo SDK. Economia 100% offline.

## Checklist de validação

- [x] Gradle `testDebugUnitTest` e `assembleDebug` aprovados.
- [x] APK de QA com identificador `br.com.proximaera.boletorush.qa` e nome `Boleto Rush QA`, instalável em paralelo sem desinstalar o aplicativo da Play.
- [x] Instalação e abertura no Samsung Galaxy A55; versão `1.6.1-qa.debug` e processo em execução, sem erros AndroidRuntime registrados.
- [x] Menu observado na hierarquia do Android (modo, jogar, loja e versão visíveis).
- [ ] Navegar pela loja e voltar, inspecionar conquistas no aparelho.
- [ ] Verificar concessão única de conquistas e saldo em 2 ou mais partidas.
- [ ] Verificar pausa, tutorial, controles, escudo, ímã, modo clássico e maratona.
- [ ] Testar anúncio recompensado voluntário, indisponibilidade e consentimento apenas em ambiente de QA.
- [ ] Validar AAB assinado, identidade real do AdMob, acesso à Play Console e trilha de testes antes de publicar.

## Observações

O AdMob existente usa IDs demonstrativos na ausência de variáveis de ambiente configuradas; isso não é uma configuração publicável. A versão 1.6.1 deve permanecer numa ramificação separada até testes físicos concluídos e aprovação de release.
