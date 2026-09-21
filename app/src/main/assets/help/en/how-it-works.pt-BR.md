---
title: Como o Soundscape funciona
layout: page
parent: "Usando o Soundscape"
has_toc: false
lang: pt-BR
permalink: /users/how-it-works.html
machine-translated: true
---
# Como o Soundscape funciona
O objetivo desta página é dar uma compreensão geral de como o aplicativo Soundscape funciona internamente. Você não precisa ler isto para usar o aplicativo, mas há alguns motivos pelos quais ele foi escrito:

1. Para ajudar qualquer recém-chegado interessado no aplicativo a entender quais são suas limitações
1. Para dar aos usuários uma ideia do que mais seria possível com novos recursos
1. Para dar aos desenvolvedores uma visão geral da função do aplicativo

Há duas tecnologias que tornam o aplicativo possível: o GPS e os dados do OpenStreetMap. O GPS nos dá uma boa ideia de onde o telefone está e por onde ele esteve. Os dados do OpenStreetMap podem então ser usados para descobrir o que está próximo e podemos usá-los para descrever isso ao usuário.

## Sinalizadores Sonoros
Na maioria dos aspectos, estes são os elementos mais simples de implementar do ponto de vista tecnológico. Supondo que tenhamos a localização do telefone e uma direção para o telefone, podemos então alterar o áudio do sinalizador para que ele soe como se viesse daquela direção. Usamos uma biblioteca da Steam Audio para realizar o posicionamento do áudio, que utiliza funções de transferência relacionadas à cabeça (head related transfer functions) para fornecer o melhor posicionamento sonoro possível. A única outra coisa que fazemos é alterar o áudio do sinalizador para que um som diferente seja reproduzido dependendo do ângulo entre a direção do usuário e a localização do sinalizador. Os ângulos variam dependendo do sinalizador selecionado (Tátil, Chama, Zunido etc.) e alguns têm um número maior de sons do que outros. E isso é o sinalizador sonoro no seu nível mais simples.

A única complexidade adicional é a suposição sobre a localização do telefone e a direção para a qual o usuário está apontando. Vamos analisar cada uma delas.

### Localização
A localização retornada pelo GPS pode ter um erro bastante grande, e isso depende de quanto do céu está visível para o GPS do telefone e de quantas árvores e edifícios altos estão refletindo o sinal de GPS no caminho até o telefone.

A abordagem que adotamos para filtrar a localização é usar o que é conhecido como correspondência de mapa (map matching). Isso pressupõe que o usuário está mais propenso a estar se deslocando ao longo de um caminho ou estrada mapeados - usamos o termo 'Via' (Way) para abranger todas as estradas, trilhas e caminhos. A correspondência de mapa observa por onde o usuário esteve e, usando a direção do movimento juntamente com os dados de mapeamento local, escolhe a localização mais provável em uma Via. Essa abordagem não apenas leva em conta os erros do GPS, mas também os erros nos dados do mapa. Nem todas as Vias são mapeadas com precisão e, portanto, elas também têm erros. Para determinar qual é a Via mais provável em que o usuário está, o algoritmo considera:
* Quão próxima uma Via está da localização do GPS e das localizações anteriores do GPS
* A direção do deslocamento - o usuário está se movendo na mesma direção da Via
* Se é possível ir da última localização correspondida no mapa até a nova localização por meio da rede de Vias. Isso é necessário para descartar a alternância entre Vias que na verdade não estão conectadas, por exemplo, uma passa sobre a outra por uma ponte, ou por baixo dela por um túnel.
A correspondência de mapa pode decidir que não há Vias próximas, ou que não está confiante sobre qual delas o usuário está, e nesse caso simplesmente aguarda a próxima localização do GPS e tenta novamente até ter confiança.

### Direção
Há várias direções que rastreamos no software:

1. A direção para a qual o telefone está apontando. Usamos isso quando o telefone está desbloqueado e o aplicativo está em uso, mas também quando o telefone está bloqueado, desde que seja mantido na horizontal com a tela apontando para o céu. É útil ter isso em mente ao colocar o telefone na sua bolsa. Se ele for colocado na horizontal no fundo de uma bolsa que está em pé, a direção aleatória para a qual a bolsa está apontando seria usada pelo aplicativo.
1. A direção na qual o telefone está se deslocando.
1. A direção dos fones de ouvido com rastreamento de cabeça. Atualmente não usamos isso, embora o aplicativo iOS tenha suportado. Temos a tecnologia disponível para adicioná-la no futuro.

