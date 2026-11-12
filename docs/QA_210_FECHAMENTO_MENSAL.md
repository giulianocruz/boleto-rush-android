# Boleto Rush 2.1.0-QA — Fechamento do Mês

## Mecânica implementada

- Três ondas especiais nos 18 segundos finais dos modos Clássico (45s) e Maratona (90s), espaçadas em 6 segundos.
- Cada onda tem **dois perigos** e **uma pista segura**. Os corredores são esquerda (.18), centro (.50) e direita (.82), com sequência que varia conforme o mês. Uma moeda no corredor seguro dá uma referência visual.
- A segunda onda utiliza CHEQUE ESPECIAL; as outras usam BOLETOS.
- As ondas não geram anúncios, paywall, transações de compra ou recompensas em dinheiro real. A sequência interrompe o gerador habitual somente durante a janela de 18 segundos.
- Aviso na tela mostra o número da onda e a faixa segura; contatos com perigos do desafio são contados mesmo se absorvidos por poder.
- Selo mensal somente para quem venceu a partida completa, enfrentou as três ondas e não tocou em nenhum dos seus perigos.
- Recompensa de **R$25 em notas puramente virtuais para visuais** e **um selo mensal**, no máximo uma vez por mês.
- Total de selos no álbum de relíquias; recompensa do mês exibida no resultado.

## Dados e persistência

- `GameMonthChallenge.kt` contém cronograma, pistas seguras, critérios de vitória e planejamento da recompensa.
- `GameSettlement` salva a conclusão mensal junto com carteira, relicários, conquistas e extrato, usando uma única transação `SharedPreferences.Editor.commit()`.
- Recibo **V4** é retrocompatível com V1/V2/V3. Retentativa de salvar a mesma partida retorna o recibo original, sem duplicar selos.
- Retrocesso do relógio não concede selo de mês anterior já ultrapassado; a recompensa exige que o mês do início da partida coincida com o mês atual no encerramento.

## AdMob

- Sem novos formatos de anúncio: continuam apenas as opções voluntárias de vídeo recompensado implementadas anteriormente (revive, bônus e relíquia).
- UMP/consentimento e as regras de crédito único de vídeo existentes não foram alteradas.
- **IDs do AdMob de teste**, sem faturamento real. Envio à Play requer IDs reais, chave de upload e revisão de privacidade.

## QA — 12/11/2026

- Comando: `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`.
- Resultado: **BUILD SUCCESSFUL**, **90 testes, 0 falhas, 0 erros**.
- Testes novos: início e fim de ondas em 45 e 90s, todas as pistas mensais, faixa segura física, qualificação da recompensa, travas por mês, idempotência, falha transacional, retrocesso de calendário, migração V3 → V4.
- `targetSdk=36`, `versionCode=36`, `versionName=2.1.0-qa.debug`, pacote QA isolado `br.com.proximaera.boletorush.qa`.

## Roteiro para homologação física

1. Testar uma partida de 45 segundos e observar o início do chefe aos 27s; no modo de 90s, aos 72s.
2. Confirmar três ondas, aviso de faixa segura e visibilidade da moeda que marca o corredor.
3. Completar as três sem colisão, confirmar **um selo mensal e R$25 virtuais**; abrir álbum, fechar/reabrir o aplicativo e conferir persistência.
4. Repetir outra vitória no mesmo mês: **nenhuma nova recompensa mensal**.
5. Tocar em boleto de chefe com e sem escudo: não deve conceder selo naquela rodada.
6. Validar comportamento sob Câmera Lenta, Turbo, Ímã, pausa, revive, resgate antecipado e mudança de mês.
7. Testar AdMob opcional usando apenas IDs de teste; recusar anúncios não pode impedir o desafio nem o recebimento do selo.
8. Verificar legibilidade do HUD e ritmo no celular físico antes de promover a release.

**Estado:** QA automatizado aprovado; sem teste real em Android físico. Não mesclar para produção nem enviar à Play Console nesta etapa.

## APK gerado no Insano

- `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.1.0-FECHAMENTO-MENSAL-QA.apk`
- Tamanho: 17.440.803 bytes.
- SHA-256: `E94FF57A3F127C82EA4221C857433E9C264A7801A3B895403AAFA24B65E18E11`
- Pacote `br.com.proximaera.boletorush.qa`, `versionCode=36`, `2.1.0-qa.debug`, `minSdk=26`, `targetSdk=36`.
- Assinatura debug independente. Sem celular físico conectado via ADB durante a homologação.
