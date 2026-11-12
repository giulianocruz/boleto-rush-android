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
| Política de privacidade pública | **A VALIDAR** | Verificar URL publicamente acessível; **repositório GitHub privado não serve** como única página pública |
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
2. Definir público-alvo, classificação e ficha, hospedando política de privacidade em página **pública**.
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
- `README.md` e `PLAY_CONSOLE_GUIA.md` atualizados para refletir **2.3.6-QA** e evitar orientação de publicação com APK 1.5.3. A URL pública de privacidade permanece **a validar** no navegador externo; não assumir acesso público apenas por existir no README.
- **Status final ainda NO-GO:** falta aparelho real, UMP/AdMob em dispositivo, política de privacidade publicamente acessível, capturas reais atuais, chave de upload e aprovação do processo de testes Play.
