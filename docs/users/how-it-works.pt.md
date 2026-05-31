---
title: Como funciona o Soundscape
layout: page
parent: Using Soundscape
has_toc: false
lang: pt
permalink: /users/how-it-works.html
machine-translated: true
---
# Como funciona o Soundscape
O objetivo desta página é dar uma compreensão geral de como a aplicação Soundscape funciona nos bastidores. Não precisa de ler isto para utilizar a aplicação, mas existem algumas razões pelas quais foi escrita:

1. Para ajudar quaisquer recém-chegados interessados na aplicação a compreender onde estão as suas limitações
1. Para dar aos utilizadores uma ideia do que mais seria possível com novas funcionalidades
1. Para dar aos programadores uma visão geral do funcionamento da aplicação

Existem duas tecnologias que tornam a aplicação possível, o GPS e os dados do OpenStreetMap. O GPS dá-nos uma boa ideia de onde o telemóvel está, e onde esteve. Os dados do OpenStreetMap podem então ser utilizados para descobrir o que está nas proximidades e podemos usá-los para descrever isso ao utilizador.

## Sinais de áudio
Na maioria dos aspetos, estes são os elementos mais simples de implementar do ponto de vista tecnológico. Assumindo que temos a localização do telemóvel e uma direção para o telemóvel, podemos então alterar o áudio do sinal de modo a que pareça vir dessa direção. Utilizamos uma biblioteca do Steam Audio para realizar o posicionamento do áudio, que utiliza funções de transferência relacionadas com a cabeça (head related transfer functions) para proporcionar o melhor posicionamento sonoro possível. A única outra coisa que fazemos é alterar o áudio do sinal para que seja reproduzido um som diferente consoante o ângulo entre a direção do utilizador e a localização do sinal. Os ângulos variam consoante o sinal selecionado (Tátil, Chama, Ping, etc.) e alguns têm um número maior de sons do que outros. E isto são os sinais de áudio ao nível mais simples.

A única complexidade adicional é a suposição sobre a localização do telemóvel e a direção para a qual o utilizador está virado. Vejamos cada uma destas por sua vez.

### Localização
A localização devolvida pelo GPS pode ter um erro bastante grande, e isto depende de quanto céu está visível para o GPS do telemóvel, e de quantas árvores e edifícios altos estão a refletir o sinal de GPS no seu percurso até ao telemóvel.

A abordagem que adotámos para filtrar a localização é utilizar o que se conhece como correspondência de mapa (map matching). Isto assume que o utilizador tem mais probabilidade de estar a deslocar-se ao longo de um caminho ou estrada mapeada - usamos o termo 'Via' (Way) para abranger todas as estradas, trilhos e caminhos. A correspondência de mapa analisa onde o utilizador esteve e, usando a direção do movimento juntamente com os dados de mapeamento locais, escolhe a localização mais provável numa Via. Esta abordagem não só tem em conta os erros do GPS, como também os erros nos dados do mapa. Nem todas as Vias estão mapeadas com precisão e, por isso, também têm erros. Para determinar qual é a Via mais provável em que o utilizador se encontra, o algoritmo considera:
* O quão próxima uma Via está da localização GPS e das localizações GPS anteriores
* A direção de deslocação - estão a mover-se na mesma direção que a Via
* Se é possível chegar da última localização correspondida no mapa à nova localização através da rede de Vias. Isto é necessário para excluir a mudança entre Vias que não estão na realidade ligadas, por exemplo, uma passa sobre a outra por uma ponte, ou por baixo dela por um túnel.
A correspondência de mapa pode decidir que não existem Vias nas proximidades, ou que não tem confiança sobre qual delas o utilizador se encontra, e, neste caso, simplesmente aguarda pela próxima localização GPS e tenta novamente até ter confiança.

### Direção
Existem várias direções que monitorizamos no software:

1. A direção para a qual o telemóvel está apontado. Utilizamos isto quando o telemóvel está desbloqueado e a aplicação está em utilização, mas também quando o telemóvel está bloqueado, desde que seja mantido na horizontal com o ecrã virado para o céu. É útil ter isto em mente ao colocar o telemóvel na sua mala. Se for colocado na horizontal no fundo de uma mala mantida na vertical, a direção aleatória para a qual a mala está apontada seria utilizada pela aplicação.
1. A direção em que o telemóvel se está a deslocar.
1. A direção a partir de auscultadores com rastreio de movimentos da cabeça (head tracking). Atualmente não a utilizamos, embora a aplicação iOS a suportasse. Temos a tecnologia preparada para a adicionar no futuro.

