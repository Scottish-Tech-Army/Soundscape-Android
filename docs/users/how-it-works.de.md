---
title: Wie Soundscape funktioniert
layout: page
parent: Using Soundscape
has_toc: false
lang: de
permalink: /users/how-it-works.html
machine-translated: true
---
# Wie Soundscape funktioniert
Das Ziel dieser Seite ist es, ein allgemeines Verständnis dafür zu vermitteln, wie die Soundscape-App im Inneren funktioniert. Sie müssen dies nicht lesen, um die App zu verwenden, aber es gibt einige Gründe, warum sie geschrieben wurde:

1. Um interessierten Neueinsteigern der App dabei zu helfen, zu verstehen, wo ihre Grenzen liegen
1. Um den Nutzern eine Vorstellung davon zu geben, was mit neuen Funktionen sonst noch möglich sein könnte
1. Um Entwicklern einen Überblick über die Funktion der App zu geben

Es gibt zwei Technologien, die die App ermöglichen: GPS und OpenStreetMap-Daten. Das GPS gibt uns eine gute Vorstellung davon, wo sich das Telefon befindet und wo es gewesen ist. OpenStreetMap-Daten können dann verwendet werden, um herauszufinden, was sich in der Nähe befindet, und wir können sie nutzen, um dies dem Nutzer zu beschreiben.

## Audiobeacons
In den meisten Hinsichten sind diese aus technologischer Sicht die einfachsten Dinge umzusetzen. Vorausgesetzt, wir kennen den Standort des Telefons und eine Richtung für das Telefon, können wir dann das Audio für das Beacon so verändern, dass es klingt, als käme es aus dieser Richtung. Wir verwenden eine Bibliothek von Steam Audio, um die Audio-Positionierung durchzuführen, die kopfbezogene Übertragungsfunktionen (head related transfer functions) verwendet, um die bestklingende Positionierung zu erzielen. Das Einzige, was wir sonst noch tun, ist, das Beacon-Audio so zu ändern, dass je nach Winkel zwischen der Nutzerrichtung und der Beacon-Position ein anderer Ton abgespielt wird. Die Winkel variieren je nach ausgewähltem Beacon (Taktil, Leuchtsignal, Ping usw.), und einige haben eine größere Anzahl von Tönen als andere. Und das sind Audiobeacons auf der einfachsten Ebene.

Die einzige zusätzliche Komplexität ist die Annahme über den Standort des Telefons und die Richtung, in die der Nutzer zeigt. Schauen wir uns diese der Reihe nach an.

### Standort
Der vom GPS zurückgegebene Standort kann einen ziemlich großen Fehler aufweisen, und dies hängt davon ab, wie viel vom Himmel für das GPS des Telefons sichtbar ist und wie viele Bäume und hohe Gebäude das GPS-Signal auf dem Weg zum Telefon reflektieren.

Der Ansatz, den wir zur Filterung des Standorts gewählt haben, ist die Verwendung des sogenannten Map Matching. Dabei wird angenommen, dass sich der Nutzer am wahrscheinlichsten entlang eines kartierten Pfades oder einer Straße bewegt – wir verwenden den Begriff „Way“, um alle Straßen, Wege und Pfade abzudecken. Map Matching betrachtet, wo der Nutzer gewesen ist, und wählt anhand der Bewegungsrichtung zusammen mit lokalen Kartendaten den wahrscheinlichsten Standort auf einem Way aus. Dieser Ansatz berücksichtigt nicht nur Fehler des GPS, sondern auch Fehler in den Kartendaten. Nicht alle Ways sind genau kartiert, und daher weisen auch sie Fehler auf. Um zu bestimmen, welcher der wahrscheinlichste Way ist, auf dem sich der Nutzer befindet, berücksichtigt der Algorithmus: 
* Wie nah ein Way am GPS-Standort und an früheren GPS-Standorten liegt
* Die Bewegungsrichtung – bewegen sie sich in dieselbe Richtung wie der Way
* Ob es möglich ist, vom letzten map-matched Standort über das Netz der Ways zum neuen Standort zu gelangen. Dies ist erforderlich, um das Wechseln zwischen Ways auszuschließen, die nicht tatsächlich verbunden sind, z. B. wenn einer über einen anderen mittels einer Brücke führt oder unter ihm durch einen Tunnel.
Das Map Matching kann entscheiden, dass es keine nahegelegenen Ways gibt oder dass es sich nicht sicher ist, auf welchem sich der Nutzer befindet, und in diesem Fall wartet es einfach auf den nächsten GPS-Standort und versucht es erneut, bis es sich sicher ist.

