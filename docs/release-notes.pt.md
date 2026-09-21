---
title: Notas de lançamento
layout: page
nav_order: 5
has_toc: false
lang: pt
permalink: /release-notes.html
machine-translated: true
---

# Notas de lançamento

O Soundscape 2.0 é uma versão importante e encontra-se atualmente em beta fechada. A principal
alteração é que o Soundscape passa a ter algo de útil a dizer quando viaja de carro, autocarro ou
comboio, e não apenas quando anda a pé. Há também muito trabalho de menor dimensão sobre a forma
como os locais são descritos, vinte novos idiomas e uma longa lista de correções.

As notas de versões anteriores estão na página
[Notas de lançamento da 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Novidades da versão 2.0

* **Anúncios durante viagens de carro, autocarro ou comboio.** O Soundscape reconhece quando se
  desloca a maior velocidade e descreve a sua viagem em vez do que o rodeia imediatamente.
* **Aviso ao atravessar cursos de água e linhas férreas.** Rios, canais, rias e linhas de caminho de
  ferro são anunciados quando os atravessa, tanto a pé como em viagem.
* **Melhores moradas e nomes de locais.** Os locais sem morada própria passam a receber a rua e a
  zona onde se encontram, os números de porta são associados ao lado correto da rua e as paragens de
  autocarro na Grã-Bretanha usam os nomes oficiais.
* **Vinte novos idiomas**, num total de 46. Este site de documentação também está traduzido.
* **Despertar ao sair.** O modo de suspensão pode agora acordar o Soundscape quando sai do local
  onde o colocou em repouso.
* **Distâncias mais curtas e naturais**, com unidades maiores quando se desloca depressa.
* **Uma saída mais rápida.** *Sair do Soundscape* está agora no topo do menu principal.
* **Melhorias nos mapas offline**, incluindo a atualização de um mapa já transferido e um mapa das
  regiões disponíveis neste site.
* **Muito trabalho de acessibilidade** com o TalkBack, sobretudo nos ecrãs iniciais.
* **Muitíssimas correções de falhas e de estabilidade.**

Foram **removidas** duas coisas na versão 2.0: o controlo por voz e o menu de idioma dentro da
aplicação. Consulte [Elementos removidos](#things-that-have-been-removed) mais abaixo para saber o
que fazer em alternativa.

---

## Com mais detalhe

### Viajar de carro, autocarro ou comboio

Esta é a maior novidade para quem já utiliza a aplicação. Antes, o Soundscape tinha muito pouco a
dizer assim que entrava num veículo: continuava a descrever o que o rodeava de imediato, o que a
velocidade elevada significava um fluxo de coisas por que já tinha passado.

O Soundscape repara agora que se desloca mais depressa do que a passo e altera o que lhe comunica.
Não há nada para ativar e tudo regressa ao normal por si assim que abranda ou sai e segue a pé.

Durante a viagem irá ouvir:

* **Onde está**, de tempos a tempos: a estrada em que segue e a direção que leva, por exemplo «A
  seguir para norte pela M8». As estradas com número são anunciadas pelo número e o Soundscape não
  volta a anunciar a mesma estrada sempre que muda o nome da rua.
* **Vilas e cidades** para onde se dirige, com a distância, bem como aquelas de que se afasta ou por
  que simplesmente passa.
* **Nós e saídas de autoestrada** quando os alcança.
* **Grandes pontos de referência** por que passa, como parques, hospitais, estádios e centros
  comerciais.
* **Paragens de autocarro, elétrico e comboio** por que passa. O Soundscape menciona apenas as
  paragens do seu lado da estrada, uma vez que as do lado oposto servem o sentido contrário.
* **Rios, canais e linhas férreas que atravessa.**
* **Túneis**, o que explica sobretudo por que motivo o Soundscape vai ficar em silêncio: lá dentro
  não há sinal de GPS.

Num **comboio**, o Soundscape percebe que segue numa linha férrea e não numa estrada, e indica-lhe
por que localidades passa e que distância percorreu desde a última estação. Perceber isto é mais
difícil do que parece, porque autoestradas e linhas férreas são muitas vezes construídas lado a lado
ao longo de quilómetros, pelo que boa parte do trabalho desta versão consistiu em não confundir uma
com a outra.

Os anúncios habituais para quem anda a pé — lojas próximas, atravessamentos e assim por diante — são
propositadamente retidos durante a viagem, e as distâncias a que as coisas são anunciadas foram
bastante alargadas, para que saiba delas antes de já as ter ultrapassado.

### Atravessar cursos de água e linhas férreas

O Soundscape avisa-o agora quando atravessa um rio, um canal, uma ria, uma baía ou uma linha de
caminho de ferro. Funciona tanto a pé como em viagem e abrange tanto passar por baixo como por cima,
pelo que uma passagem pedonal superior e um túnel pedonal são ambos descritos.

### Melhores moradas e nomes de locais

Foi dedicado muito trabalho a que o Soundscape descreva os locais como uma pessoa o faria:

* Os locais sem morada própria passam a ser descritos pela rua e pela zona onde se encontram, em vez
  de ficarem vagos.
* Os números de porta são associados ao lado correto da rua. Antes, uma morada podia ser indicada a
  partir do passeio oposto.
* A morada de um local já não repete o nome do próprio local.
* As paragens de autocarro na Grã-Bretanha usam os nomes oficiais dos transportes públicos, em geral
  os que constam dos horários e do poste da paragem.
* Os caminhos sem nome que acompanham um rio ou um canal passam a ter o nome do curso de água que
  seguem.
* Caminhos e estradas sem nome são descritos de forma mais sensata e as palavras utilizadas estão
  devidamente traduzidas, em vez de surgirem em inglês.

### Idiomas

Na versão 2.0 foram acrescentados vinte novos idiomas: árabe, bengali, búlgaro, catalão, croata,
checo, hauçá, húngaro, indonésio, coreano, marata, sérvio, eslovaco, esloveno, suaíli, tâmil,
telugu, tailandês, urdu e vietnamita. Todos estes idiomas estão em fase alfa e gostaríamos muito de
receber comentários sobre a sua exatidão. No total, o Soundscape está agora disponível em 46 idiomas
e este site de documentação também foi traduzido.

O árabe egípcio foi integrado no árabe e o luganda foi retirado, pois nenhum deles tinha texto
traduzido suficiente para ser útil.

As traduções são um trabalho comunitário e agradecemos a sua ajuda, ou as suas correções sempre que
algo se leia mal. Qualquer cadeia de texto pode ser melhorada em
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Modo de suspensão

O modo de suspensão ganhou o **despertar ao sair**. Quando coloca o Soundscape em repouso, pode
pedir-lhe que acorde assim que sair da zona, o que é útil quando chega a algum lado e quer silêncio
até voltar a partir.

### Distâncias e voz

As distâncias faladas foram encurtadas e tornadas mais naturais, e o Soundscape passa agora a
unidades maiores quando se desloca depressa: milhas ou quilómetros em vez de uma longa contagem em
pés ou metros. Cada idioma decide por si como dizer uma distância fracionária, algo que antes estava
forçado a um padrão de feitio inglês.

### Mapas offline

Os mapas offline chegaram com a versão 1.0 e têm vindo a ser melhorados:

* Um mapa já transferido pode agora ser atualizado no lugar quando existe uma versão mais recente, a
  partir do ecrã de detalhes do extrato.
* Os mapas que não podem ser usados — por exemplo, uma transferência danificada — são agora
  claramente assinalados, em vez de falharem em silêncio.
* As transferências são mais fiáveis e o ecrã mostra o que está a acontecer enquanto a lista de
  mapas disponíveis é obtida, em vez de um indicador de carregamento em ecrã inteiro.
* Uma transferência concluída só aparece como concluída quando está realmente pronta a usar.
* Existe um [mapa das regiões disponíveis]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  neste site.

### Acessibilidade

Foi feito muitíssimo trabalho sobre o comportamento dos leitores de ecrã, sobretudo nos ecrãs
iniciais, onde antes o foco saltava para o sítio errado. Outras melhorias incluem uma melhor leitura
de tamanhos de ficheiro e números decimais, indicações corretas do tipo «toque duas vezes para...»
em idiomas que colocam o verbo no fim, e sugestões sensatas onde não existia nenhuma.

### Menus e navegação

* **Sair do Soundscape** é agora o primeiro item do menu principal, em vez de estar mais abaixo.
* O menu principal já não deixa ver uma faixa do ecrã de um dos lados, que dava a quem usa leitor de
  ecrã uma área adicional confusa para tocar.
* O gesto de retroceder do sistema já não salta um nível quando percorre categorias em Locais
  próximos.
* O *tutorial áudio* passou a chamar-se **tutorial guiado**.
* As definições foram arrumadas e *Repor predefinições* limpa agora tudo corretamente.

### Estabilidade

A versão 2.0 inclui uma longa lista de correções de falhas e bloqueios, entre eles o bloqueio da
aplicação no ecrã inicial, bloqueios ao repor as definições, falhas com um mapa transferido
danificado, falhas ao abrir os detalhes de um percurso a partir do ecrã principal, falhas ao mudar
de idioma e vários problemas comunicados automaticamente através da Play Store. O comportamento de
arranque e face à bateria também foi tornado mais robusto em telemóveis que fecham agressivamente as
aplicações em segundo plano.

### Elementos removidos
{: #things-that-have-been-removed }

* **O controlo por voz** foi removido. Nunca funcionou de forma suficientemente fiável para valer a
  pena mantê-lo, e os botões multimédia dos auscultadores cobrem em grande medida o mesmo — consulte
  [Ajuda sobre a utilização dos controlos multimédia]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **O menu de idioma dentro da aplicação** desapareceu. O Soundscape segue agora o idioma definido
  no telemóvel, que é o que a maioria das pessoas esperava. Para o alterar, mude o idioma do
  telemóvel ou defina um idioma por aplicação nas respetivas definições, se estiverem disponíveis.

## Comunicar-nos problemas

Se algo não estiver bem, gostaríamos de saber. Escreva para o Help Desk em
<soundscapeAndroid@scottishtecharmy.support>, ou pergunte no Slack se for membro da STA.

Se um anúncio esteve errado ou não aconteceu, uma gravação da sua viagem ajuda-nos imenso: podemos
reproduzi-la e ver exatamente com que dados o Soundscape estava a trabalhar. As instruções estão em
[Fornecer um registo de localização para depuração]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Uma nota sobre o iPhone

Tudo o que precede diz respeito à aplicação Android, mas vale a pena saber para onde foi o restante
trabalho desta versão. O Soundscape funciona agora também no iPhone e ambas as aplicações são
construídas a partir do mesmo código partilhado: os mesmos ecrãs, as mesmas formulações e os mesmos
anúncios. Assim, uma novidade como os anúncios de viagem descritos acima chega às duas ao mesmo
tempo, em vez de ser escrita duas vezes. É esta base comum que explica a demora da versão 2.0 e é
ela que deverá fazer com que as próximas versões cheguem mais depressa a ambas as plataformas. A
aplicação para iPhone está atualmente disponível através do TestFlight mediante convite: pergunte no
Slack se for membro da STA, ou escreva para o Help Desk.
