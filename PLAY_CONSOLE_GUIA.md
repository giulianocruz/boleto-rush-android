# Publicar Boleto Rush! — Guia da Google Play Console (Brasil, novembro/2026)

**Situação em 12/11/2026:** o candidato consolidado atual é **Boleto Rush 2.3.10-QA**, pacote de testes independente `br.com.proximaera.boletorush.qa`, `versionCode=47`, `targetSdk=36`. A compilação Android e **122 testes unitários** passaram (0 falhas), mas **faltam partidas completas no celular físico**, anúncios recompensados/consentimento UMP e capturas reais atualizadas. AdMob ainda contém **IDs de teste** e a **assinatura de upload comercial não está configurada**: o Gradle bloqueia corretamente `bundleRelease` e `assembleRelease` sem passar pela verificação. **Não enviar o APK de QA à Play nem afirmar que o lançamento está homologado.** A documentação consolidada e os bloqueios atuais ficam em `docs/LANCAMENTO_236_CONSOLIDADO.md`. Para executar o APK no Insano, use `TESTAR-BOLETO-RUSH-ANDROID.cmd` com USB/depuração autorizada. A RC1 de 1.5 mencionada abaixo é histórica.

## 1. Criar aplicativo
Acesse https://play.google.com/console/ e selecione **Todos os apps > Criar app** (os rótulos podem variar).

| Campo | Preenchimento sugerido |
|---|---|
| Nome | Boleto Rush! |
| Idioma principal | Português (Brasil) |
| App ou jogo | Jogo |
| Gratuito ou pago | Gratuito |
| Nome do pacote (applicationId) | `br.com.proximaera.boletorush` — não alterar |
| Categoria da loja | Jogos > Casual |
| Nome do desenvolvedor | O nome verificado da sua conta Play Console (Próxima Era Games somente se cadastrado/aprovado) |
| Contato de suporte | Seu e-mail de suporte real e monitorado — **não inventar endereço** |
| Países / regiões | Começar pelo Brasil; ampliar após revisar classificação e localização |
| Compras internas | Não existem |
| Acesso/login | Sem login, cadastro ou acesso restrito |

**Atenção:** depois que um app é oferecido gratuitamente, não pode ser convertido em pago no mesmo pacote. Monetização com anúncio opcional continua sendo app grátis.

## 2. Conteúdo do app — Política e programas > Conteúdo do app
Conclua todos os cards apresentados pela conta.

- **Política de privacidade:** a versão do site indicada no README é `https://giulianocruz.github.io/boleto-rush-android/privacy.html`, e seu acesso externo foi **verificado em 12/11/2026**: HTTP 200, HTML e conteúdo específico do Boleto Rush. É necessário continuar revisando a exatidão das declarações de privacidade e de Segurança dos dados. O arquivo `PRIVACIDADE.md` dentro de um repositório GitHub privado não substitui uma página pública. O jogo possui opção de privacidade no menu; ainda é necessário confirmar a abertura do link dentro do aplicativo no Android real e sua aceitação na Play Console.
- **Anúncios:** **SIM**. O anúncio recompensado é opcional após perder; não há intersticial obrigatório.
- **Acesso ao app:** sem login/senha. Instruções de acesso: "O jogo abre diretamente no menu; selecione Clássico ou Maratona e toque em Jogar agora. Para encontrar o anúncio opcional, perca as três vidas; o recurso depende de rede e consentimento."
- **Classificação de conteúdo (IARC):** responder ao questionário com a experiência real; trata-se de arcade casual com caricaturas de documentos, sem apostas e sem pagamentos. **Não inventar classificação indicativa**.
- **Público-alvo:** declarar as faixas realmente pretendidas. Se incluir crianças com menos de 13 anos, revisar as exigências especiais de Google Play Families e as configurações de anúncios antes de publicar. Não escolher uma idade artificialmente só para evitar políticas.
- **App de notícias / aplicativo governamental:** não se aplica, pelo conceito atual.
- **Permissões:** Internet e estado de rede são usados pelo SDK do AdMob e UMP. Verificar a permissão `AD_ID` incorporada automaticamente pelo SDK e responder corretamente à declaração de ID de publicidade.
- **Uso de dados / Segurança dos dados:** **não** marcar "não coletamos dados" sem considerar o SDK de anúncios, mesmo que recordes e preferência fiquem localmente. O Google Mobile Ads informa coleta/compartilhamento de informações de dispositivo/IDs, interações no app, diagnóstico e IP (com possível inferência de localização aproximada) para anúncios, análises e prevenção de fraude. Usar o formulário atual, confrontar com a versão do SDK e as configurações reais, declarar finalidades e transmissão criptografada conforme a documentação do provedor. Ver https://developers.google.com/admob/android/privacy/play-data-disclosure e https://support.google.com/googleplay/android-developer/answer/10787469 .
- **Conta e exclusão de conta:** não existe criação de conta no jogo. Não inventar processo de exclusão de conta que não existe.

