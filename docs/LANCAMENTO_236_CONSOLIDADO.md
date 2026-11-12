# Boleto Rush! — Candidato consolidado de QA para a Google Play

**Data de auditoria:** 12/11/2026  
**Estado:** `NÃO PUBLICAR / NO-GO` até passar na homologação física e configurar assinatura/publicidade.  
**Código candidato:** `release/boleto-rush-play-qa-20261112`, baseado em `feature/boleto-rush-236-safe-relic-trades`.  
**Alvo de revisão:** `main`. Uma única PR integra o histórico de melhorias, mas **não faz merge automático**.

## Por que houve consolidação?

A versão de trabalho se acumulou em **46 commits à frente de `origin/main`, modificando 76 arquivos** e originando várias PRs de QA encadeadas. A entrega para Play deve revisar o conjunto de ponta a ponta, não apenas a última correção isolada.

O branch integrado contém o progresso da linha 1.8 → 2.3.6, incluindo colisões justas, reviver opcional, loja, melhorias permanentes, Raspão, relíquias raras, Cofre, álbum, missão semanal, desafio mensal, proteção e política de consentimento de anúncios.

**Não mesclar a sequência de PRs antigas e a integrada simultaneamente:** elas têm código sobreposto; manter as PRs antigas como histórico/QA até a PR consolidada ser revisada.

## Estado de cada requisito

| Requisito | Estado | Evidência / próximo passo |
|---|---|---|
| Código integrado em uma branch revisável | OK | 46 commits de desenvolvimento no candidato, comparados com `origin/main` |
| `targetSdk` Android 16/API 36 | OK | `app/build.gradle.kts`, alvo 36 (requisito de novas submissões desde 31/08/2026) |
| Compilação e JUnit | OK | `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug`, **111 testes, 0 falhas**, na 2.3.6 |
| APK QA para testar | OK | `br.com.proximaera.boletorush.qa`, versão `2.3.6-qa.debug`, code 43, Android 8+ |
| Integridade do APK | OK | `aapt`, `apksigner`, SHA-256; cópia no desktop Insano |
| Assets de ícone/feature graphic | Presentes | Arquivos `store/play-icon-512.png` e `store/feature-graphic-1024x500.jpg`; revisar visual e políticas da ficha |
| Jogo completo em Android físico | **BLOQUEADO** | Sem dispositivo ADB conectado; jogar 45s/90s, validar toque, telas e FPS |
| Coleção, Cofre, missão e chefe mensal no Android | **BLOQUEADO** | Verificar ciclo de partida, prêmio único, dados após fechar/reabrir |
| Consentimento UMP e AdMob recompensado | **BLOQUEADO** | Só IDs de teste; conferir consentimento, vídeo concluído/interrompido e prêmio único no aparelho |
| Capturas atuais da versão 2.3.6 | **BLOQUEADO** | Capturar telas **reais** da versão candidata, sem mockups fictícios |
| Política de privacidade pública | **ACESSÍVEL** | URL `https://giulianocruz.github.io/boleto-rush-android/privacy.html` respondeu HTTP 200 e retornou HTML de Boleto Rush (GET externo, 12/11). Falta conferir conteúdo/declarações no Android e na Play Console |
| Segurança dos dados / classificação etária / anúncios | **A VALIDAR** | Responder na Play Console conforme SDK, público real e comportamento |
| Chave de upload comercial | **BLOQUEADO** | Titular deve configurar keystore segura fora do Git/Chat; `verifyPlayConfiguration` exige isso |
| IDs AdMob comerciais | **BLOQUEADO** | Substituir IDs demonstrativos apenas na build assinada de publicação |
| AAB comercial / teste interno | **NÃO GERADO** | Gerar após os itens anteriores; não publicar APK debug |
| Teste fechado exigido pelo tipo/data da conta | **A VALIDAR** | Se conta pessoal criada após 13/11/2023: 12 testadores inscritos por 14 dias contínuos |
| Produção | **NÃO INICIADA** | Depende da homologação, eventuais requisitos de teste e aprovação da Play |

## QA no computador Insano

**APK pré-compilado confirmado:**  
`C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.6-TROCAS-SEGURAS-QA.apk`

SHA-256: `37F6F469C83B437DB0FDCB79A3614330430B89E77B162C35EA5F5695C0DD9610`  
Tamanho: `17.457.187 bytes`  
Pacote: `br.com.proximaera.boletorush.qa`  
Versão: `2.3.6-qa.debug` (code 43).  
Assinatura válida em esquema APK v2 **de debug**, não assinatura da Google Play.

**Para iniciar o teste com um celular autorizado:** clicar em  
`C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\TESTAR-BOLETO-RUSH-ANDROID.cmd`

Esse kit só deve instalar o pacote `.qa`, não remove dados, não reinicia o aparelho e salva evidências localmente. É necessário desbloquear o celular e autorizar a depuração USB. Com `--preflight`, o atalho faz **somente leitura**.

## Critérios de aceitação do teste humano

