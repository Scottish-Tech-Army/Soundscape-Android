---
title: Cómo funciona Soundscape
layout: page
parent: Using Soundscape
has_toc: false
lang: es
permalink: /users/how-it-works.html
machine-translated: true
---
# Cómo funciona Soundscape
El objetivo de esta página es ofrecer una comprensión general de cómo funciona la aplicación Soundscape por dentro. No necesita leer esto para usar la aplicación, pero hay algunas razones por las que se ha escrito:

1. Para ayudar a cualquier recién llegado interesado en la aplicación a entender cuáles son sus limitaciones
1. Para dar a los usuarios una idea de qué más sería posible con nuevas funcionalidades
1. Para dar a los desarrolladores una visión general del funcionamiento de la aplicación

Hay dos tecnologías que hacen posible la aplicación: el GPS y los datos de OpenStreetMap. El GPS nos da una buena idea de dónde está el teléfono y por dónde ha estado. Los datos de OpenStreetMap se pueden usar entonces para averiguar qué hay cerca y podemos usarlos para describírselo al usuario.

## Señales de audio
En la mayoría de los aspectos, estas son las cosas más sencillas de implementar desde un punto de vista tecnológico. Suponiendo que tengamos la ubicación del teléfono y una dirección para el teléfono, podemos alterar el audio de la señal para que suene como si proviniera de esa dirección. Usamos una biblioteca de Steam Audio para realizar el posicionamiento del audio, que usa funciones de transferencia relacionadas con la cabeza (HRTF) para ofrecer el mejor posicionamiento sonoro posible. Lo único más que hacemos es cambiar el audio de la señal de modo que se reproduzca un sonido diferente según el ángulo entre la dirección del usuario y la ubicación de la señal. Los ángulos varían según la señal seleccionada (Táctil, Bengala, Silbido, etc.) y algunas tienen un número mayor de sonidos que otras. Y eso son las señales de audio en su nivel más sencillo.

La única complejidad adicional es la suposición sobre la ubicación del teléfono y la dirección en la que apunta el usuario. Veamos cada una por turno.

### Ubicación
La ubicación devuelta por el GPS puede tener un error bastante grande, y esto depende de cuánto cielo sea visible para el GPS del teléfono y de cuántos árboles y edificios altos estén reflejando la señal del GPS en su camino hacia el teléfono.

El enfoque que hemos adoptado para filtrar la ubicación es usar lo que se conoce como emparejamiento de mapas (map matching). Esto supone que lo más probable es que el usuario se esté desplazando a lo largo de un camino o carretera mapeados; usamos el término "Vía" (Way) para abarcar todas las carreteras, pistas y caminos. El emparejamiento de mapas observa por dónde ha estado el usuario y, usando la dirección del movimiento junto con los datos de mapas locales, elige la ubicación más probable en una Vía. Este enfoque no solo tiene en cuenta los errores del GPS, sino también los errores en los datos del mapa. No todas las Vías están mapeadas con precisión, por lo que también tienen errores. Para determinar cuál es la Vía más probable en la que se encuentra el usuario, el algoritmo considera:
* Lo cerca que está una Vía de la ubicación del GPS y de las ubicaciones del GPS anteriores
* La dirección de desplazamiento: ¿se mueven en la misma dirección que la Vía?
* Si es posible llegar desde la última ubicación emparejada con el mapa a la nueva ubicación a través de la red de Vías. Esto es necesario para descartar el cambio entre Vías que en realidad no están conectadas, por ejemplo, una que pasa por encima de otra por un puente, o por debajo de ella por un túnel.
El emparejamiento de mapas puede decidir que no hay Vías cercanas, o que no está seguro de en cuál se encuentra el usuario y, en ese caso, simplemente espera a la siguiente ubicación del GPS y vuelve a intentarlo hasta que esté seguro.

### Dirección
Hay varias direcciones que rastreamos en el software:

1. La dirección en la que apunta el teléfono. Usamos esta cuando el teléfono está desbloqueado y la aplicación está en uso, pero también cuando el teléfono está bloqueado siempre que se sostenga plano con la pantalla apuntando al cielo. Es útil tener esto en cuenta al guardar el teléfono en el bolso. Si se coloca plano en el fondo de un bolso que se sostiene en vertical, la aplicación usaría la dirección aleatoria hacia la que apunta el bolso.
1. La dirección en la que se desplaza el teléfono.
1. La dirección de los auriculares con seguimiento de la cabeza. Actualmente no la usamos, aunque la aplicación de iOS sí la admitía. Tenemos la tecnología preparada para añadirla en el futuro.