## 3. Ficha principal da loja
**Título (até 30 caracteres):** Boleto Rush!

**Descrição curta (até 80 caracteres):**
Desvie dos boletos, colete moedas e supere seus recordes!

**Descrição completa:**
A vida cobra. Você desvia!

No Boleto Rush!, uma carteira corajosa enfrenta uma chuva de boletos em uma cidade brasileira cheia de humor. Mova para os lados, colete moedas, faça combos, pegue escudos e sobreviva aos desafios.

- CLÁSSICO: sobreviva por 45 segundos.
- MARATONA: sobreviva por 90 segundos.
- Colete moedas, faça combos, pratique o Raspão e supere seus recordes.
- Encontre relíquias de bronze, prata e ouro, troque fragmentos no Cofre e complete o álbum de coleção.
- Supere missões semanais e o desafio Fechamento do Mês para liberar visuais e recompensas virtuais.
- Use escudos, conquiste vidas extras e aprenda com o tutorial.
- Controle rápido com um dedo, pausa e som opcional.
- Anúncios recompensados são opcionais: quando disponíveis, podem oferecer uma segunda chance ou bônus virtuais. Não há anúncios obrigatórios durante as partidas.

Divirta-se com uma corrida casual original, feita no Brasil pela Próxima Era Games.

O jogo funciona offline. As funções de publicidade e consentimento exigem conexão à internet.

**Categoria:** Casual; **idioma:** Português (Brasil). O vídeo de prévia é opcional.

## 4. Imagens da loja — arquivos corretos
- **Ícone:** `store/play-icon-512.png` — o ícone transparente novo, PNG RGBA de 512×512, máximo 1 MB. Não usar a antiga imagem JPG quadrada.
- **Imagem de destaque:** `store/feature-graphic-1024x500.jpg` — JPG 1024×500, sem transparência.
- **Capturas antigas (somente para referência):** a RC1 passou no smoke test do emulador e foram geradas **duas capturas reais em 1080×1920**, de menu e gameplay, no artefato `BoletoRush-1.5.0-rc1-SCREENSHOTS-REAIS` do GitHub Actions (run 40698310814). Foram também exportadas como RGB PNG de 24 bits para o kit de publicação. São obrigatórias **no mínimo 2** capturas reais do app para a página. Para boa descoberta de jogos, aconselha-se **3 ou mais em 1080×1920, retrato 9:16**, feitas a partir do APK atual, contendo gameplay, seleção de modo, tutorial/resultado. As capturas antigas do diretório `store/` são de beta 1.2; não usar como imagem da RC sem comparação visual. Mockups gerados por IA não substituem capturas autênticas.
- Sugestão de ordem: (1) gameplay com carteira/moedas/boletos; (2) clássico/maratona e mascote no menu; (3) escudo, vidas e resultado. Não inventar pontuação, botão ou anúncio que não existem.

Requisitos gráficos oficiais: https://support.google.com/googleplay/android-developer/answer/9866151 .