Quando o telemóvel está bloqueado e numa mala, a aplicação utilizará a direção de deslocação. No entanto, se o utilizador não se estiver a mover, então não há nenhuma direção disponível. Quando isto acontece, os sinais de áudio ficam mais silenciosos para indicar que não é possível saber a direção atual do sinal - o utilizador poderia estar a virar-se sem alterar a localização.

Para algumas utilizações da direção na aplicação, a direção é 'ajustada' (snapped) à direção da Via correspondida no mapa, pelo que, se o utilizador estiver a caminhar aproximadamente na direção da Via, então assume-se que a direção real da Via está correta e é utilizada nesses cálculos.

### Conclusão
Embora à primeira vista os sinais de áudio sejam diretos, a utilização da correspondência de mapa para tentar remover os erros de localização e direção introduz uma quantidade considerável de complexidade.

## Dados do mapa
Os dados do mapa utilizados pela aplicação têm quase todos origem no projeto OpenStreetMap. Operamos um servidor que contém um mapa do mundo inteiro em múltiplos níveis de zoom. Cada nível de zoom é dividido em mosaicos (tiles). O nível de zoom 0 contém 1 mosaico, o nível 1 contém 4 mosaicos, o nível 2 contém 16 mosaicos e assim por diante até ao nível 14, que contém cerca de 268 milhões de mosaicos para cobrir o planeta. Cada mosaico contém múltiplas camadas e cada camada tem pontos, linhas e polígonos que podem ser desenhados para criar um mapa gráfico. Esse mapa gráfico é o que é mostrado ao utilizador na interface gráfica da aplicação. Cada ponto, linha e polígono tem metadados que descrevem o que é. Isto vem maioritariamente diretamente dos dados do OpenStreetMap, pelo que uma linha pode ser um `footway` que é um `sidewalk` ou uma `road` que é uma `minor`

Os dados são transformados no mapa gráfico através de um 'estilo' que tem regras sobre como desenhar os diferentes pontos, linhas e polígonos em cada camada, por exemplo, como desenhar um caminho, como desenhar uma floresta, como desenhar uma paragem de autocarro. As regras podem variar consoante o nível de zoom, razão pela qual, à medida que aproxima o zoom, cada vez mais pontos e linhas se tornam visíveis que não estão visíveis quando o zoom está afastado, por exemplo, paragens de autocarro e caminhos.

Ao alterar o estilo, podemos mudar a aparência do mapa da interface gráfica, que é de onde vem o 'mapa acessível' que estamos a experimentar. Visa ter maior contraste e linhas e texto mais a negrito. O estilo está incorporado na aplicação, pelo que não temos de alterar o mapa no servidor para mudar a sua aparência.

Mas como utilizamos os dados do mapa para o áudio?

### Utilizar os dados do mapa para o áudio
Atualmente utilizamos uma quantidade relativamente pequena dos dados de mapeamento para gerar a interface de áudio do utilizador. Quase toda a interface de áudio utiliza apenas os mosaicos no nível máximo de zoom. A aplicação junta uma grelha de 2 por 2 mosaicos à volta do local onde o utilizador se encontra e depois analisa apenas algumas das camadas:

* `transportation` - para todos os tipos de Vias, incluindo estradas, caminhos, vias férreas e linhas de elétrico.
* `poi` - pontos de interesse, por exemplo, lojas, centros desportivos, bancos de jardim, marcos de correio, paragens de autocarro, etc.
* `building` - este é para `poi` que estão mapeados como mais do que apenas um ponto, por exemplo, grandes supermercados ou câmaras municipais.

Junta linhas e polígonos através das fronteiras dos mosaicos e transforma todas as Vias em segmentos de Via ligados e cruzamentos. Isto é importante porque nos permite pesquisar ao longo de uma Via para descobrir onde podemos chegar.

Todos os dados analisados são também colocados num formato fácil de pesquisar, para que a aplicação possa facilmente encontrar quais elementos do mapa estão nas proximidades. Neste ponto, os dados são classificados em categorias. As categorias atuais são:

* Estradas
* Estradas e caminhos (todas as Vias)
* Cruzamentos - os pontos em que as Vias se cruzam
* Entradas - estes são pontos num edifício que foram marcados como uma entrada.
* Passadeiras - passadeiras de estrada
* POIs - todos os pontos de interesse
* Paragens de transportes - paragens de autocarro, estações ferroviárias, paragens de elétrico e assim por diante.
* Subcategorias de POIs:
  * Informação
  * Objeto
  * Local
  * Marco de Referência
  * Mobilidade
  * Segurança