### Richtung
Es gibt mehrere Richtungen, die wir in der Software verfolgen:

1. Die Richtung, in die das Telefon zeigt. Wir verwenden diese, wenn das Telefon entsperrt ist und die App in Verwendung ist, aber auch, wenn das Telefon gesperrt ist, solange es flach gehalten wird und der Bildschirm zum Himmel zeigt. Es ist nützlich, dies zu bedenken, wenn Sie Ihr Telefon in Ihre Tasche stecken. Wenn es flach auf dem Boden einer aufrecht gehaltenen Tasche liegt, würde die zufällige Richtung, in die Ihre Tasche zeigt, von der App verwendet.
1. Die Richtung, in die sich das Telefon bewegt.
1. Die Richtung von Kopfhörern mit Head-Tracking. Wir verwenden diese derzeit nicht, obwohl die iOS-App sie unterstützte. Wir haben die Technologie vorhanden, um sie in Zukunft hinzuzufügen.

Wenn das Telefon gesperrt ist und sich in einer Tasche befindet, verwendet die App die Bewegungsrichtung. Wenn sich der Nutzer jedoch nicht bewegt, ist keine Richtung verfügbar. Wenn dies geschieht, werden die Audiobeacons leiser, um anzuzeigen, dass es nicht möglich ist, die aktuelle Richtung des Beacons zu kennen – der Nutzer könnte sich drehen, ohne den Standort zu ändern.

Bei einigen Verwendungen der Richtung in der App wird die Richtung an die Richtung des map-matched Way „angepasst“, sodass, wenn der Nutzer ungefähr in Richtung des Way läuft, angenommen wird, dass die tatsächliche Richtung des Way korrekt ist, und sie in diesen Berechnungen verwendet wird.

### Fazit
Obwohl die Audiobeacons auf den ersten Blick unkompliziert sind, führt die Verwendung von Map Matching zur Beseitigung von Standort- und Richtungsfehlern eine erhebliche Komplexität ein.

## Kartendaten
Die von der App verwendeten Kartendaten stammen fast alle vom OpenStreetMap-Projekt. Wir betreiben einen Server, der eine Karte der gesamten Welt in mehreren Zoomstufen enthält. Jede Zoomstufe ist in Kacheln aufgeteilt. Zoomstufe 0 enthält 1 Kachel, Stufe 1 enthält 4 Kacheln, Stufe 2 enthält 16 Kacheln und so weiter bis Stufe 14, die etwa 268 Millionen Kacheln enthält, um den Planeten abzudecken. Jede Kachel enthält mehrere Ebenen, und jede Ebene hat Punkte, Linien und Polygone, die gezeichnet werden können, um eine grafische Karte zu erstellen. Diese grafische Karte ist das, was dem Nutzer in der App-GUI angezeigt wird. Jeder Punkt, jede Linie und jedes Polygon hat Metadaten, die beschreiben, was es ist. Dies stammt größtenteils direkt aus den OpenStreetMap-Daten, sodass eine Linie ein `footway` sein könnte, der ein `sidewalk` ist, oder ein `road`, der ein `minor` ist

Die Daten werden über einen „Stil“ in die grafische Karte umgewandelt, der Regeln dafür hat, wie die verschiedenen Punkte, Linien und Polygone in jeder Ebene gezeichnet werden, z. B. wie man einen Pfad zeichnet, wie man einen Wald zeichnet, wie man eine Bushaltestelle zeichnet. Die Regeln können je nach Zoomstufe variieren, weshalb beim Hineinzoomen immer mehr Punkte und Linien sichtbar werden, die beim Herauszoomen nicht sichtbar sind, z. B. Bushaltestellen und Pfade.

Durch Ändern des Stils können wir verändern, wie die GUI-Karte aussieht, woher die „barrierefreie Karte“ stammt, die wir ausprobieren. Sie zielt darauf ab, einen höheren Kontrast und kräftigere Linien und Texte zu haben. Der Stil ist in die App integriert, sodass wir die Karte auf dem Server nicht ändern müssen, um ihr Aussehen zu ändern.

Aber wie verwenden wir die Kartendaten für Audio?

### Verwenden der Kartendaten für Audio
Wir verwenden derzeit eine relativ geringe Menge der Kartendaten, um die akustische Benutzeroberfläche zu erzeugen. Fast die gesamte Audio-UI verwendet nur die Kacheln auf der maximalen Zoomstufe. Die App fügt ein 2-mal-2-Raster aus Kacheln um den Nutzer herum zusammen und betrachtet dann nur einige der Ebenen:

