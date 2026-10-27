# Prompt para integrar as artes na aplicação existente — Gemini / Android Studio

Use esta instrução APÓS colocar as artes aprovadas no repositório:

> Você é desenvolvedor Android Kotlin/Jetpack Compose sênior de jogos casuais mobile. Trabalhe no repositório EXISTENTE `BoletoRush`, package `br.com.proximaera.boletorush`, sem criar projeto novo e sem trocar a engine. Leia `arte_premium/00_DIRECAO_DE_ARTE.md` e `arte_premium/04_INVENTARIO_E_ENTREGA.md`. As imagens foram aprovadas em `arte_premium/aprovadas`.
>
> Integre em pequenos commits: (1) asset loader/cache para os bitmaps otimizados; (2) sprite do mascote e itens mantendo os hitboxes; (3) background do menu e gameplay usando cropping adequado a 9:16; (4) atualização nativa de botões, HUD e painéis em Compose com contrastes acessíveis e tipografia consistente; (5) efeitos de coleta e dano de baixo custo; (6) resultados e tutorial visuais.
>
> REGRAS: preserve ambos modos Clássico 45s e Maratona 90s, recordes separados em SharedPreferences, pausa, som opcional, stats, lifecycle. Não inserir login, API, anúncios, compras nem permissões. Não inventar novos sprites não entregues. Não colocar textos como bitmap em botões. Implementar fallback para os desenhos antigos caso um PNG esteja ausente. Limitar memória, manter 60fps onde possível, minimizar recomposições desnecessárias e não realizar decodificação de imagens no loop de frames.
>
> Antes de fazer release: compile APK debug e AAB release; instale em emulador; valide menu, tutorial, selecionar ambos os modos, HUD, pausa/retomada, fim de partida, recordes, tamanho do APK e logs; documente resultados honestos em TESTES.md. Não publicar na Play Store sem AAB com assinatura própria e confirmação do titular.

### Checklist de aceite visual
A nova arte deve melhorar a experiência sem esconder boletos, moedas, pontos ou botões. Tela pequena e aparelhos com menos memória têm prioridade. Cada imagem deve ter um motivo funcional e sua referência deve permanecer rastreável no repositório.