Quando o telefone está bloqueado e dentro de uma bolsa, o aplicativo usará a direção do deslocamento. No entanto, se o usuário não estiver se movendo, então nenhuma direção fica disponível. Quando isso acontece, os sinalizadores sonoros ficam mais silenciosos para indicar que não é possível saber a direção atual do sinalizador - o usuário poderia estar se virando sem mudar de localização.

Para alguns usos da direção no aplicativo, a direção é 'ajustada' (snapped) à direção da Via correspondida no mapa, então, se o usuário estiver caminhando aproximadamente na direção da Via, a direção real da Via é assumida como correta e é usada nesses cálculos.

### Conclusão
Embora à primeira vista os sinalizadores sonoros sejam simples, o uso da correspondência de mapa para tentar remover os erros de localização e direção introduz uma boa quantidade de complexidade.

## Dados do mapa
Os dados do mapa usados pelo aplicativo originam-se quase todos do projeto OpenStreetMap. Mantemos um servidor que contém um mapa do mundo inteiro em vários níveis de zoom. Cada nível de zoom é dividido em blocos (tiles). O nível de zoom 0 contém 1 bloco, o nível 1 contém 4 blocos, o nível 2 contém 16 blocos e assim por diante, até o nível 14, que contém cerca de 268 milhões de blocos para cobrir o planeta. Cada bloco contém várias camadas, e cada camada tem pontos, linhas e polígonos que podem ser desenhados para criar um mapa gráfico. Esse mapa gráfico é o que é mostrado ao usuário na interface gráfica do aplicativo. Cada ponto, linha e polígono tem metadados que descrevem o que ele é. Isso vem na maior parte diretamente dos dados do OpenStreetMap, então uma linha pode ser uma `footway` que é uma `sidewalk` (calçada) ou uma `road` (estrada) que é `minor` (secundária).

Os dados são transformados no mapa gráfico por meio de um 'estilo' que tem regras sobre como desenhar os diferentes pontos, linhas e polígonos em cada camada, por exemplo, como desenhar um caminho, como desenhar uma floresta, como desenhar um ponto de ônibus. As regras podem variar conforme o nível de zoom, e é por isso que, à medida que você dá zoom, mais e mais pontos e linhas se tornam visíveis, os quais não ficam visíveis quando o zoom está afastado, por exemplo, pontos de ônibus e caminhos.

Ao alterar o estilo, podemos mudar a aparência do mapa da interface gráfica, e é daí que vem o 'mapa acessível' que estamos testando. Ele pretende ter maior contraste e linhas e textos mais marcantes. O estilo está embutido no aplicativo, então não precisamos alterar o mapa no servidor para mudar sua aparência.

Mas como usamos os dados do mapa para o áudio?

### Usando os dados do mapa para o áudio
Atualmente usamos uma quantidade relativamente pequena dos dados de mapeamento para gerar a interface de áudio do usuário. Quase toda a interface de áudio usa apenas os blocos no nível máximo de zoom. O aplicativo une uma grade de 2 por 2 blocos ao redor de onde o usuário está e então observa apenas algumas das camadas:

* `transportation` - para todos os tipos de Vias, incluindo estradas, caminhos, ferrovias e linhas de bonde.
* `poi` - pontos de interesse, por exemplo, lojas, centros esportivos, bancos, caixas de correio, pontos de ônibus etc.
* `building` - isto é para `poi` que são mapeados como mais do que apenas um ponto, por exemplo, grandes supermercados ou prefeituras.

Ele junta linhas e polígonos através dos limites dos blocos e transforma todas as Vias em segmentos de Via conectados e interseções. Isso é importante porque nos permite buscar ao longo de uma Via para descobrir aonde podemos chegar.

Todos os dados analisados também são colocados em um formato fácil de pesquisar, de modo que o aplicativo possa encontrar facilmente quais elementos do mapa estão próximos. Nesse ponto, os dados são classificados em categorias. As categorias atuais são:

* Estradas
* Estradas e caminhos (todas as Vias)
* Interseções - os pontos nos quais as Vias se cruzam
* Entradas - estes são pontos em um edifício que foram marcados como uma entrada.
* Cruzamentos - travessias de estrada
* POIs - todos os pontos de interesse
* Pontos de transporte público - pontos de ônibus, estações ferroviárias, paradas de bonde e assim por diante.
* Subcategorias de POIs:
  * Informação
  * Objeto
  * Lugar
  * Ponto de Referência
  * Mobilidade
  * Segurança
* Povoamentos e subcategorias (veja a próxima seção)
  * Cidades grandes
  * Cidades
  * Vilas
  * Aldeias

 Com isso implementado, para qualquer localização o aplicativo pode então facilmente encontrar

 * "Todos os pontos de transporte público num raio de 50m" ou
 * "A interseção mais próxima à minha frente" ou
 * "A aldeia, vila, cidade ou cidade grande mais próxima"