Cuando el teléfono está bloqueado y dentro de un bolso, la aplicación usará la dirección de desplazamiento. Sin embargo, si el usuario no se mueve, no hay ninguna dirección disponible. Cuando esto ocurre, las señales de audio se vuelven más silenciosas para indicar que no es posible conocer la dirección actual de la señal: el usuario podría estar girando sin cambiar de ubicación.

Para algunos usos de la dirección en la aplicación, la dirección se "ajusta" a la dirección de la Vía emparejada con el mapa, de modo que si el usuario camina aproximadamente en la dirección de la Vía, se supone que la dirección real de la Vía es correcta y se usa en esos cálculos.

### Conclusión
Aunque a primera vista las señales de audio son sencillas, el uso del emparejamiento de mapas para intentar eliminar los errores de ubicación y dirección introduce una buena cantidad de complejidad.

## Datos del mapa
Los datos del mapa que usa la aplicación se originan casi todos en el proyecto OpenStreetMap. Ejecutamos un servidor que contiene un mapa del mundo entero en múltiples niveles de zoom. Cada nivel de zoom se divide en teselas. El nivel de zoom 0 contiene 1 tesela, el nivel 1 contiene 4 teselas, el nivel 2 contiene 16 teselas y así sucesivamente hasta el nivel 14, que contiene alrededor de 268 millones de teselas para cubrir el planeta. Cada tesela contiene múltiples capas y cada capa tiene puntos, líneas y polígonos que se pueden dibujar para crear un mapa gráfico. Ese mapa gráfico es lo que se le muestra al usuario en la interfaz gráfica de la aplicación. Cada punto, línea y polígono tiene metadatos que describen qué es. Esto procede en su mayor parte directamente de los datos de OpenStreetMap, por lo que una línea podría ser un `footway` que es un `sidewalk` o una `road` que es `minor`

Los datos se convierten en el mapa gráfico mediante un "estilo" que tiene reglas sobre cómo dibujar los diferentes puntos, líneas y polígonos de cada capa, por ejemplo, cómo dibujar un camino, cómo dibujar un bosque, cómo dibujar una parada de autobús. Las reglas pueden variar según el nivel de zoom, por lo que a medida que amplía, cada vez más puntos y líneas se hacen visibles que no lo son al alejar, por ejemplo, paradas de autobús y caminos.

Al alterar el estilo podemos cambiar el aspecto del mapa de la interfaz gráfica, que es de donde procede el "mapa accesible" que estamos probando. Su objetivo es tener mayor contraste y líneas y texto más marcados. El estilo está integrado en la aplicación, por lo que no tenemos que cambiar el mapa del servidor para cambiar su aspecto.

Pero ¿cómo usamos los datos del mapa para el audio?

### Uso de los datos del mapa para el audio
Actualmente usamos una cantidad relativamente pequeña de los datos de mapas para generar la interfaz de usuario de audio. Casi toda la interfaz de audio usa solo las teselas del nivel de zoom máximo. La aplicación une una cuadrícula de 2 por 2 teselas alrededor de donde se encuentra el usuario y luego observa solo algunas de las capas:

* `transportation` - para todos los tipos de Vías, incluidas carreteras, caminos, ferrocarriles y vías de tranvía.
* `poi` - puntos de interés, por ejemplo, comercios, centros deportivos, bancos, buzones, paradas de autobús, etc.
* `building` - este es para los `poi` que están mapeados como algo más que un simple punto, por ejemplo, grandes supermercados o ayuntamientos.

Une las líneas y los polígonos a través de los límites de las teselas y convierte todas las Vías en segmentos de Vía conectados y cruces. Esto es importante porque nos permite buscar a lo largo de una Vía para averiguar a dónde podemos llegar.

Todos los datos analizados también se ponen en un formato fácil de buscar para que la aplicación pueda encontrar fácilmente qué elementos del mapa están cerca. En este punto, los datos se clasifican en categorías. Las categorías actuales son:

* Carreteras
* Carreteras y caminos (todas las Vías)
* Cruces: los puntos en los que se cruzan las Vías
* Entradas: son puntos de un edificio que se han marcado como entrada.
* Pasos de peatones: cruces de carretera
* POI: todos los puntos de interés
* Paradas de transporte: paradas de autobús, estaciones de ferrocarril, paradas de tranvía, etc.
* Subcategorías de POI:
  * Información
  * Objeto
  * Lugar
  * Punto de referencia
  * Movilidad
  * Seguridad
