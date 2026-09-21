---
title: Notas de la versión
layout: page
nav_order: 5
has_toc: false
lang: es
permalink: /release-notes.html
machine-translated: true
---

# Notas de la versión

Soundscape 2.0 es una versión importante y se encuentra actualmente en beta cerrada. El cambio
principal es que Soundscape ya tiene algo útil que decir cuando viajas en coche, autobús o tren, y
no solo cuando vas a pie. También hay mucho trabajo de menor tamaño sobre cómo se describen los
lugares, veinte idiomas nuevos y una larga lista de correcciones.

Las notas de versiones anteriores están en la página
[Notas de la versión 1.x]({{ "/v1.0-release-notes.html" | relative_url }}).

## Novedades de la versión 2.0

* **Avisos mientras viajas en coche, autobús o tren.** Soundscape reconoce cuando te desplazas a
  cierta velocidad y describe tu trayecto en lugar de tu entorno inmediato.
* **Aviso al cruzar cursos de agua y vías férreas.** Ríos, canales, rías y líneas de ferrocarril se
  anuncian al cruzarlos, tanto si vas caminando como si vas en un vehículo.
* **Mejores direcciones y nombres de lugares.** Los lugares que no tienen dirección propia reciben
  ahora la calle y la zona en la que se encuentran, los números de portal se asignan al lado
  correcto de la calle y las paradas de autobús de Gran Bretaña usan sus nombres oficiales.
* **Veinte idiomas nuevos**, hasta un total de 46. Este sitio de documentación también está
  traducido.
* **Despertar al salir.** El modo de reposo ya puede despertar Soundscape cuando abandonas el lugar
  donde lo dejaste en reposo.
* **Distancias más cortas y naturales**, con unidades mayores cuando te desplazas rápido.
* **Una salida más rápida.** *Salir de Soundscape* está ahora en la parte superior del menú
  principal.
* **Mejoras en los mapas sin conexión**, incluida la actualización de un mapa ya descargado y un
  mapa de las regiones disponibles en este sitio.
* **Mucho trabajo de accesibilidad** con TalkBack, sobre todo en las pantallas de introducción.
* **Muchísimas correcciones de bloqueos y estabilidad.**