## 5. AdMob: IDs próprios antes da loja
1. Em https://admob.google.com/ crie/encontre **Boleto Rush!** como app Android com package `br.com.proximaera.boletorush`.
2. Obtenha **App ID** no formato `ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy`.
3. Crie uma unidade **Recompensado** e obtenha **Ad Unit ID** no formato `ca-app-pub-xxxxxxxxxxxxxxxx/yyyyyyyyyy`.
4. Configure mensagens e consentimento no AdMob/UMP; revise tratamento do público-alvo.
5. Em execução de teste com ID real, habilite o dispositivo como dispositivo de teste; não clique anúncios reais.
6. O código de produção aceita `BOLETO_ADMOB_APP_ID` e `BOLETO_REWARDED_UNIT_ID` via variáveis de ambiente/Secrets da compilação. Sem esses valores usa os IDs demonstrativos do Google, que **não são uma configuração publicável**.
7. Se exigido para publicidade, configurar `app-ads.txt` no site do desenvolvedor e verificar a propriedade no AdMob; isso é separado de `ads.txt`.

## 6. Assinatura Android e AAB
O pacote para Google Play é um **AAB assinado com a chave de upload do titular**. Use Play App Signing. Não confunda chave de upload com assinatura de debug nem publique senha ou `.jks` no Git.

**Caminho mais simples — Android Studio:**
- Abrir o projeto e selecionar `Build > Generate Signed Bundle / APK > Android App Bundle`.
- Criar uma nova chave de upload (ou selecionar a já existente), guardar o arquivo e suas senhas em local privado com backup seguro.
- Selecionar `release` e gerar `app-release.aab`.
- Antes do envio, configurar IDs reais do AdMob e conferir com `./gradlew :app:verifyPlayConfiguration` (variáveis de ambiente/gradle properties apropriadas).
- A chave usada em atualizações futuras deve ser a mesma chave de upload, salvo procedimento oficial de troca.

**Caminho automatizado — GitHub Actions:**
- Repositório > Settings > Secrets and variables > Actions > New repository secret.
- Adicionar `BOLETO_ADMOB_APP_ID` e `BOLETO_REWARDED_UNIT_ID` (valores reais).
- Adicionar `BOLETO_UPLOAD_KEYSTORE_BASE64` (conteúdo Base64 do arquivo da chave — nunca no código), `BOLETO_UPLOAD_STORE_PASSWORD`, `BOLETO_UPLOAD_KEY_ALIAS` e `BOLETO_UPLOAD_KEY_PASSWORD`.
- Executar o workflow `Boleto Rush — QA APK + Signed Play AAB` somente quando houver cota disponível. **Sem segredos de publicação, ele roda os testes e gera exclusivamente o APK QA com anúncios de demonstração**; com a chave de upload e IDs comerciais válidos, gera também o AAB assinado. Alternativamente, usar Android Studio/Gradle autorizado no Insano. A tentativa de executar `bundleRelease` ou `assembleRelease` sem os pré-requisitos é **bloqueada intencionalmente**. Não depender da cota do GitHub Actions para compilar o APK QA localmente.
- Não enviar senhas ou arquivo de chave pelo chat público.

## 7. Testes e publicação na Play
1. **Teste interno:** enviar AAB assinado e convidar testadores confiáveis. Testar abertura, 45 s e 90 s completos, colisões, revive recompensado, consentimento, botão da política, offline, orientação e retorno após sair do app.
2. **Teste fechado:** se sua conta pessoal de desenvolvedor foi criada **depois de 13/11/2023**, o Google exige ao menos **12 pessoas inscritas por 14 dias consecutivos** antes de pedir acesso à produção.
3. **Produção:** somente após resultados satisfatórios, responder à solicitação de acesso, completar declarações e submeter o app à revisão. O Google decide a aprovação.
4. É possível distribuir antes um APK beta via GitHub para QA, mas isso **não substitui** o teste fechado da Play Console.

Requisitos de teste: https://support.google.com/googleplay/android-developer/answer/14151465 .