* Asentamientos y subcategorías (consulte la siguiente sección)
  * Ciudades
  * Pueblos
  * Aldeas
  * Caseríos

 Con esto en marcha, para cualquier ubicación la aplicación puede entonces encontrar fácilmente

 * "Todas las paradas de transporte dentro de 50 m" o
 * "El cruce más cercano delante de mí" o
 * "El caserío, aldea, pueblo o ciudad más cercano"
  
Con esto en marcha, crear los avisos de audio es solo cuestión de consultar los datos según la ubicación y la dirección actuales. A medida que el usuario se mueve por una cuadrícula de teselas, la actualiza para que quede centrada en torno a la ubicación actual.

### Más datos
Uno de los problemas con nuestra cuadrícula de datos de mapas muy local es que significa que solo podemos "ver" como máximo alrededor de 1 km en cualquier dirección. Eso está bien para cuando describimos lo que tenemos delante, pero a veces nos gustaría dar más contexto. El principal ejemplo de esto es cuando se usa la aplicación y el usuario no está caminando.

Cuando la aplicación detecta que el usuario se desplaza a más de 5 metros por segundo, cambia cómo describe el mundo. En lugar de avisar de cada cruce y POI, avisa con menos frecuencia y solo de las carreteras cercanas. El problema con esto es que conocer el nombre de una carretera no es muy útil si no se sabe en qué pueblo está.

Para intentar abordar esto, ahora también analizamos los datos del mapa en un nivel de zoom inferior y extraemos datos de la capa `place`. Esta contiene los nombres de pueblos, ciudades, barrios, aldeas, etc. Un problema al mapear las cosas es que no siempre hay un límite obvio entre estos lugares. OpenStreetMap a veces tiene los límites de las ciudades en su base de datos, pero incluso cuando es así, para cuando llega a nuestro mapa en teselas esa información suele perderse. Lo que sí tenemos es la ubicación en la que se dibujan los nombres de los lugares en el mapa. Estos se categorizan y luego la aplicación encontrará el caserío, aldea, pueblo o ciudad más cercano al usuario y lo comunicará.

Para muchas ciudades, el nombre real de la ciudad nunca se anunciará, porque la mayoría de las ciudades se dividen en divisiones más pequeñas como barrios, pero esas sí dan un contexto adicional muy útil. Solo recuerde que el hecho de que la aplicación comunique que se encuentra cerca de una calle en un barrio concreto solo significa que la etiqueta de ese barrio es el punto más cercano, y podría ser incorrecta o incluso estar al otro lado de un río.

### Más contexto
Cuanto más contexto se pueda añadir en las descripciones, mejor, siempre que se mantenga conciso y predecible. Uno de los problemas que vimos al describir cruces es que a menudo había Vías "sin nombre" involucradas. Son Vías que no tienen nombre. En los datos de mapas, estas podrían ser solo una pista, un camino o una vía de servicio, pero sin más contexto no son muy útiles en las descripciones de texto. Por suerte, podemos hacerlo mejor, así que lo que hace la aplicación es que, siempre que está a punto de anunciar una Vía sin nombre, comprueba si puede averiguar algo más de contexto para ella.

* **¿Es una acera?**
Muchas zonas de OpenStreetMap ahora tienen las aceras mapeadas por separado de las carreteras. Estas suelen estar etiquetadas como `sidewalk`, pero normalmente no indican cuál es la carretera de la que son la acera.

    Cuando la aplicación se encuentra con una acera sin nombre, busca una carretera que cree que discurre junto a ella y la usa para nombrar la acera. Esto resulta ser muy importante para nuestros avisos. En lugar de anunciar cada cruce de aceras, a medida que avanzamos por una acera los avisos se hacen como si nos moviéramos por la carretera asociada. En lugar de *"Viajando al oeste por sendero"* tenemos *"Viajando al oeste por Moor Road"*. El usuario está en la acera mapeada, pero la descripción tiene más sentido.

* **¿Termina en una Vía con nombre?**
Muy a menudo hay caminos peatonales que unen dos carreteras. Al observar ambos extremos del camino podemos añadir fácilmente ese contexto, de modo que en una dirección podría ser *"Camino a Moor Road"* y al acercarse desde el otro extremo podría ser *"Camino a Roselea Drive"*. Esto solo se hace cuando el camino no se bifurca; si se divide en dos caminos sin nombre, no intentamos añadir este contexto.

* **¿Termina cerca de un Marcador?**
Si una Vía sin nombre empieza o termina cerca de un Marcador, este se usa para describirla, por ejemplo, *"Camino al cruce del árbol grande"*. El usuario puede añadir Marcadores donde quiera, y al añadir Marcadores a lo largo de las redes de caminos puede añadir contexto a toda una ruta.