1. Menu e tutorial legíveis sem cortes em telefone Android real (incluir escala de fonte maior se possível).
2. Terminar 45s no Clássico e 90s na Maratona; jogar partida com derrota, vitória, pausa e reviver opcional.
3. Testar colisões justas, Raspão e todos os poderes; item `Saldo Extra` não pode diminuir escudo já ativo.
4. Preparar escudo de relíquia e melhoria permanente nível 3: proteção inicia com 9s. Coleta posterior de escudo mantém o maior tempo.
5. Testar Cofre, notas/fragmentos, moedas de melhoria e dívida, incluindo recusa sem descontar relíquias se recompensa não couber.
6. Coletar relíquias de diferentes raridades; álbum permanente e missão semanal devem ser salvos após partidas, inclusive com Cofre lotado.
7. Fechamento do Mês: 3 ondas, indicação de pista segura, prêmio somente após resolver 6 perigos e terminar sem contato.
8. Fechar/reabrir app: verificar recordes, Cofre, visual equipado, missão, álbum, selos, recompensas e preferências.
9. UMP: abrir opções de privacidade, mudar consentimento, conferir ausência de anúncio antigo; vídeo incompleto não credita prêmio.
10. Capturar telas reais e revisar logcat/performance; documentar dispositivo, versão Android e eventuais falhas.

## Passos para o primeiro teste fechado

1. Concluir QA local do APK paralelo e corrigir defeitos confirmados.
2. Definir público-alvo, classificação e ficha. A página de privacidade já responde HTTP 200; revisar a correspondência das declarações de dados com o SDK e testar o link no celular.
3. Configurar App ID e Rewarded Unit ID reais no AdMob, UMP e assinatura de upload **em ambiente seguro**, nunca diretamente no Git.
4. Executar `verifyPlayConfiguration` e gerar AAB comercial `release` **assinado**, somente após aprovação das verificações.
5. Distribuir inicialmente em teste interno. Em contas pessoais sujeitas à regra, configurar teste fechado com 12 testadores inscritos por 14 dias contínuos e recolher feedback.
6. Pedir acesso à produção quando elegível, passar na revisão do Google e apenas então disponibilizar publicamente.

## Fontes oficiais consultadas (12/11/2026)

- Nível de API alvo para novos apps em 2026: https://support.google.com/googleplay/android-developer/answer/11926878
- Requisitos de teste para contas pessoais novas: https://support.google.com/googleplay/android-developer/answer/14151465
- Recursos gráficos da ficha: https://support.google.com/googleplay/android-developer/answer/9866151

**Nota:** a exigência dos 12 testadores depende da categoria e data da conta do titular. Não presumir que a regra já se aplica sem conferir a Play Console.

## Resultado efetivo da integração no Insano — 12/11/2026

- `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug --console=plain`: **BUILD SUCCESSFUL**, 43 tarefas.
- JUnit da **ramificação integrada**, não apenas a branch anterior: **111 testes / 0 falhas / 0 erros**, 16 classes.
- Metadados verificados via `aapt`: `br.com.proximaera.boletorush.qa`, `versionCode=43`, `2.3.6-qa.debug`, `minSdk=26`, `targetSdk=36`.
- Hash SHA-256 da nova build integrada: `37F6F469C83B437DB0FDCB79A3614330430B89E77B162C35EA5F5695C0DD9610`. **Idêntico** ao APK 2.3.6 já disponível na Área de Trabalho do Insano; cópia existente não foi sobrescrita.
- `gradlew.bat --offline :app:bundleRelease --dry-run --console=plain`: **`verifyPlayConfiguration` presente no grafo de dependências** de `bundleRelease` (`RELEASE_GUARD_DRY_RUN=PASS`). Nenhum AAB comercial foi gerado.
- Branch `release/boleto-rush-play-qa-20261112` mantém histórico completo: 46 commits a mais que `origin/main` antes do commit de documentação; nenhuma PR anterior foi mesclada ou descartada.
- `README.md` e `PLAY_CONSOLE_GUIA.md` atualizados para refletir **2.3.6-QA** e evitar orientação de publicação com APK 1.5.3. A URL pública de privacidade foi **confirmada acessível** por GET HTTP 200 no Insano em 12/11; ainda falta validar a abertura no app e o texto declarativo da Play Console.
- **Status final ainda NO-GO:** falta aparelho real, UMP/AdMob em dispositivo, revisão das declarações de privacidade e dados, capturas reais atuais, chave de upload e aprovação do processo de testes Play.

## Revisão do pipeline em 12/11/2026 — QA e release separados

Na PR de integração foi encontrado e corrigido um conflito: o antigo GitHub Actions tentava executar `assembleDebug :app:bundleRelease` juntos, enquanto `bundleRelease` depende da verificação de **credenciais comerciais**. Sem chave, o processo falharia antes de distribuir o próprio APK QA.