* Povoações e subcategorias (ver secção seguinte)
  * Cidades
  * Vilas
  * Aldeias
  * Lugarejos

 Com isto implementado, para qualquer localização a aplicação pode então encontrar facilmente

 * "Todas as paragens de transportes num raio de 50 m" ou
 * "O cruzamento mais próximo à minha frente" ou
 * "O lugarejo, aldeia, vila ou cidade mais próximo"

Com isto implementado, criar os avisos de áudio é apenas uma questão de consultar os dados com base na localização e direção atuais. À medida que o utilizador se move através de uma grelha de mosaicos, esta é atualizada de modo a ficar centrada na localização atual.

### Mais dados
Um dos problemas com a nossa grelha de dados de mapa muito local é que isto significa que só conseguimos 'ver' no máximo cerca de 1 km em qualquer direção. Isto é aceitável para quando estamos a descrever o que está à nossa frente, mas por vezes gostaríamos de dar mais contexto. O principal exemplo disto é quando a aplicação é utilizada e o utilizador não está a caminhar.

Quando a aplicação deteta que o utilizador se está a deslocar a mais de 5 metros por segundo, muda a forma como descreve o mundo. Em vez de anunciar todos os cruzamentos e POIs, anuncia com menos frequência e apenas as estradas nas proximidades. O problema com isto é que saber o nome de uma estrada não é muito útil se não souber em que vila se encontra.

Para tentar resolver isto, agora também analisamos os dados do mapa num nível de zoom mais baixo e extraímos dados da camada `place`. Esta contém os nomes de vilas, cidades, bairros, aldeias e assim por diante. Um problema ao mapear estas coisas é que nem sempre existe uma fronteira óbvia entre estes locais. O OpenStreetMap por vezes tem fronteiras de cidades na sua base de dados, mas, mesmo quando é o caso, no momento em que chega ao nosso mapa de mosaicos essa informação perde-se frequentemente. O que temos é a localização onde os nomes dos locais são desenhados no mapa. Estes são categorizados e depois a aplicação encontra o lugarejo, aldeia, vila ou cidade mais próximo do utilizador e reporta isso.

Para muitas cidades, o nome real da cidade nunca será anunciado, porque a maioria das cidades está dividida em divisões mais pequenas, como bairros, mas estes dão contexto extra e são muito úteis. Apenas lembre-se que, pelo facto de a aplicação reportar que está nas proximidades de uma rua num determinado bairro, isso significa apenas que o rótulo desse bairro é o ponto mais próximo e pode estar incorreto ou até mesmo do outro lado de um rio.

### Mais contexto
Quanto mais contexto puder ser adicionado às descrições, melhor, desde que seja mantido conciso e previsível. Um dos problemas que vimos ao descrever cruzamentos é que muitas vezes havia Vias 'sem nome' envolvidas. Estas são Vias que não têm nome. Nos dados de mapeamento estas podem ser apenas um trilho, um caminho ou uma estrada de serviço, mas sem mais contexto não é muito útil nas descrições de texto. Felizmente, podemos fazer melhor, pelo que o que a aplicação faz é que, sempre que está prestes a anunciar uma Via sem nome, vê se consegue descobrir mais algum contexto para ela.

* **É um passeio?**
Muitas áreas do OpenStreetMap têm agora os passeios mapeados separadamente das estradas. Estes são normalmente etiquetados como `sidewalk`, mas normalmente não indicam qual é a estrada de que são o passeio.

    Quando a aplicação encontra um passeio sem nome, pesquisa por uma estrada que pensa estar a correr ao seu lado e utiliza-a para dar nome ao passeio. Isto acaba por ser muito importante para os nossos avisos. Em vez de anunciar cada cruzamento de passeio, à medida que nos movemos ao longo de um passeio os avisos são feitos como se estivéssemos a mover-nos ao longo da estrada associada. Em vez de *"A viajar em direção a oeste num caminho"* temos *"A viajar em direção a oeste na Moor Road"*. O utilizador está no passeio mapeado, mas a descrição faz mais sentido.

* **Termina numa Via com nome?**
Muito frequentemente existem caminhos pedonais que ligam duas estradas. Ao olhar para ambas as extremidades do caminho, podemos facilmente adicionar esse contexto, de modo a que numa direção possa ser *"Caminho para a Moor Road"* e aproximando-se da outra extremidade possa ser *"Caminho para a Roselea Drive"*. Isto só é feito quando o caminho não se bifurca; se se bifurcar em dois caminhos sem nome, então não tentamos adicionar este contexto.

* **Termina perto de um Marco?**
Se uma Via sem nome começar ou terminar perto de um Marco, este é utilizado para a descrever, por exemplo, *"Caminho para o cruzamento da árvore grande"*. O utilizador pode adicionar Marcos onde quiser e, ao adicionar Marcos ao longo de redes de caminhos, pode adicionar contexto a toda uma rota.