* **¿Entra o sale de un POI?**
Si una Vía sin nombre empieza fuera de un POI y termina dentro de él (o viceversa), podemos añadir ese contexto, por ejemplo, *"Pista a Lennox Park"*. 

* **¿Termina cerca de una Entrada?**
Si una Vía sin nombre empieza o termina más cerca de una Entrada, podemos añadir ese contexto, por ejemplo, *"Vía de servicio a Best Buy"*.

* **¿Termina cerca de un Punto de referencia o Lugar?**
Si una Vía sin nombre empieza o termina más cerca de un Punto de referencia, también podemos añadir ese contexto, por ejemplo, *"Vía de servicio a la Catedral de St. Giles"*.

* **¿Es un callejón sin salida?**
La aplicación marca como callejón sin salida cualquier Vía sin nombre que no lleve a ninguna parte.

* **¿Pasa por algún tramo de escaleras?**
Si la Vía sin nombre pasa por encima de un puente, a través de un túnel o sube/baja escaleras, esto se anota y se añade al contexto. Esto es independiente del etiquetado del destino, por lo que es posible un contexto como *"Camino por puente a Lennox Park"*.

Estos contextos se añaden en orden, por lo que es posible tener *"Camino a Park Lane por escaleras"* en una dirección y *"Camino a Lennox Park por escaleras"* en la otra. La calle con nombre tiene prioridad al salir, pero el parque se usa al entrar en él.

#### Contexto futuro
Hay varios contextos adicionales que esperamos añadir en el futuro, entre ellos:
* Contexto para Vías que siguen características lineales de agua, por ejemplo, *"Camino junto al río Dee"*
* Contexto para Vías que siguen el borde de masas de agua *"Camino junto al embalse de Milngavie"*
* Contexto para Vías que siguen ferrocarriles, por ejemplo, *"Camino junto al ferrocarril"*. Esto podría incluso incluir el nombre de la línea de ferrocarril
* Añadir contenido adicional a puentes y túneles: qué hay por encima o por debajo de ellos, por ejemplo, *"Camino por puente sobre el ferrocarril a Moor Road"*

## Avisos de audio
Ahora que tenemos los datos del mapa en un formato que podemos usar fácilmente, generar los avisos es realmente bastante sencillo.

### Avisos al caminar
Al caminar, los avisos de audio que pueden producirse son (en orden de prioridad):

1. Describir a qué distancia está el destino actual
1. Describir un cruce próximo
1. Describir los 5 puntos de interés más cercanos

Todos los avisos tienen un límite de frecuencia para que no se repitan con demasiada frecuencia. Si el usuario deja de moverse, los avisos se detendrán y, aun moviéndose, un aviso no se repetirá en cada nueva ubicación del GPS. La frecuencia, igual que en la aplicación de iOS, es:

* Cada 60 segundos para el destino actual
* Cada 30 segundos para un cruce próximo
* Cada 60 segundos para un punto de interés

Los avisos se pueden filtrar a través del menú de ajustes, y ciertamente hay margen para ampliar este comportamiento.

### Avisos al desplazarse más rápido
Al desplazarse a más de 5 metros por segundo, el aviso al destino actual sigue produciéndose, pero los avisos de cruces y puntos de interés se sustituyen por un aviso que describe aproximadamente dónde se encuentra el usuario. Esto da una parada de transporte cercana, un punto de interés que nos contiene, por ejemplo, dentro de un parque grande, o una carretera y un asentamiento cercanos. Estos usan los datos descritos anteriormente, y hay margen evidente para permitir la personalización de esto en el futuro.

## Marcadores y rutas
En su mayor parte, los marcadores y las rutas son simplemente una funcionalidad de la interfaz de usuario que no depende ni del GPS ni realmente de los datos del mapa. Los marcadores son ubicaciones con nombre que el usuario quiere almacenar, y las rutas son una lista ordenada de esos marcadores. La interfaz de usuario para crear ambos está tomada directamente de la versión de iOS.

### Reproducción de rutas
La reproducción de rutas es donde las rutas cobran vida. Cuando se reproduce una ruta, se crea una señal de audio en el primer marcador de la ruta. Una vez que el usuario se acerca a ese marcador, la ruta mueve automáticamente la señal de audio al siguiente marcador de la ruta. Si no hay más marcadores, la reproducción de la ruta finaliza.

## Conclusión
Esperamos que esto haya dado una idea de cómo funciona la aplicación. La aplicación está siempre en desarrollo basándose en los comentarios de los usuarios, así que póngase en contacto si hay algo que crea que se podría añadir.