Com isso implementado, criar as notificações de áudio é apenas uma questão de consultar os dados com base na localização e direção atuais. À medida que o usuário se move por uma grade de blocos, ela é atualizada para que fique centralizada na localização atual.

### Mais dados
Um dos problemas com nossa grade de dados de mapa muito local é que isso significa que só podemos 'enxergar' no máximo cerca de 1km em qualquer direção. Isso é aceitável para quando estamos descrevendo o que está à nossa frente, mas às vezes gostaríamos de fornecer mais contexto. O principal exemplo disso é quando o aplicativo é usado e o usuário não está caminhando.

Quando o aplicativo detecta que o usuário está se deslocando a mais de 5 metros por segundo, ele muda a forma como descreve o mundo. Em vez de anunciar cada interseção e POI, ele anuncia com menos frequência e apenas estradas próximas. O problema com isso é que saber o nome de uma estrada não é muito útil se você não sabe em qual cidade ela está.

Para tentar resolver isso, agora também analisamos os dados do mapa em um nível de zoom mais baixo e extraímos dados da camada `place`. Esta contém os nomes de cidades grandes, cidades, bairros, vilas e assim por diante. Um problema ao mapear coisas é que nem sempre há um limite óbvio entre esses lugares. O OpenStreetMap às vezes tem limites de cidades em seu banco de dados, mas mesmo quando esse é o caso, quando essa informação chega ao nosso mapa em blocos, ela frequentemente se perde. O que temos é a localização onde os nomes dos lugares são desenhados no mapa. Estes são categorizados e então o aplicativo encontra a aldeia, vila, cidade ou cidade grande mais próxima do usuário e a informa.

Para muitas cidades grandes, o nome real da cidade nunca será anunciado, porque a maioria das cidades grandes é dividida em divisões menores, como bairros, mas esses fornecem contexto extra e são muito úteis. Apenas lembre-se de que, quando o aplicativo informa que você está perto de uma rua em um bairro específico, isso significa apenas que o rótulo daquele bairro é o ponto mais próximo, e ele pode estar incorreto ou até mesmo do outro lado de um rio.

### Mais contexto
Quanto mais contexto puder ser adicionado nas descrições, melhor, desde que seja mantido conciso e previsível. Um dos problemas que vimos ao descrever interseções é que muitas vezes havia Vias 'sem nome' envolvidas. Estas são Vias que não têm nome. Nos dados de mapeamento, elas podem ser apenas uma trilha, um caminho ou uma estrada de serviço, mas sem mais contexto não é muito útil nas descrições em texto. Felizmente, podemos fazer melhor, então o que o aplicativo faz é, sempre que está prestes a anunciar uma Via sem nome, ele verifica se consegue descobrir mais contexto para ela.

* **É uma calçada?**
Muitas áreas do OpenStreetMap agora têm calçadas mapeadas separadamente das estradas. Estas geralmente são marcadas como `sidewalk`, mas normalmente não indicam qual é a estrada da qual elas são a calçada.

    Quando o aplicativo encontra uma calçada sem nome, ele busca uma estrada que ele acha que está correndo ao lado dela e a usa para nomear a calçada. Isso acaba sendo muito importante para nossas notificações. Em vez de anunciar cada interseção de calçada, à medida que nos movemos ao longo de uma calçada, as notificações são feitas como se estivéssemos nos movendo ao longo da estrada associada. Em vez de *"Deslocando-se para Oeste ao longo do caminho"*, temos *"Deslocando-se para Oeste ao longo da Moor Road"*. O usuário está na calçada mapeada, mas a descrição faz mais sentido.

* **Ela termina em uma Via com nome?**
Muito frequentemente há caminhos para pedestres que ligam duas estradas. Ao observar as duas extremidades do caminho, podemos facilmente adicionar esse contexto, de modo que em uma direção pode ser *"Caminho para a Moor Road"* e, aproximando-se pela outra extremidade, pode ser *"Caminho para a Roselea Drive"*. Isso só é feito quando o caminho não se divide; se ele se divide em dois caminhos sem nome, então não tentamos adicionar esse contexto.

* **Ela termina perto de um Favorito?**
Se uma Via sem nome começa ou termina perto de um Favorito, isso é usado para descrevê-la, por exemplo, *"Caminho para o cruzamento da árvore grande"*. O usuário pode adicionar Favoritos onde quiser, e ao adicionar Favoritos ao longo das redes de caminhos, pode adicionar contexto a uma rota inteira.