* `transportation` – für alle Arten von Ways einschließlich Straßen, Pfaden, Eisenbahnen und Straßenbahnen.
* `poi` – Points-of-Interest, z. B. Geschäfte, Sportzentren, Bänke, Briefkästen, Bushaltestellen usw.
* `building` – dies ist für `poi`, die als mehr als nur ein Punkt kartiert sind, z. B. große Supermärkte oder Rathäuser.

Es verbindet Linien und Polygone über die Kachelgrenzen hinweg und wandelt alle Ways in verbundene Way-Segmente und Kreuzungen um. Dies ist wichtig, weil es uns ermöglicht, entlang eines Way zu suchen, um herauszufinden, wohin wir gelangen können.

Alle geparsten Daten werden außerdem in ein leicht durchsuchbares Format gebracht, sodass die App leicht herausfinden kann, welche Merkmale der Karte sich in der Nähe befinden. An diesem Punkt werden die Daten in Kategorien klassifiziert. Die aktuellen Kategorien sind:

* Straßen
* Straßen und Pfade (alle Ways)
* Kreuzungen – die Punkte, an denen sich Ways kreuzen
* Eingänge – dies sind Punkte an einem Gebäude, die als Eingang markiert wurden.
* Überwege – Straßenüberquerungen
* POIs – alle Points-of-Interest
* Haltestellen – Bushaltestellen, Bahnhöfe, Straßenbahnhaltestellen und so weiter.
* Unterkategorien von POIs:
  * Information
  * Objekt
  * Ort
  * Orientierungspunkt
  * Mobilität
  * Sicherheit
* Siedlungen und Unterkategorien (siehe nächster Abschnitt)
  * Städte (Großstädte)
  * Städte
  * Dörfer
  * Weiler

 Mit dieser Grundlage kann die App dann für jeden beliebigen Standort leicht finden

 * „Alle Haltestellen innerhalb von 50 m“ oder
 * „Die nächste Kreuzung vor mir“ oder
 * „Der nächste Weiler, das nächste Dorf, die nächste Stadt oder Großstadt“
  
Mit dieser Grundlage ist das Erstellen der Audio-Hinweise nur eine Frage der Abfrage der Daten auf Basis des aktuellen Standorts und der Richtung. Während sich der Nutzer über ein Kachelraster bewegt, aktualisiert es dieses, sodass es sich um den aktuellen Standort zentriert.

### Mehr Daten
Eines der Probleme mit unserem sehr lokalen Kartendatenraster ist, dass es bedeutet, dass wir höchstens etwa 1 km in jede Richtung „sehen“ können. Das ist in Ordnung, wenn wir beschreiben, was sich vor uns befindet, aber manchmal möchten wir mehr Kontext geben. Das Hauptbeispiel hierfür ist, wenn die App verwendet wird und der Nutzer nicht zu Fuß unterwegs ist.

Wenn die App erkennt, dass sich der Nutzer mit mehr als 5 Metern pro Sekunde fortbewegt, ändert sie, wie sie die Welt beschreibt. Anstatt jede Kreuzung und jeden POI anzusagen, sagt sie seltener und nur nahegelegene Straßen an. Das Problem dabei ist, dass die Kenntnis eines Straßennamens nicht sehr nützlich ist, wenn man nicht weiß, in welcher Stadt sie liegt.

Um dies zu adressieren, parsen wir die Kartendaten nun auch auf einer niedrigeren Zoomstufe und extrahieren Daten aus der `place`-Ebene. Diese enthält die Namen von Städten, Großstädten, Stadtvierteln, Dörfern und so weiter. Ein Problem beim Kartieren von Dingen ist, dass es nicht immer eine offensichtliche Grenze zwischen diesen Orten gibt. OpenStreetMap hat manchmal Stadtgrenzen in seiner Datenbank, aber selbst wenn das der Fall ist, geht diese Information oft verloren, bis sie es bis zu unserer gekachelten Karte schafft. Was wir haben, ist der Standort, an dem die Ortsnamen auf der Karte gezeichnet werden. Diese werden kategorisiert, und dann findet die App den nächstgelegenen Weiler, das nächstgelegene Dorf, die nächstgelegene Stadt oder Großstadt zum Nutzer und meldet diese.

