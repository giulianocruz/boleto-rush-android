# Boleto Rush 2.3.7-QA — compatibilidade Android e auditoria Lint

## Alterações

A versão QA 2.3.7, `versionCode=44`, preserva a mecânica do Boleto Rush 2.3.6 e corrige dois avisos do Android Lint relacionados ao lançamento:

1. **Controle explícito de backup e transferência (Android 12+).** O manifesto declara `android:dataExtractionRules="@xml/data_extraction_rules"` e o arquivo `res/xml/data_extraction_rules.xml` exclui os dados locais de backup em nuvem e de transferência entre dispositivos. `android:allowBackup="false"` permanece para sistemas anteriores. Isso evita que o progresso, preferências e o histórico de rodadas sejam copiados automaticamente por esses mecanismos.
2. **Ícone adaptativo monocromático.** O novo `drawable/ic_boleto_monochrome.xml` representa uma fatura simplificada em uma cor e é referenciado pelos dois ícones adaptativos (normal e redondo). O desenho colorido continua intacto.

## Por que isso importa

O jogo guarda saldo e progressão localmente, sem sincronização entre dispositivos. O backup/transferência de preferências poderia criar comportamento inesperado e não condizente com o planejamento atual. O ícone monocromático melhora a integração com os temas de launcher nas versões recentes do Android, incluindo o Android 16.

**Limite para usuários:** ao trocar de celular ou reinstalar o aplicativo, o progresso pode não ser recuperado automaticamente. Antes de publicar, explicar isso claramente na ficha e na experiência do usuário e reconsiderar a política quando existir sincronização oficial.

## Validação

- Auditoria Android Lint executada na linha anterior 2.3.6: **0 erros e 37 avisos**. Os avisos incluíam `DataExtractionRules` e dois `MonochromeLauncherIcon`.
- Nova compilação deverá confirmar que os XMLs são válidos, os 111 testes permanecem aprovados e que esses três avisos desaparecem. Registrar os números exatos abaixo somente após a execução.
- `targetSdk=36`, `minSdk=26`, anúncio debug de demonstração e bloqueio de release sem chave de upload permanecem intactos.
- A orientação do jogo continua fixa em retrato; os avisos do Lint sobre telas grandes **não foram suprimidos**, porque a mudança precisa de inspeção visual em Android real.

## Homologação pendente

- Verificar aparência do ícone monocromático num launcher compatível, além do colorido.
- Testar instalações e atualizações sem perda de progresso. Ao migrar aparelho, confirmar que a restrição de transferência corresponde ao comportamento comunicado ao jogador.
- Verificar partidas completas de 45 e 90 segundos, Cofre, álbum, selos mensais, UMP e AdMob de teste.
- Não enviar o APK QA diretamente à Play, não ativar AdMob comercial e não mesclar o PR integrado sem aprovação de QA física.

## Resultado final no computador Insano — 12/11/2026

- Comando: `gradlew.bat --offline :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --max-workers=2 --console=plain`.
- **BUILD SUCCESSFUL**, 53 tarefas executadas/atualizadas.
- **111 testes unitários, 0 falhas e 0 erros** em 16 suítes.
- **Android Lint: 0 erros e 34 avisos**, reduzidos de **37 avisos** na versão anterior; `DataExtractionRules` e os dois `MonochromeLauncherIcon` não aparecem mais. Avisos de orientação de tela grande e estilo do código continuam abertos para revisão.
- Acrescentadas regras legadas `res/xml/backup_rules.xml` via `android:fullBackupContent` além de `android:dataExtractionRules`; ambas impedem o backup/transferência automática de dados locais.
- APK QA independente: `C:\Users\Pessoinha\Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.7-COMPATIBILIDADE-ANDROID-QA.apk`.
- Tamanho: **18.167.243 bytes**. SHA-256: `2D80058BDCDEDA8C931B6C711CC2EDE767E9F3E2BF9CFEA05D76874E66EE97E3`.
- `aapt`: `br.com.proximaera.boletorush.qa`, `versionCode=44`, `versionName=2.3.7-qa.debug`, `minSdk=26`, `targetSdk=36`.
- `apksigner verify --verbose`: assinatura **debug** válida (APK Signature Scheme v2), um signatário; não substitui assinatura comercial da Google Play.
- Atalho `TESTAR-BOLETO-RUSH-ANDROID.cmd` da Área de Trabalho atualizado para 2.3.7, backup do anterior em `TESTAR-BOLETO-RUSH-ANDROID-ANTERIOR-236.cmd`.
- Pré-verificação real do atalho: `QA_PREFLIGHT_EXIT=0`, pacote e SHA-256 confirmados, `READY_DEVICES=0`, nenhuma instalação, inicialização ou exclusão de dados.
- **Não homologado em aparelho Android real.** A versão está pronta para QA físico, não para publicação.