* **Entra ou sai de um POI?**
Se uma Via sem nome começar fora de um POI e terminar dentro dele (ou vice-versa), então podemos adicionar esse contexto, por exemplo, *"Trilho para o Lennox Park"*.

* **Termina perto de uma Entrada?**
Se uma Via sem nome começar ou terminar mais perto de uma Entrada, então podemos adicionar esse contexto, por exemplo, *"Estrada de serviço para a Best Buy"*.

* **Termina perto de um Marco de Referência ou Local?**
Se uma Via sem nome começar ou terminar mais perto de um Marco de Referência, então também podemos adicionar esse contexto, por exemplo, *"Estrada de serviço para a Catedral de St. Giles"*.

* **É um beco sem saída?**
A aplicação marca como um beco sem saída quaisquer Vias sem nome que não levem a lado nenhum.

* **Passa por algum degrau?**
Se a Via sem nome passar sobre uma ponte, através de um túnel ou subir/descer degraus, então isto é registado e adicionado ao contexto. Isto é separado da etiquetagem do destino, pelo que é possível um contexto como *"Caminho sobre a ponte para o Lennox Park"*.

Estes contextos são adicionados por ordem e, por isso, é possível ter *"Caminho para a Park Lane via degraus"* numa direção e *"Caminho para o Lennox Park via degraus"* na outra direção. A rua com nome tem prioridade ao sair do parque, mas o parque é utilizado ao entrar nele.

#### Contexto futuro
Existem vários contextos adicionais que esperamos adicionar no futuro, incluindo:
* Contexto para Vias que seguem elementos lineares de água, por exemplo, *"Caminho junto ao Rio Dee"*
* Contexto para Vias que seguem a margem de massas de água *"Caminho junto ao reservatório de Milngavie"*
* Contexto para Vias que seguem vias férreas, por exemplo, *"Caminho junto à via férrea"*. Isto poderia até incluir o nome da linha ferroviária
* Adicionar conteúdo extra a pontes e túneis, sobre o que estão por cima ou por baixo, por exemplo, *"Caminho via ponte sobre a via férrea para a Moor Road"*

## Avisos de áudio
Agora que temos os dados do mapa num formato que podemos utilizar facilmente, gerar avisos é realmente bastante direto.

### Avisos ao caminhar
Ao caminhar, os avisos de áudio que podem ocorrer são (por ordem de prioridade):

1. Descrever a que distância está o destino atual
1. Descrever um cruzamento que se aproxima
1. Descrever os 5 pontos de interesse mais próximos

Todos os avisos têm a frequência limitada para que não se repitam com demasiada frequência. Se o utilizador parar de se mover, então os avisos pararão, e mesmo em movimento um aviso não se repetirá a cada nova localização GPS. A frequência, tal como na aplicação iOS, é:

* A cada 60 segundos para o destino atual
* A cada 30 segundos para um cruzamento que se aproxima
* A cada 60 segundos para um ponto de interesse

Os avisos podem ser filtrados através do menu de definições, e há certamente margem para alargar este comportamento.

### Avisos ao deslocar-se mais rapidamente
Ao deslocar-se a mais de 5 metros por segundo, o aviso para o destino atual continua a ocorrer, mas os avisos de cruzamentos e pontos de interesse são substituídos por um aviso que descreve aproximadamente onde o utilizador se encontra. Isto fornece uma paragem de transportes próxima, um ponto de interesse que nos contém, por exemplo, dentro de um grande parque, ou uma estrada e povoação próximas. Estes utilizam os dados descritos anteriormente, e há margem óbvia para permitir a personalização disto no futuro.

## Marcos e Rotas
Na sua maioria, os marcos e as rotas são apenas uma funcionalidade da interface do utilizador que não depende nem do GPS nem realmente dos dados do mapa. Os marcos são localizações com nome que o utilizador quer armazenar, e as rotas são uma lista ordenada desses marcos. A interface do utilizador para criar ambos é retirada diretamente da versão iOS.

### Reprodução de rotas
A reprodução de rotas é onde as rotas ganham vida. Quando uma rota é reproduzida, é criado um sinal de áudio no primeiro marco da rota. Assim que o utilizador se aproxima desse marco, a rota move automaticamente o sinal de áudio para o marco seguinte da rota. Se não houver mais marcos, então a reprodução da rota termina.

## Conclusão
Esperamos que isto tenha dado alguma perceção sobre como a aplicação funciona. A aplicação está sempre a evoluir com base no feedback dos utilizadores, por isso, por favor, entre em contacto se houver algo que pense que poderia ser adicionado.