Bei vielen Großstädten wird der tatsächliche Stadtname nie angesagt, weil die meisten Großstädte in kleinere Unterteilungen wie Stadtviertel aufgeteilt sind, aber diese bieten zusätzlichen Kontext und sind sehr nützlich. Denken Sie nur daran, dass, wenn die App meldet, dass Sie sich in der Nähe einer Straße in einem bestimmten Stadtviertel befinden, dies lediglich bedeutet, dass die Beschriftung für dieses Stadtviertel der nächstgelegene Punkt ist, und sie könnte falsch oder sogar jenseits eines Flusses sein.

### Mehr Kontext
Je mehr Kontext in den Beschreibungen hinzugefügt werden kann, desto besser, solange er prägnant und vorhersehbar bleibt. Eines der Probleme, die wir beim Beschreiben von Kreuzungen sahen, war, dass oft „unbenannte“ Ways beteiligt waren. Dies sind Ways, die keinen Namen haben. In den Kartendaten könnten diese nur ein Track, ein Pfad oder eine Zufahrtsstraße sein, aber ohne mehr Kontext ist es in den Textbeschreibungen nicht sehr nützlich. Glücklicherweise können wir es besser machen, sodass die App immer dann, wenn sie im Begriff ist, einen unbenannten Way anzusagen, prüft, ob sie etwas mehr Kontext dafür herausfinden kann.

* **Ist es ein Gehweg?**
Viele Bereiche von OpenStreetMap haben jetzt Gehwege separat von Straßen kartiert. Diese sind normalerweise als `sidewalk` getaggt, aber sie geben normalerweise nicht an, zu welcher Straße sie der Gehweg sind.

    Wenn die App auf einen unbenannten Gehweg trifft, sucht sie nach einer Straße, von der sie annimmt, dass sie daneben verläuft, und verwendet diese, um den Gehweg zu benennen. Dies erweist sich als sehr wichtig für unsere Hinweise. Anstatt jede Gehweg-Kreuzung anzusagen, werden, während wir uns entlang eines Gehwegs bewegen, die Hinweise so gegeben, als ob wir uns entlang der zugehörigen Straße bewegen würden. Anstelle von *„Nach Westen entlang Pfad fahrend“* haben wir *„Nach Westen entlang Moor Road fahrend“*. Der Nutzer befindet sich auf dem kartierten Gehweg, aber die Beschreibung ergibt mehr Sinn.

* **Endet er an einem benannten Way?**
Sehr oft gibt es Fußwege, die zwei Straßen miteinander verbinden. Indem wir beide Enden des Pfades betrachten, können wir diesen Kontext leicht hinzufügen, sodass es in einer Richtung *„Pfad zur Moor Road“* sein könnte und beim Annähern vom anderen Ende *„Pfad zum Roselea Drive“*. Dies geschieht nur dort, wo sich der Pfad nicht teilt; wenn er sich in zwei unbenannte Pfade teilt, versuchen wir nicht, diesen Kontext hinzuzufügen.

* **Endet er in der Nähe einer Markierung?**
Wenn ein unbenannter Way in der Nähe einer Markierung beginnt oder endet, wird diese verwendet, um ihn zu beschreiben, z. B. *„Pfad zur Kreuzung am großen Baum“*. Der Nutzer kann Markierungen setzen, wo immer er möchte, und indem er Markierungen entlang von Pfadnetzen setzt, kann er einer ganzen Route Kontext hinzufügen.

* **Betritt oder verlässt er einen POI?**
Wenn ein unbenannter Way außerhalb eines POI beginnt und in ihm endet (oder umgekehrt), dann können wir diesen Kontext hinzufügen, z. B. *„Track zum Lennox Park“*. 

* **Endet er in der Nähe eines Eingangs?**
Wenn ein unbenannter Way näher an einem Eingang beginnt oder endet, dann können wir diesen Kontext hinzufügen, z. B. *„Zufahrtsstraße zu Best Buy“*.

* **Endet er in der Nähe eines Orientierungspunkts oder Ortes?**
Wenn ein unbenannter Way näher an einem Orientierungspunkt beginnt oder endet, dann können wir auch diesen Kontext hinzufügen, z. B. *„Zufahrtsstraße zur St. Giles Cathedral“*.

* **Ist es eine Sackgasse?**
Die App markiert alle unbenannten Ways, die nirgendwohin führen, als Sackgasse.