## 8. Texto proposto para o primeiro lançamento (após homologação da 2.3.9)
"Boleto Rush! chegou: desvie dos boletos, colete moedas e relíquias, enfrente desafios, complete o álbum de colecionáveis e libere visuais em partidas de 45 ou 90 segundos. Os anúncios recompensados são opcionais."

**Atenção:** validar no Android real todos os recursos citados. O texto antigo da RC1 não descreve a versão 2.3.9.

## 9. Definição de pronto
- [ ] Android abre e termina partidas completas de 45s e 90s no **celular físico**, sem crash, perda de controle ou corte de interface na versão **2.3.10-QA**. Testes antigos de RC1 não substituem esta homologação.
- [ ] Ícone com fundo transparente, menu, Cofre, álbum, relíquias, missão semanal, Fechamento do Mês, proteção inicial e pausa conferidos em Android real.
- [ ] Capturas reais atualizadas, ícone e feature banner corretos.
- [ ] Vídeos de **teste** do AdMob e UMP validados em aparelho real; depois, configuração de IDs reais validada com consentimento, sem cliques em anúncios comerciais.
- [ ] Política e formulário Segurança dos dados condizem com SDK e comportamento.
- [ ] Chave de upload criada, armazenada com backup e AAB assinado.
- [ ] Teste interno concluído; para contas pessoais novas sujeitas à regra, **12 testadores inscritos continuamente por 14 dias no teste fechado**, seguidos de solicitação de acesso à produção. Conferir exigências específicas na Play Console.
- [ ] Google Play Console aprovou a publicação.

**Não lançar as edições 13/22 como variantes idênticas.** São projetos futuros, com nomes, mecânicas e conteúdo diferenciados.

## Atualização da QA 2.3.7 — backup e ícone

A versão 2.3.7 declara regras explícitas para não transferir automaticamente progresso e preferências locais via mecanismos de backup do Android 8+. A ficha/FAQ devem informar claramente que não há sincronização ou restauração de progresso entre aparelhos. O ícone adaptativo recebeu variante monocromática, que ainda precisa de inspeção no celular. Consulte `docs/QA_237_ANDROID_COMPAT.md`.

## Ajuste de anúncios recompensados — 2.3.8-QA

A tela de resultados deixa de oferecer vídeos de duplicação com ambas as carteiras lotadas. Quando só parte dos valores cabe, mostra moedas e notas que realmente serão creditadas e ajusta a legenda do botão. O crédito continua único e atômico. Ver documentação: `docs/QA_238_AD_REWARD_FAIRNESS.md`.

## QA 2.3.9 — relógio e retorno ao app

O loop da partida foi protegido contra avanço quando a Activity está em segundo plano. Testar em Android real o retorno de anúncios recompensados e a troca de aplicativos, confirmando que o relógio não avança com a tela oculta. Ver `docs/QA_239_CLOCK_FOREGROUND.md`.

## Verificação adicional — identidade de Play 2.3.9

O Gradle agora possui `:app:verifyPlayIdentity`, também executado pela QA do CI. O manifesto de release processado em 12/11/2026 confirmou o pacote `br.com.proximaera.boletorush` e `versionName=2.3.9` **sem sufixo QA**; o APK debug segue `br.com.proximaera.boletorush.qa / 2.3.9-qa.debug`. O AAB assinado **ainda não foi gerado**. Confira os tamanhos dos recursos e as capturas antigas em `docs/QA_240_PLAY_IDENTITY.md`.

## Atualização 2.3.10 — comprovantes de partida

A QA corrigiu os campos do recibo para mostrar somente o valor realmente depositado quando notas/moedas atingem o limite máximo. Também mantém o selo mensal e a missão semanal registrados mesmo se a recompensa de notas não couber integralmente. A variante comercial agora tem versionName `2.3.10` e a QA `2.3.10-qa.debug`, code 47. Ainda não foi gerado um AAB para a Play. Consulte `docs/QA_2310_RECEIPTS.md`.
