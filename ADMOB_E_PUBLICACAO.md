# Boleto Rush — checklist AdMob e publicação

## Implementação desta beta
- Formato: **Rewarded Ad** somente por decisão do jogador após perder todas as vidas.
- Recompensa: revive uma vez com uma vida; invulnerabilidade inicial e limpeza dos obstáculos.
- Não mostra anúncio na tela inicial ou no meio de uma partida.
- Se o anúncio não estiver disponível ou for fechado sem obter recompensa, não concede revive.
- Componentes: Google Mobile Ads SDK **25.5.0** e UMP **4.0.0**.
- **TEST_APP_ID:** ca-app-pub-3940256099942544~3347511713.
- **TEST_REWARDED_AD_UNIT:** ca-app-pub-3940256099942544/5224354917.
- **Esses IDs pertencem ao ambiente de teste do Google, não monetizam e não devem ser enviados para a produção comercial definitiva.**

## Antes de monetizar
1. Registrar o Boleto Rush em uma conta própria AdMob, com o package br.com.proximaera.boletorush.
2. Criar bloco de anúncios **Recompensados** e substituir o APP ID no AndroidManifest e o ad unit ID no RewardedReviveAds.kt. Não publicar contas reais em testes locais sem configurar dispositivos de teste.
3. Configurar mensagens UMP em AdMob > Privacidade e mensagens. Validar EEE, Reino Unido e Suíça conforme os requisitos aplicáveis.
4. Confirmar classificação indicativa e público-alvo; se o aplicativo mirar crianças, revisar Google Families e sinalizações do SDK antes de monetizar.
5. Revisar o formulário Segurança de Dados / ID de publicidade / anúncios e esta política de privacidade na Play Console.
6. Gerar chave de upload própria e um AAB **assinado**, em local seguro fora do Git. Não enviar o bundle unsigned.
7. Cumprir testes exigidos por sua conta Play Console e esperar revisão da Google.

## Referências
- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/rewarded
- https://developers.google.com/admob/android/privacy

## O que não afirmar ainda
- Não há anúncios de produção habilitados.
- Não há receita ativa com AdMob.
- Uma versão pública no GitHub **não** significa lançamento oficial no Google Play.