* **Ela entra ou sai de um POI?**
Se uma Via sem nome começa fora de um POI e termina dentro dele (ou vice-versa), então podemos adicionar esse contexto, por exemplo, *"Trilha para o Lennox Park"*.

* **Ela termina perto de uma Entrada?**
Se uma Via sem nome começa ou termina mais perto de uma Entrada, então podemos adicionar esse contexto, por exemplo, *"Estrada de serviço para a Best Buy"*.

* **Ela termina perto de um Ponto de Referência ou Lugar?**
Se uma Via sem nome começa ou termina mais perto de um Ponto de Referência, então também podemos adicionar esse contexto, por exemplo, *"Estrada de serviço para a Catedral de St. Giles"*.

* **É um beco sem saída?**
O aplicativo marca como beco sem saída qualquer Via sem nome que não leve a lugar nenhum.

* **Ela passa por algum degrau?**
Se a Via sem nome passa sobre uma ponte, através de um túnel ou sobe/desce degraus, então isso é anotado e adicionado ao contexto. Isso é separado da marcação de destino, então um contexto como *"Caminho sobre ponte para o Lennox Park"* é possível.

Esses contextos são adicionados em ordem e, portanto, é possível ter *"Caminho para a Park Lane via degraus"* em uma direção e *"Caminho para o Lennox Park via degraus"* na outra direção. A rua com nome tem prioridade ao sair, mas o parque é usado ao entrar nele.

#### Contexto futuro
Há vários contextos adicionais que esperamos adicionar no futuro, incluindo:
* Contexto para Vias que acompanham elementos lineares de água, por exemplo, *"Caminho ao lado do Rio Dee"*
* Contexto para Vias que acompanham a borda de corpos d'água *"Caminho ao lado do reservatório de Milngavie"*
* Contexto para Vias que acompanham ferrovias, por exemplo, *"Caminho ao lado da ferrovia"*. Isso poderia até incluir o nome da linha ferroviária
* Adicionar conteúdo extra a pontes e túneis, sobre o que elas passam por cima ou por baixo, por exemplo, *"Caminho via ponte sobre ferrovia para a Moor Road"*

## Notificações de áudio
Agora que temos os dados do mapa em um formato que podemos usar facilmente, gerar as notificações realmente é bastante simples.

### Notificações ao caminhar
Ao caminhar, as notificações de áudio que podem ocorrer são (em ordem de prioridade):

1. Descrever a que distância está o destino atual
1. Descrever uma interseção próxima
1. Descrever os 5 pontos de interesse mais próximos

Todas as notificações têm sua frequência limitada para que não se repitam com muita frequência. Se o usuário parar de se mover, as notificações param e, mesmo em movimento, uma notificação não se repetirá a cada nova localização do GPS. A frequência, conforme o aplicativo iOS, é:

* A cada 60 segundos para o destino atual
* A cada 30 segundos para uma interseção próxima
* A cada 60 segundos para um ponto de interesse

As notificações podem ser filtradas pelo menu de ajustes, e certamente há margem para ampliar esse comportamento.

### Notificações ao se deslocar mais rápido
Ao se deslocar a mais de 5 metros por segundo, a notificação do destino atual ainda ocorre, mas as notificações de interseção e de pontos de interesse são substituídas por uma notificação que descreve aproximadamente onde o usuário está. Isso fornece um ponto de transporte público próximo, um ponto de interesse que nos contém, por exemplo, dentro de um grande parque, ou uma estrada e povoamento próximos. Estes usam os dados descritos anteriormente, e há espaço óbvio para permitir a personalização disso no futuro.

## Favoritos e Rotas
Em grande parte, os favoritos e as rotas são apenas um recurso de interface do usuário que não depende nem do GPS nem realmente dos dados do mapa. Favoritos são localizações nomeadas que o usuário deseja armazenar, e rotas são uma lista ordenada desses favoritos. A interface do usuário para criar ambos foi retirada diretamente da versão iOS.

### Reprodução de rota
A reprodução de rota é onde as rotas ganham vida. Quando uma rota é reproduzida, um sinalizador sonoro é criado no primeiro favorito da rota. Assim que o usuário se aproxima desse favorito, a rota automaticamente move o sinalizador sonoro para o próximo favorito da rota. Se não houver mais favoritos, então a reprodução da rota termina.

## Conclusão
Esperamos que isso tenha dado alguma visão sobre como o aplicativo funciona. O aplicativo está sempre evoluindo com base no feedback dos usuários, então, por favor, entre em contato se houver algo que você acha que poderia ser adicionado.
