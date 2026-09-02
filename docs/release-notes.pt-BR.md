---
title: Notas de versão
layout: page
nav_order: 5
has_toc: false
lang: pt-BR
permalink: /release-notes.html
machine-translated: true
---

# Notas de versão

O Soundscape 2.0 é uma versão importante e está atualmente em beta fechado. A principal mudança é
que o Soundscape agora tem algo útil a dizer quando você viaja de carro, ônibus ou trem, e não
apenas quando está a pé. Há também muito trabalho de menor porte sobre como os lugares são
descritos, vinte novos idiomas e uma longa lista de correções.

As notas de versões anteriores estão na página
[Notas de versão da 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Novidades da versão 2.0

* **Avisos durante viagens de carro, ônibus ou trem.** O Soundscape reconhece quando você está se
  deslocando em velocidade e descreve a viagem em vez do ambiente imediato.
* **Aviso ao cruzar cursos d'água e ferrovias.** Rios, canais, enseadas e linhas ferroviárias são
  anunciados quando você os cruza, tanto a pé quanto em viagem.
* **Melhores endereços e nomes de lugares.** Lugares sem endereço próprio agora recebem a rua e a
  região em que estão, os números são associados ao lado correto da rua e os pontos de ônibus na
  Grã-Bretanha usam seus nomes oficiais.
* **Vinte novos idiomas**, totalizando 46. Este site de documentação também foi traduzido.
* **Despertar ao sair.** O modo de repouso agora pode acordar o Soundscape quando você deixa o local
  onde o colocou para dormir.
* **Distâncias mais curtas e naturais**, com unidades maiores quando você se desloca rapidamente.
* **Uma saída mais rápida.** *Sair do Soundscape* agora fica no topo do menu principal.
* **Melhorias nos mapas off-line**, incluindo a atualização de um mapa já baixado e um mapa das
  regiões disponíveis neste site.
* **Muito trabalho de acessibilidade** com o TalkBack, especialmente nas telas iniciais.
* **Muitíssimas correções de travamentos e de estabilidade.**

Duas coisas foram **removidas** na versão 2.0: o controle por voz e o menu de idioma dentro do
aplicativo. Veja [Itens removidos](#things-that-have-been-removed) abaixo para saber o que fazer no
lugar.

---

## Em mais detalhes

### Viajar de carro, ônibus ou trem

Esta é a maior novidade para quem já usa o aplicativo. Antes, o Soundscape tinha muito pouco a dizer
assim que você entrava em um veículo: continuava descrevendo o ambiente imediato, o que em
velocidade significava um fluxo de coisas pelas quais você já havia passado.

Agora o Soundscape percebe que você está se deslocando mais rápido do que caminhando e muda o que
informa. Não há nada para ativar, e tudo volta ao normal por conta própria assim que você diminui a
velocidade ou desce e segue a pé.

Durante a viagem você vai ouvir:

* **Onde você está**, de tempos em tempos: a rodovia em que está e a direção em que segue, por
  exemplo «Seguindo para o norte pela M8». Rodovias com número são anunciadas pelo número, e o
  Soundscape não repete a mesma rodovia toda vez que o nome da rua muda.
* **Cidades e vilarejos** para os quais você está indo, com a distância, além daqueles dos quais
  você se afasta ou pelos quais simplesmente passa.
* **Entroncamentos e saídas de rodovia** ao alcançá-los.
* **Grandes pontos de referência** ao passar por eles, como parques, hospitais, estádios e shopping
  centers.
* **Pontos de ônibus, de bonde e estações de trem** ao passar por eles. O Soundscape menciona apenas
  os pontos do seu lado da via, já que os do lado oposto atendem ao sentido contrário.
* **Rios, canais e ferrovias que você cruza.**
* **Túneis**, o que explica principalmente por que o Soundscape vai ficar em silêncio: lá dentro não
  há sinal de GPS.

Em um **trem**, o Soundscape percebe que você está em uma ferrovia e não em uma rodovia, e informa
por quais localidades você está passando e quanto percorreu desde a última estação. Descobrir isso é
mais difícil do que parece, porque rodovias e ferrovias muitas vezes correm lado a lado por
quilômetros, então boa parte do trabalho desta versão foi justamente não confundir uma com a outra.

Os avisos comuns para quem caminha — lojas próximas, travessias e assim por diante — são
propositalmente contidos durante a viagem, e as distâncias em que as coisas são anunciadas foram
bastante ampliadas, para que você saiba delas antes de já ter passado.

### Cruzar cursos d'água e ferrovias

O Soundscape agora avisa quando você cruza um rio, um canal, uma enseada, uma baía ou uma linha
ferroviária. Funciona tanto a pé quanto em viagem e abrange tanto passar por baixo quanto por cima,
de modo que tanto uma passarela quanto uma passagem subterrânea são descritas.

### Melhores endereços e nomes de lugares

Muito trabalho foi dedicado a fazer o Soundscape descrever os lugares como uma pessoa faria:

* Lugares sem endereço próprio agora são descritos pela rua e pela região em que estão, em vez de
  ficarem vagos.
* Os números são associados ao lado correto da rua. Antes, um endereço podia ser informado a partir
  da calçada oposta.
* O endereço de um lugar não repete mais o nome do próprio lugar.
* Os pontos de ônibus na Grã-Bretanha usam os nomes oficiais do transporte público, geralmente os
  que aparecem no horário e na placa do ponto.
* Trilhas sem nome que acompanham um rio ou canal agora recebem o nome do curso d'água que seguem.
* Caminhos e ruas sem nome são descritos de forma mais sensata, e as palavras usadas estão
  devidamente traduzidas em vez de aparecerem em inglês.

### Idiomas

Vinte novos idiomas foram adicionados na versão 2.0: árabe, bengali, búlgaro, catalão, croata,
tcheco, hauçá, húngaro, indonésio, coreano, marata, sérvio, eslovaco, esloveno, suaíli, tâmil,
telugo, tailandês, urdu e vietnamita. Todos esses idiomas estão em fase alfa e temos muito interesse
em receber comentários sobre a precisão deles. No total, o Soundscape agora está disponível em 46
idiomas, e este site de documentação também foi traduzido.

O árabe egípcio foi incorporado ao árabe e o luganda foi retirado, pois nenhum dos dois tinha texto
traduzido suficiente para ser útil.

As traduções são um trabalho da comunidade e agradecemos sua ajuda, ou suas correções quando algo
soar mal. Qualquer texto pode ser melhorado em
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Modo de repouso

O modo de repouso ganhou o **despertar ao sair**. Ao colocar o Soundscape em repouso, você pode
pedir que ele acorde assim que você deixar a área, o que é útil quando você chega a algum lugar e
quer silêncio até sair novamente.

### Distâncias e voz

As distâncias faladas foram encurtadas e ficaram mais naturais, e o Soundscape agora muda para
unidades maiores quando você se desloca rápido: milhas ou quilômetros em vez de uma longa contagem
em pés ou metros. Cada idioma decide por si como dizer uma distância fracionária, algo que antes era
forçado a um padrão de molde inglês.

### Mapas off-line

Os mapas off-line chegaram na versão 1.0 e vêm sendo aprimorados:

* Um mapa baixado agora pode ser atualizado no lugar quando há uma versão mais recente, pela tela de
  detalhes do trecho.
* Mapas que não podem ser usados — por exemplo, um download corrompido — agora são claramente
  sinalizados, em vez de falharem em silêncio.
* Os downloads são mais confiáveis, e a tela mostra o que está acontecendo enquanto a lista de mapas
  disponíveis é obtida, em vez de um indicador de carregamento em tela cheia.
* Um download concluído só aparece como concluído quando está realmente pronto para uso.
* Há um [mapa das regiões disponíveis]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  neste site.

### Acessibilidade

Muito trabalho foi feito no comportamento dos leitores de tela, especialmente nas telas iniciais,
onde o foco antes ia para o lugar errado. Outras melhorias incluem melhor leitura de tamanhos de
arquivo e números decimais, dicas corretas do tipo «toque duas vezes para...» em idiomas que colocam
o verbo no fim, e dicas sensatas onde não havia nenhuma.

### Menus e navegação

* **Sair do Soundscape** agora é o primeiro item do menu principal, em vez de ficar mais abaixo.
* O menu principal não deixa mais aparecer uma faixa da tela em uma das laterais, que dava a quem
  usa leitor de tela uma área extra confusa para tocar.
* O gesto de voltar do sistema não pula mais um nível quando você navega pelas categorias em Lugares
  próximos.
* O *tutorial de áudio* passou a se chamar **tutorial guiado**.
* As configurações foram organizadas, e *Restaurar padrões* agora limpa tudo corretamente.

### Estabilidade

A versão 2.0 inclui uma longa lista de correções de travamentos e congelamentos, entre eles o
travamento do aplicativo na tela de abertura, congelamentos ao restaurar as configurações, falhas
com um mapa baixado danificado, falhas ao abrir os detalhes de uma rota pela tela inicial, falhas ao
trocar de idioma e vários problemas relatados automaticamente pela Play Store. O comportamento de
bateria e de inicialização também ficou mais robusto em celulares que encerram agressivamente os
aplicativos em segundo plano.

### Itens removidos
{: #things-that-have-been-removed }

* **O controle por voz** foi removido. Nunca funcionou de forma confiável o bastante para valer a
  pena mantê-lo, e os botões de mídia dos fones cobrem boa parte da mesma necessidade — veja
  [Ajuda sobre o uso dos controles de mídia]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **O menu de idioma dentro do aplicativo** foi retirado. O Soundscape agora segue o idioma
  configurado no celular, que é o que a maioria das pessoas esperava. Para mudá-lo, troque o idioma
  do celular ou defina um idioma por aplicativo nas configurações, se houver essa opção.

## Como nos informar de problemas

Se algo não estiver certo, gostaríamos de saber. Escreva para o Help Desk em
<soundscapeAndroid@scottishtecharmy.support>, ou pergunte no Slack se você for membro da STA.

Se um aviso saiu errado ou não aconteceu, uma gravação da sua viagem nos ajuda enormemente: podemos
reproduzi-la e ver exatamente com quais dados o Soundscape estava trabalhando. As instruções estão
em
[Fornecer um registro de localização para depuração]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Uma nota sobre o iPhone

Tudo acima se refere ao aplicativo Android, mas vale saber para onde foi o restante do trabalho
desta versão. O Soundscape agora também roda no iPhone, e os dois aplicativos são criados a partir
do mesmo código compartilhado: as mesmas telas, os mesmos textos e os mesmos avisos. Assim, uma
novidade como os avisos de viagem descritos acima chega aos dois ao mesmo tempo, em vez de ser
escrita duas vezes. Essa base comum explica por que a versão 2.0 demorou o que demorou, e é o que
deve fazer as próximas versões chegarem mais rápido às duas plataformas. O aplicativo para iPhone
está disponível no momento pelo TestFlight mediante convite: pergunte no Slack se você for membro da
STA, ou escreva para o Help Desk.
