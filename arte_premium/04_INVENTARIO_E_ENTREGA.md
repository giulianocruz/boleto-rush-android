# Inventário e integração — Boleto Rush! premium

## P0 — produzir primeiro e testar no Android
| ID | Nome final | Uso real | Saída/observação |
|---|---|---|---|
| P01 | p01_ziper_referencia.png | guia oficial do personagem | imagem referência / não importar no APK |
| P02 | p02_icone_loja.png | imagem Play Store | exportar **512×512 PNG 32 bits**, até 1 MB |
| P03 | p03_logo_conceito.png | direção do logotipo | reconstituir texto exato e exportar em vetor/PNG |
| P04 | p04_hero_personagem.png | imagem separada do menu | PNG RGBA recortado e otimizado |
| P05 | p05_fundo_menu.png | menu portrait | 1080×1920, WEBP/JPG otimizado |
| P06 | p06_fundo_gameplay.png | gameplay portrait | 1080×1920, WEBP/JPG otimizado |
| P07 | p07_ziper_sprite_idle.png | player | exportar PNG/WebP com alpha, ~256–384px |
| P09 | p09_moeda.png | coletável | exportar sprite com alpha, 128–256px |
| P10 | p10_boleto.png | obstáculo | exportar sprite com alpha, 128–256px |
| P11 | p11_escudo.png | powerup | exportar sprite com alpha, 128–256px |
| P12 | p12_vida.png | powerup | exportar sprite com alpha, 128–256px |
| P14 | p14_ui_referencia.png | UI Compose | referência visual; NÃO usar como screenshot fake |
| P19 | p19_banner_play_base.png | feature graphic Play | compor logo exato e exportar **1024×500 PNG 24 bits/JPG sem alpha** |

## P1 — polimento da experiência
P08 sprite escudado; P13 efeitos; P15 ícones; P16 vitória; P17 derrota; P18 badges; P20 tutorial pictogramas.

## P2 — marketing e lançamento
P21 post quadrado; P22 story; V01–V04 vídeos; S01–S06 áudio.

## Regras de integração no projeto existente (Kotlin + Compose)
O jogo em `app/src/main/java/br/com/proximaera/boletorush/MainActivity.kt` ainda usa desenho vetorial via `Canvas` para cenário, personagem, itens e efeitos. As artes entregues devem substituir *somente* a apresentação, preservando a lógica de colisão, hitboxes, tempo dos modos e placar.

Fluxo ideal:
1. Receber fontes em `arte_premium/recebidas` e escolher melhores versões.
2. Remover fundo indesejado, conferir alpha e recortar espaço transparente inútil.
3. Otimizar `player`, `moeda`, `boleto`, `escudo`, `coração` para escala real no Android (evitar texturas 1024px para elemento de 48px).
4. Guardar arquivos finais em `arte_premium/aprovadas`, copiando os assets de runtime para `app/src/main/res/drawable-nodpi/` ou `assets/` com nomes Android sem letras maiúsculas.
5. Renderizar sprites escalados e alinhados **nas mesmas coordenadas** do jogo original; manter colisões no estado/modelo, independentes da imagem.
6. HUD e botões permanecem em Compose (acessibilidade, textos traduzíveis, responsividade), aplicando apenas paleta, tipografia e estilos; evitar botões estáticos "achatados" como PNG.
7. Fazer uma partida completa de 45s e outra de 90s no emulador e em telefone físico.
8. Capturar screenshots REAIS da nova versão e só então preparar peças da loja e o vídeo real.

## Google Play — detalhes de exportação
- Ícone Play Store: 512×512, PNG 32 bits com alpha, tamanho máximo 1024 KB; a imagem pode ter fundo opaco, mas o arquivo deve estar no formato correto.
- Feature graphic: 1024×500, JPG ou PNG 24 bits SEM transparência.
- Screenshots: mostrar a experiência real, em retrato 9:16 para nosso caso; gerar ao menos 3 boas capturas reais de jogo para elegibilidade promocional, primeiro conjunto prioriza gameplay, não apenas capa.
- Não publicar prints gerados por IA que simulem telas/recursos inexistentes.
- Não usar no banner falsas promessas ou chamar o app de "#1", "mais baixado" etc.
- Políticas/cadastro Play Store, assinatura da build e publicação seguem processo separado; criar a arte não significa app publicado na loja.

Fonte para consultar no momento da exportação:
https://support.google.com/googleplay/android-developer/answer/9866151

## Definição de pronto (DoD)
- Um mascote oficialmente escolhido, consistente nos principais assets.
- Ícone legível em 48px e banner legível em miniatura.
- Personagem e itens distinguíveis no celular em movimento.
- Sem texto gerado incorreto, marcas de terceiros ou canal alpha falso.
- FPS e tamanho do APK revisados após importação.
- Capturas reais atualizadas depois da implementação visual.
- Não divulgar a publicação Google Play antes da aprovação real.