En la versión 2.0 se han **eliminado** dos cosas: el control por voz y el menú de idioma dentro de
la aplicación. Consulta [Elementos eliminados](#things-that-have-been-removed) más abajo para saber
qué hacer en su lugar.

---

## Con más detalle

### Viajar en coche, autobús o tren

Es la mayor novedad para quienes ya usan la aplicación. Antes, Soundscape tenía muy poco que decir
en cuanto subías a un vehículo: seguía describiendo tu entorno inmediato, lo que a cierta velocidad
significaba un flujo de cosas por las que ya habías pasado.

Ahora Soundscape se da cuenta de que te desplazas más rápido que caminando y cambia lo que te dice.
No hay nada que activar, y vuelve a la normalidad por sí solo en cuanto reduces la velocidad o te
bajas y echas a andar.

Mientras viajas oirás:

* **Dónde estás**, de vez en cuando: la carretera por la que circulas y la dirección que llevas, por
  ejemplo «Circulando hacia el norte por la M8». Las carreteras con número se anuncian por su
  número, y Soundscape no repite la misma carretera cada vez que cambia el nombre de la calle.
* **Pueblos y ciudades** hacia los que te diriges, con la distancia, así como aquellos de los que te
  alejas o por los que simplemente pasas.
* **Enlaces y salidas de autopista** al llegar a ellos.
* **Grandes puntos de referencia** al pasar junto a ellos, como parques, hospitales, estadios y
  centros comerciales.
* **Paradas de autobús, tranvía y tren** al pasar junto a ellas. Soundscape solo menciona las
  paradas de tu lado de la calzada, ya que las del lado contrario sirven al sentido opuesto.
* **Ríos, canales y vías férreas que cruzas.**
* **Túneles**, lo que sobre todo explica por qué Soundscape va a quedarse en silencio: dentro no hay
  señal de GPS.

En un **tren**, Soundscape deduce que vas por una vía férrea y no por una carretera, y te indica por
qué localidades pasas y cuánto has recorrido desde la última estación. Averiguarlo es más difícil de
lo que parece, porque autopistas y vías férreas se construyen a menudo en paralelo durante
kilómetros, así que buena parte del trabajo de esta versión ha consistido en no confundir una con
otra.

Los avisos habituales para caminar (tiendas cercanas, pasos de peatones, etc.) se retienen a
propósito mientras viajas, y las distancias a las que se anuncian las cosas se amplían bastante para
que te enteres antes de haberlas dejado atrás.

### Cruzar cursos de agua y vías férreas

Soundscape ahora te avisa cuando cruzas un río, un canal, una ría, una bahía o una línea de
ferrocarril. Funciona tanto a pie como viajando, y cubre igualmente pasar por debajo y por encima,
de modo que se describen tanto una pasarela como un paso subterráneo.

### Mejores direcciones y nombres de lugares

Se ha trabajado mucho para que Soundscape describa los lugares como lo haría una persona:

* Los lugares sin dirección propia se describen ahora por la calle y la zona en la que están, en
  lugar de quedar imprecisos.
* Los números de portal se asignan al lado correcto de la calle. Antes podía indicarse una dirección
  desde la acera de enfrente.
* La dirección de un lugar ya no repite el nombre del propio lugar.
* Las paradas de autobús de Gran Bretaña usan sus nombres oficiales de transporte público, que suelen
  ser los que aparecen en los horarios y en el poste de la parada.
* Los senderos sin nombre que discurren junto a un río o un canal reciben ahora el nombre del curso
  de agua al que acompañan.
* Los caminos y calles sin nombre se describen con más sentido, y las palabras empleadas están
  correctamente traducidas en lugar de aparecer en inglés.

### Idiomas

En la versión 2.0 se han añadido veinte idiomas nuevos: árabe, bengalí, búlgaro, catalán, croata,
checo, hausa, húngaro, indonesio, coreano, maratí, serbio, eslovaco, esloveno, suajili, tamil,
telugu, tailandés, urdu y vietnamita. Todos estos idiomas están en fase alfa y nos interesa mucho
recibir comentarios sobre su exactitud. En total, Soundscape está ahora disponible en 46 idiomas, y
este sitio de documentación también se ha traducido.

El árabe egipcio se ha integrado en el árabe y el luganda se ha retirado, ya que ninguno de los dos
tenía texto traducido suficiente para resultar útil.

Las traducciones son un trabajo comunitario y agradecemos tu ayuda, o tus correcciones cuando algo
se lea mal. Cualquier cadena puede mejorarse en
<https://hosted.weblate.org/projects/soundscape-android/android-app/>.

### Modo de reposo

El modo de reposo ha ganado el **despertar al salir**. Cuando pones Soundscape en reposo, puedes
pedirle que se despierte en cuanto abandones la zona, algo útil cuando llegas a algún sitio y
quieres silencio hasta que vuelvas a salir.

### Distancias y voz

Las distancias habladas se han acortado y suenan más naturales, y Soundscape cambia ahora a unidades
mayores cuando te desplazas rápido: millas o kilómetros en lugar de una larga cuenta de pies o
metros. Cada idioma decide por sí mismo cómo expresar una distancia fraccionaria, algo que antes
estaba forzado a un patrón propio del inglés.

### Mapas sin conexión

Los mapas sin conexión llegaron con la versión 1.0 y se han ido mejorando:

* Un mapa descargado puede actualizarse ahora en el mismo sitio cuando hay una versión más reciente,
  desde la pantalla de detalles del extracto.
* Los mapas que no se pueden usar (por ejemplo, una descarga dañada) se marcan claramente en lugar
  de fallar en silencio.
* Las descargas son más fiables, y la pantalla muestra qué está ocurriendo mientras se obtiene la
  lista de mapas disponibles, en lugar de un indicador de carga a pantalla completa.
* Una descarga terminada solo aparece como terminada cuando está realmente lista para usarse.
* Hay un [mapa de las regiones disponibles]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  en este sitio.

### Accesibilidad

Se ha trabajado mucho en el comportamiento de los lectores de pantalla, sobre todo en las pantallas
de introducción, donde el foco saltaba antes al lugar equivocado. Otras mejoras incluyen una mejor
lectura de tamaños de archivo y números decimales, indicaciones correctas del tipo «toca dos veces
para...» en idiomas que colocan el verbo al final, y sugerencias con sentido allí donde no había
ninguna.

### Menús y navegación

* **Salir de Soundscape** es ahora el primer elemento del menú principal, en lugar de estar más
  abajo.
* El menú principal ya no deja ver una franja de la pantalla a un lado, que ofrecía a quienes usan
  lector de pantalla una zona adicional confusa donde tocar.
* El gesto de retroceso del sistema ya no se salta un nivel cuando navegas por categorías en Lugares
  cercanos.
* El *tutorial de audio* pasa a llamarse **tutorial guiado**.
* Los ajustes se han ordenado, y *Restablecer valores predeterminados* borra ahora todo
  correctamente.

### Estabilidad

La versión 2.0 incluye una larga lista de correcciones de bloqueos y cuelgues, entre ellos el
bloqueo de la aplicación en la pantalla de inicio, cuelgues al restablecer los ajustes, fallos
cuando un mapa descargado estaba dañado, fallos al abrir los detalles de una ruta desde la pantalla
principal, fallos al cambiar de idioma y varios problemas notificados automáticamente a través de
Play Store. El comportamiento de batería y arranque también se ha hecho más robusto en teléfonos que
cierran de forma agresiva las aplicaciones en segundo plano.

### Elementos eliminados
{: #things-that-have-been-removed }

* **El control por voz** se ha eliminado. Nunca funcionó de forma lo bastante fiable como para
  mantenerlo, y los botones multimedia de los auriculares cubren en gran medida lo mismo: consulta
  [Ayuda sobre el uso de los controles multimedia]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **El menú de idioma dentro de la aplicación** ha desaparecido. Soundscape sigue ahora el idioma
  que tengas configurado en el teléfono, que es lo que la mayoría esperaba. Para cambiarlo, cambia
  el idioma del teléfono o define un idioma por aplicación en sus ajustes, si los ofrece.

## Cómo informarnos de un problema

Si algo no va bien, nos gustaría saberlo. Escribe al Help Desk a
<soundscapeAndroid@scottishtecharmy.support>, o pregunta en Slack si eres miembro de la STA.

Si un aviso fue incorrecto o no se produjo, una grabación de tu trayecto nos ayuda enormemente:
podemos reproducirla y ver exactamente con qué información contaba Soundscape. Tienes las
instrucciones en
[Proporcionar un registro de ubicación para depuración]({{ "/testing/test-instructions.html" | relative_url }}#providing-a-debug-location-trace).

## Una nota sobre el iPhone

Todo lo anterior se refiere a la aplicación de Android, pero conviene saber a dónde fue el resto del
trabajo de esta versión. Soundscape funciona ahora también en iPhone, y ambas aplicaciones se
construyen a partir del mismo código compartido: las mismas pantallas, las mismas expresiones y los
mismos avisos. Así, una novedad como los avisos de viaje descritos arriba llega a las dos a la vez
en lugar de escribirse dos veces. Esa base común explica por qué la versión 2.0 ha tardado lo que ha
tardado, y es lo que debería hacer que las próximas versiones lleguen antes a ambas plataformas. La
aplicación de iPhone está disponible actualmente a través de TestFlight por invitación: pregunta en
Slack si eres miembro de la STA, o escribe al Help Desk.