- O workflow `.github/workflows/android-release-candidate.yml` agora executa **`testDebugUnitTest :app:assembleDebug` em todos os casos**.
- A variante **debug** força os dois IDs oficiais de teste do Google AdMob, mesmo quando `BOLETO_ADMOB_APP_ID` e `BOLETO_REWARDED_UNIT_ID` estão disponíveis no ambiente. Teste com identificadores comerciais fictícios confirmou que o XML gerado ainda contém somente os IDs demonstrativos.
- O APK QA é **verificado e enviado como artifact antes** de tentar gerar um AAB comercial.
- A geração do AAB roda **somente** se o workflow recebeu chave de upload e senhas. O Gradle exige ainda IDs AdMob de publicação válidos, preservando `verifyPlayConfiguration`. Não é produzido AAB não assinado.
- O caminho de `aapt` no workflow usa `$ANDROID_HOME/build-tools/36.0.0/aapt`, evitando depender de sua presença no `PATH` da máquina GitHub Actions.
- Nome dos artifacts atualizado para 2.3.6-QA; referências antigas à 1.5.0 removidas do workflow.
- Validação local: **YAML interpretado com sucesso**, ordem de artifact e validações confirmada; **111 testes unitários / 0 falhas** e compilação Android com sucesso.
- `bundleRelease --dry-run` confirmou a presença de `verifyPlayConfiguration`. Execução direta da verificação **falhou corretamente** com `Upload key not configured`, e **não** publicou ou gerou nenhum AAB.
- Ainda não foi executado o workflow atualizado no GitHub Actions (cota da conta pode estar indisponível); esta é uma **validação local de código, não homologação de CI real**.

## Atualização posterior — candidato QA 2.3.7 (12/11/2026)

O histórico de integração 2.3.6 deste documento foi mantido como evidência. O **candidato atual** na PR #39 passou a ser **Boleto Rush 2.3.7-QA, versionCode 44**, com regras explícitas de backup e transferência local para Android 8+ e ícone monocromático adaptativo. A configuração de anúncios de teste e a proteção contra AAB comercial não assinado foram preservadas.

**Validação atual:** `BUILD SUCCESSFUL`, **111 testes / zero falhas**, **Android Lint 0 erros / 34 avisos**, redução de 3 avisos relacionados a backup e ícones. APK e SHA-256 estão em [QA 2.3.7 — compatibilidade Android](QA_237_ANDROID_COMPAT.md).

A PR integrada permanece em **NO-GO** até testes completos em celular físico, consentimento UMP/AdMob, capturas atuais da ficha, verificação de conta Play e geração de AAB assinado com credenciais comerciais mantidas fora do Git.

## Atualização posterior — candidato QA 2.3.8 (12/11/2026)

A linha histórica de QA 2.3.6 e o relatório de compatibilidade da 2.3.7 continuam válidos como registros anteriores. O **candidato mais recente** da PR integrada #39 é **Boleto Rush 2.3.8-QA, versionCode 45**, com correção do valor prometido em anúncios recompensados opcionais quando a carteira de moedas/notas está perto da capacidade máxima.

**Evidência na branch:** `BUILD SUCCESSFUL`; **115 testes / zero falhas / zero erros**; **Android Lint 0 erros / 34 avisos**. APK, SHA-256 e roteiro completos estão em [QA 2.3.8 — recompensa justa](QA_238_AD_REWARD_FAIRNESS.md).

O estado de publicação **permanece NO-GO**: faltam dispositivo físico Android, testes UMP/AdMob, capturas reais, revisão final das declarações da Play Console, assinatura de upload e AdMob comercial. A `main` não foi alterada.

## Atualização posterior — candidato QA 2.3.9 (12/11/2026)

O candidato mais recente da PR integrada #39 é **Boleto Rush 2.3.9-QA (versionCode 46)**. O loop de atualização agora exige **tela de jogo ativa e aplicativo em primeiro plano**, com renovação da referência de tempo quando o Android retorna de segundo plano. Isso evita avanço invisível durante anúncios recompensados ou troca de aplicativo.

**Resultado:** `BUILD SUCCESSFUL`, **118 testes JUnit / 0 falhas / 0 erros**, **Android Lint 0 erros / 34 avisos**. APK, assinatura, SHA-256 e roteiro em [QA 2.3.9 — relógio e anúncios](QA_239_CLOCK_FOREGROUND.md).

**Ainda NO-GO para produção:** nenhuma homologação física Android, IDs comerciais ou chave de upload, capturas atuais ou conclusões sobre desempenho real. O teste da PR #39 continua em rascunho.

## Complemento de QA — coleta pós-partida (12/11/2026)

O kit de testes Windows ganhou o modo `-CollectOnly` e um novo atalho no Insano, `COLETAR-EVIDENCIAS-BOLETO-RUSH.cmd`, para capturar o estado **após** partidas, acesso ao Cofre, álbum e opções de privacidade. Ele não reinicia nem reinstala a QA e exige o aplicativo em primeiro plano para evitar capturas de telas de outros aplicativos.

**Validação local:** parser PowerShell 5.1 sem erros, pré-verificação da QA 2.3.9 aprovada e coleta sem aparelho conectado bloqueada de modo seguro, sem evidências fictícias. A execução com celular real ainda não ocorreu. Roteiro em `docs/QA_239_POS_PARTIDA_WINDOWS.md`.
