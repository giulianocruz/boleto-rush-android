# Publicar Boleto Rush! — Guia da Google Play Console (Brasil, outubro/2026)

**Situação em 28/10/2026:** release candidate Android v1.5.0-rc1 (versionCode 8, targetSdk 36, minSdk 26). Há APK de testes e um AAB de QA *sem assinatura de upload* enquanto a chave do titular não for configurada. **Não envie o AAB não assinado à Play**. O produto contém Google Mobile Ads/UMP; sem IDs próprios, a unidade é demonstrativa e não monetiza. Ainda falta QA físico da abertura, partida completa e anúncio opcional.

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

- **Política de privacidade:** `https://github.com/giulianocruz/boleto-rush-android/blob/main/PRIVACIDADE.md`. Também existe botão no menu do jogo. Se o Google exigir página HTML dedicada, hospede o conteúdo no domínio oficial antes de enviar para revisão.
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
- Colete moedas, faça combos e supere seus recordes.
- Use escudos, conquiste vidas extras e aprenda com o tutorial.
- Controle rápido com um dedo, pausa e som opcional.
- Se perder, você pode ESCOLHER assistir a um anúncio recompensado para reviver uma vez. Não há anúncio obrigatório.

Divirta-se com uma corrida casual original, feita no Brasil pela Próxima Era Games.

O jogo funciona offline. As funções de publicidade e consentimento exigem conexão à internet.

**Categoria:** Casual; **idioma:** Português (Brasil). O vídeo de prévia é opcional.

## 4. Imagens da loja — arquivos corretos
- **Ícone:** `store/play-icon-512.png` — o ícone transparente novo, PNG RGBA de 512×512, máximo 1 MB. Não usar a antiga imagem JPG quadrada.
- **Imagem de destaque:** `store/feature-graphic-1024x500.jpg` — JPG 1024×500, sem transparência.
- **Capturas:** são obrigatórias **no mínimo 2** capturas reais do app para a página. Para boa descoberta de jogos, aconselha-se **3 ou mais em 1080×1920, retrato 9:16**, feitas a partir do APK atual, contendo gameplay, seleção de modo, tutorial/resultado. As capturas antigas do diretório `store/` são de beta 1.2; não usar como imagem da RC sem comparação visual. Mockups gerados por IA não substituem capturas autênticas.
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
- Executar workflow `Boleto Rush — Android RC (APK + AAB)` em Actions. Com tudo configurado, ele valida a configuração e assina o AAB. Sem chave, entrega **AAB não assinado para QA**, que **não pode ser enviado à Play**.
- Não enviar senhas ou arquivo de chave pelo chat público.

## 7. Testes e publicação na Play
1. **Teste interno:** enviar AAB assinado e convidar testadores confiáveis. Testar abertura, 45 s e 90 s completos, colisões, revive recompensado, consentimento, botão da política, offline, orientação e retorno após sair do app.
2. **Teste fechado:** se sua conta pessoal de desenvolvedor foi criada **depois de 13/11/2023**, o Google exige ao menos **12 pessoas inscritas por 14 dias consecutivos** antes de pedir acesso à produção.
3. **Produção:** somente após resultados satisfatórios, responder à solicitação de acesso, completar declarações e submeter o app à revisão. O Google decide a aprovação.
4. É possível distribuir antes um APK beta via GitHub para QA, mas isso **não substitui** o teste fechado da Play Console.

Requisitos de teste: https://support.google.com/googleplay/android-developer/answer/14151465 .

## 8. Texto da atualização — 1.5.0 RC1
"Primeira versão de testes para Google Play: corrida de moedas e boletos com os modos Clássico e Maratona, mascote ilustrado, recordes, proteção com escudo, vida extra e opção voluntária de anúncio recompensado para reviver. Melhorias de estabilidade e configuração de privacidade."

## 9. Definição de pronto
- [ ] Android abre e termina partidas no **celular físico**; não há crash de inicialização.
- [ ] Ícone com fundo transparente e menu conferidos no Android.
- [ ] Capturas reais atualizadas, ícone e feature banner corretos.
- [ ] Configuração AdMob real validada, anúncio opcional respeita consentimento.
- [ ] Política e formulário Segurança dos dados condizem com SDK e comportamento.
- [ ] Chave de upload criada, armazenada com backup e AAB assinado.
- [ ] Teste interno e, quando necessário, fechado satisfatórios.
- [ ] Google Play Console aprovou a publicação.

**Não lançar as edições 13/22 como variantes idênticas.** São projetos futuros, com nomes, mecânicas e conteúdo diferenciados.
