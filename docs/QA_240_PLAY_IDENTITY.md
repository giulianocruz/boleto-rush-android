# Boleto Rush 2.3.9 — identidade de publicação e recursos da loja

Data: 12/11/2026 — auditoria feita no computador Insano.

## Separação segura das versões

O projeto usava `versionName = "2.3.9-qa"` em `defaultConfig` e o sufixo `.debug` na variante debug. Isso daria ao **AAB de release** o nome `2.3.9-qa`, mesmo depois da homologação.

A definição agora é:

| Variante | Application ID | VersionCode | VersionName |
| --- | --- | --- | --- |
| Play/release (ainda não gerada) | `br.com.proximaera.boletorush` | 46 | `2.3.9` |
| QA/debug | `br.com.proximaera.boletorush.qa` | 46 | `2.3.9-qa.debug` |

A tarefa **`:app:verifyPlayIdentity`** verifica esses nomes e o ID da versão de produção sem exigir chave de upload e pode ser executada em todo build QA ou CI. A tarefa de assinatura comercial `:app:verifyPlayConfiguration` passou a depender também desta validação. Isso impede que o pipeline assine uma release com `-qa` no nome ou um pacote incorreto.

O workflow `.github/workflows/android-release-candidate.yml` executa `:app:verifyPlayIdentity` no começo dos testes de QA. Não houve configuração de chaves, IDs comerciais ou anúncio real.

## Validação de identidade

- `gradlew.bat --offline :app:verifyPlayIdentity :app:testDebugUnitTest :app:assembleDebug`: **BUILD SUCCESSFUL**.
- Mensagem real: `Play identity verified: br.com.proximaera.boletorush / 2.3.9; QA version: 2.3.9-qa.debug`.
- O APK debug gerado **continua byte a byte igual** ao anterior: SHA-256 `DB6EEB2352DCA3ABBB7601DBE8511D33C7B76AAD839A8B09930F81FF9FB28E07`, versão `2.3.9-qa.debug`, code 46.
- `bundleRelease --dry-run` confirmou `:app:verifyPlayConfiguration` no grafo de release. Isso **não gera** AAB.
- O bloqueio de assinatura e os IDs AdMob de demonstração continuam ativos para impedir publicação não intencional.

## Materiais já existentes para a ficha da Play

Arquivos localizados na pasta `store/`:

| Arquivo | Tamanho |
| --- | --- |
| `play-icon-512.png` | 512 × 512 px |
| `feature-graphic-1024x500.jpg` | 1.024 × 500 px |
| `feature-graphic-1024x500.png` | 1.024 × 500 px |
| `screenshot-menu-v12.jpg` | 1.080 × 2.160 px |
| `screenshot-tutorial-v12.jpg` | 1.080 × 2.160 px |
| `screenshot-partida-v12.jpg` | 1.080 × 2.160 px |
| `screenshot-final-v12.jpg` | 1.080 × 2.160 px |

O tamanho do ícone e da imagem de destaque foi verificado localmente e corresponde às dimensões recomendadas/exigidas na documentação oficial. **As capturas v12 são antigas**: não representam necessariamente o estado da 2.3.9 e não devem ser enviadas como evidência desta versão sem comparação visual.

Antes de enviar a ficha: capturar telas reais da 2.3.9 em Android, revisar textos sobre anúncios opcionais, verificar se o recurso de 1.024 × 500 possui o conteúdo/estilo desejado e conferir as declarações de privacidade.

## Acesso e próximos bloqueios

- A QA 2.3.9 permanece no kit do Insano: `Desktop\01 - Arquivos de Jogos\Boleto Rush\BoletoRush-2.3.9-RELOGIO-SEGURO-QA.apk`.
- Sem celular conectado via ADB: falta validar partidas, segurança do retorno após vídeo, UI e screenshots reais.
- A política de privacidade pública respondeu HTTP 200 em auditoria anterior; ainda precisa conferir as declarações da ficha.
- Sem keystore/upload key e IDs próprios de AdMob: **sem AAB de release e sem upload na Google Play**.
- A PR integrada #39 permanece em rascunho, `main` intacta.

Fontes oficiais:
- Requisitos de API alvo (novos apps Android 16/API 36 desde 31/08/2026): https://support.google.com/googleplay/android-developer/answer/11926878?hl=pt-br
- Recursos gráficos e capturas para ficha: https://support.google.com/googleplay/android-developer/answer/9866151?hl=pt-BR

## Confirmação da variante real de release

Além do dry-run, executei `gradlew.bat --offline :app:processReleaseMainManifest` com sucesso. O manifesto real gerado em `app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml` contém:
- `package="br.com.proximaera.boletorush"`
- `android:versionName="2.3.9"`

Esse comando **não compila ou assina um AAB**, mas confirma a identidade que entraria na variante release, evitando o sufixo QA. A assinatura continua bloqueada até o titular configurar as credenciais.