* **Führt er an Stufen vorbei?**
Wenn der unbenannte Way über eine Brücke, durch einen Tunnel oder Stufen hinauf/hinab führt, dann wird dies vermerkt und dem Kontext hinzugefügt. Dies ist getrennt vom Ziel-Tagging, sodass Kontext wie *„Pfad über Brücke zum Lennox Park“* möglich ist.

Diese Kontexte werden in Reihenfolge hinzugefügt, sodass es möglich ist, in einer Richtung *„Pfad zur Park Lane über Stufen“* und in der anderen Richtung *„Pfad zum Lennox Park über Stufen“* zu haben. Die benannte Straße erhält beim Verlassen des Parks Priorität, aber der Park wird beim Betreten verwendet.

#### Zukünftiger Kontext
Es gibt verschiedene zusätzliche Kontexte, die wir in Zukunft hinzufügen möchten, darunter:
* Kontext für Ways, die linearen Gewässern folgen, z. B. *„Pfad neben dem River Dee“*
* Kontext für Ways, die dem Rand von Gewässern folgen, *„Pfad neben dem Milngavie-Reservoir“*
* Kontext für Ways, die Eisenbahnen folgen, z. B. *„Pfad neben der Eisenbahn“*. Dies könnte sogar den Namen der Eisenbahnlinie enthalten
* Zusätzlichen Inhalt zu Brücken und Tunneln hinzufügen, worüber oder worunter sie führen, z. B. *„Pfad über Brücke über Eisenbahn zur Moor Road“*

## Audio-Hinweise
Nachdem wir nun die Kartendaten in einem Format haben, das wir leicht verwenden können, ist das Erzeugen von Hinweisen wirklich ziemlich unkompliziert.

### Hinweise beim Gehen
Beim Gehen sind die Audio-Hinweise, die auftreten können (in Reihenfolge der Priorität):

1. Beschreiben, wie weit das aktuelle Ziel entfernt ist
1. Eine bevorstehende Kreuzung beschreiben
1. Die 5 nächstgelegenen Points-of-Interest beschreiben

Alle Hinweise sind ratenbegrenzt, sodass sie sich nicht zu oft wiederholen. Wenn der Nutzer aufhört, sich zu bewegen, stoppen die Hinweise, und selbst während der Bewegung wiederholt sich ein Hinweis nicht bei jedem neuen GPS-Standort. Die Häufigkeit entsprechend der iOS-App ist:

* Alle 60 Sekunden für das aktuelle Ziel
* Alle 30 Sekunden für eine bevorstehende Kreuzung
* Alle 60 Sekunden für einen Point-of-Interest

Hinweise können über das Einstellungsmenü gefiltert werden, und es gibt sicherlich Spielraum, dieses Verhalten zu erweitern.

### Hinweise bei schnellerer Fortbewegung
Wenn man sich mit mehr als 5 Metern pro Sekunde fortbewegt, findet der Hinweis zum aktuellen Ziel weiterhin statt, aber die Hinweise zu Kreuzungen und Points-of-Interest werden durch einen Hinweis ersetzt, der ungefähr beschreibt, wo sich der Nutzer befindet. Dies gibt eine nahegelegene Haltestelle, einen Point-of-Interest, der uns enthält, z. B. innerhalb eines großen Parks, oder eine nahegelegene Straße und Siedlung an. Diese verwenden die zuvor beschriebenen Daten, und es gibt offensichtlich Raum, dies in Zukunft anpassbar zu machen.

## Markierungen und Routen
Größtenteils sind Markierungen und Routen nur eine Funktion der Benutzeroberfläche, die weder auf GPS noch wirklich auf Kartendaten beruht. Markierungen sind benannte Orte, die der Nutzer speichern möchte, und Routen sind eine geordnete Liste dieser Markierungen. Die Benutzeroberfläche zum Erstellen beider wurde direkt aus der iOS-Version übernommen.

### Routenwiedergabe
Die Routenwiedergabe ist der Ort, an dem Routen zum Leben erwachen. Wenn eine Route abgespielt wird, wird ein Audiobeacon bei der ersten Markierung der Route erstellt. Sobald der Nutzer dieser Markierung nahe kommt, verschiebt die Route das Audiobeacon automatisch zur nächsten Markierung der Route. Wenn es keine weiteren Markierungen gibt, endet die Routenwiedergabe.

## Fazit
Hoffentlich hat dies einen Einblick gegeben, wie die App funktioniert. Die App wird ständig auf Basis von Feedback der Nutzer weiterentwickelt, also melden Sie sich bitte, wenn es etwas gibt, von dem Sie denken, dass es hinzugefügt werden könnte.
