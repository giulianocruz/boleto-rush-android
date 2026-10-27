# Integração premium — Boleto Rush! v1.3
Data: 26/10/2026.

## Fontes
ZIP recebido pelo usuário: `out. 26 - 20_30.zip`, localizado na pasta Downloads do PC Insano. Três arquivos originais JPG:
- `Wallet_holding_golden_coin_20261026220316.jpg` — novo ícone/arte de marketing.
- `Wallet_mascot_dodging_bills_20261026220316.jpg` — arte de capa com logotipo e cidade brasileira.
- `Game_sprite_sheet_collection_ele…_20261026220316.jpg` — grade 4×4 de sprites, recortada em 16 PNGs.

Original preservado em `arte_premium/recebidas/flow_20261026/`. Reprodutibilidade dos recortes em `integrate_assets_v13.ps1`.

## Identidade visual adotada
**Carteira marrom-caramelo antropomórfica**, zíper/fecho dourado, olhos grandes, braços/pernas curtos, moedas douradas com estrela e boletos vermelhos caricaturados. A arte enviada é a nova referência visual de fato; substitui a carteira turquesa hipotética dos prompts preliminares. As demais cores turquesa/dourado/coral permanecem na interface.

## Integração técnica
- `app/src/main/res/drawable-nodpi/premium_promo.jpg` — menu ilustrado.
- `app/src/main/res/drawable-nodpi/premium_icon.jpg` — ícone no manifesto.
- `app/src/main/res/drawable-nodpi/spr_hero_*.png`: idle, run, jump, hurt.
- `app/src/main/res/drawable-nodpi/spr_bill_*.png`: idle, attack, damaged, defeated.
- `app/src/main/res/drawable-nodpi/spr_coin_*.png`: front, angle, side, collect.
- `app/src/main/res/drawable-nodpi/spr_shield_*.png`: collect, idle, active, impact.
- Sprite sheets originais são JPEGs com fundo escuro; **os PNGs têm recorte de fundo aproximado**, não são masters RGBA nativos. Inspecionar halos e bordas no celular.
- Coração fica no desenho vetorial original porque não existe sprite de coração no pacote.
- Cenário da partida e botões permanecem baseados em Canvas/Compose, em vez de criar fundos ou botões estáticos inexistentes no ZIP.

## Política de qualidade
Os hitboxes, regra de pontos, dois modos, timer, recordes locais e ausência de anúncios ficam preservados. Manter capturas de gameplay reais na Play Store. Artes geradas por IA precisam ser declaradas corretamente nos formulários aplicáveis.

## Publicação
Publicar esta versão em GitHub Release e distribuir APK debug para teste. AAB de release **sem assinatura de upload** não pode ser enviado à Google Play. A publicação na Play Store depende de chave segura, dados da conta e testes exigidos.
