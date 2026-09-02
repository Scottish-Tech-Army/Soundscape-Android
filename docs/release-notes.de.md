---
title: Versionshinweise
layout: page
nav_order: 5
has_toc: false
lang: de
permalink: /release-notes.html
machine-translated: true
---

# Versionshinweise

Soundscape 2.0 ist eine große Veröffentlichung und befindet sich derzeit in einer geschlossenen
Beta. Die wichtigste Neuerung ist, dass Soundscape nun auch dann etwas Nützliches zu sagen hat, wenn
Sie mit dem Auto, dem Bus oder der Bahn unterwegs sind, und nicht mehr nur zu Fuß. Hinzu kommen viele
kleinere Verbesserungen bei der Beschreibung von Orten, zwanzig neue Sprachen und eine lange Liste
von Fehlerbehebungen.

Hinweise zu älteren Versionen finden Sie auf der Seite
[Versionshinweise für 1.x]({% link v1.0-release-notes.md %}).

## Was ist neu in 2.0

* **Ansagen während der Fahrt mit Auto, Bus oder Bahn.** Soundscape erkennt, wenn Sie sich mit
  höherer Geschwindigkeit fortbewegen, und beschreibt Ihre Reise statt Ihrer unmittelbaren Umgebung.
* **Hinweise beim Überqueren von Gewässern und Bahnstrecken.** Flüsse, Kanäle, Meeresarme und
  Bahnlinien werden angesagt, wenn Sie sie überqueren – zu Fuß ebenso wie unterwegs.
* **Bessere Adressen und Ortsnamen.** Orte ohne eigene Adresse erhalten nun die Straße und das
  Gebiet, in dem sie liegen, Hausnummern werden der richtigen Straßenseite zugeordnet, und
  Bushaltestellen in Großbritannien verwenden ihre offiziellen Namen.
* **Zwanzig neue Sprachen**, womit es nun insgesamt 46 sind. Auch diese Dokumentationswebsite wurde
  übersetzt.
* **Aufwachen beim Verlassen.** Der Schlafmodus kann Soundscape nun wieder aufwecken, wenn Sie den
  Ort verlassen, an dem Sie ihn in den Schlafmodus versetzt haben.
* **Kürzere, natürlichere Entfernungsangaben**, mit größeren Einheiten, wenn Sie schnell unterwegs
  sind.
* **Ein schnellerer Weg hinaus.** *Soundscape beenden* steht jetzt ganz oben im Hauptmenü.
* **Verbesserungen bei Offline-Karten**, darunter das Aktualisieren einer bereits heruntergeladenen
  Karte und eine Karte der verfügbaren Regionen auf dieser Website.
* **Viel Arbeit an der Barrierefreiheit** mit TalkBack, insbesondere rund um die Einführungsbildschirme.
* **Sehr viele Absturz- und Stabilitätskorrekturen.**

Zwei Dinge wurden in 2.0 **entfernt**: die Sprachsteuerung und das Sprachmenü innerhalb der App.
Unter [Entfernte Funktionen](#things-that-have-been-removed) weiter unten steht, was Sie
stattdessen tun können.

---

## Ausführlicher

### Unterwegs mit Auto, Bus oder Bahn

Dies ist die größte Neuerung für bestehende Nutzerinnen und Nutzer. Bisher hatte Soundscape in einem
Fahrzeug sehr wenig zu sagen – es beschrieb weiterhin die unmittelbare Umgebung, was bei höherer
Geschwindigkeit einen Strom von Dingen bedeutete, an denen Sie längst vorbeigefahren waren.

Soundscape bemerkt nun, wenn Sie schneller als im Gehtempo unterwegs sind, und ändert entsprechend,
was es Ihnen mitteilt. Es gibt nichts einzuschalten, und sobald Sie langsamer werden oder aussteigen
und gehen, kehrt es von selbst zum normalen Verhalten zurück.

Während der Fahrt hören Sie:

* **Wo Sie sind**, in gewissen Abständen – die Straße, auf der Sie sich befinden, und Ihre
  Fahrtrichtung, zum Beispiel „Fahrt nach Norden auf der M8“. Straßen mit einer Nummer werden mit
  ihrer Nummer angesagt, und Soundscape wiederholt dieselbe Straße nicht jedes Mal, wenn sich deren
  Straßenname ändert.
* **Städte und Dörfer**, auf die Sie zufahren, mit Entfernungsangabe, sowie solche, von denen Sie
  sich entfernen oder an denen Sie schlicht vorbeikommen.
* **Autobahnkreuze und -ausfahrten**, sobald Sie sie erreichen.
* **Große Orientierungspunkte**, an denen Sie vorbeikommen, etwa Parks, Krankenhäuser, Stadien und
  Einkaufszentren.
* **Bus-, Straßenbahn- und Bahnhaltestellen**, an denen Sie vorbeikommen. Soundscape nennt nur die
  Haltestellen auf Ihrer Straßenseite, da die auf der Gegenseite der Gegenrichtung dienen.
* **Flüsse, Kanäle und Bahnstrecken, die Sie überqueren.**
* **Tunnel**, was vor allem erklärt, warum Soundscape gleich still wird – darin gibt es kein
  GPS-Signal.

In einem **Zug** erkennt Soundscape, dass Sie sich auf einer Bahnstrecke und nicht auf einer Straße
befinden, und nennt Ihnen die Orte, an denen Sie vorbeikommen, sowie die zurückgelegte Strecke seit
dem letzten Bahnhof. Das ist schwieriger, als es klingt, denn Autobahnen und Bahnstrecken verlaufen
oft kilometerweit nebeneinander. Ein guter Teil der Arbeit in dieser Version floss deshalb darin,
das eine nicht mit dem anderen zu verwechseln.

Die gewöhnlichen Ansagen für Fußgänger – nahe gelegene Geschäfte, Straßenüberquerungen und so weiter
– werden während der Fahrt bewusst zurückgehalten, und die Entfernungen, ab denen etwas angesagt
wird, sind deutlich vergrößert, damit Sie davon erfahren, bevor Sie daran vorbei sind.

### Überqueren von Gewässern und Bahnstrecken

Soundscape sagt Ihnen nun, wenn Sie einen Fluss, einen Kanal, einen Meeresarm, eine Bucht oder eine
Bahnlinie überqueren. Das funktioniert zu Fuß ebenso wie unterwegs und umfasst sowohl das
Darunterhindurch- als auch das Darüberhinweggehen, sodass eine Fußgängerbrücke und eine
Unterführung beide beschrieben werden.

### Bessere Adressen und Ortsnamen

Es wurde viel daran gearbeitet, dass Soundscape Orte so beschreibt, wie ein Mensch es täte:

* Orte ohne eigene Adresse werden nun über die Straße und das Gebiet beschrieben, in dem sie liegen,
  statt vage zu bleiben.
* Hausnummern werden der richtigen Straßenseite zugeordnet. Zuvor konnte eine Adresse vom
  gegenüberliegenden Gehweg gemeldet werden.
* Die Adresse eines Ortes wiederholt nicht mehr den Namen des Ortes selbst.
* Bushaltestellen in Großbritannien verwenden ihre offiziellen Namen aus dem öffentlichen Nahverkehr,
  also in der Regel jene, die im Fahrplan und auf dem Schild an der Haltestelle stehen.
* Unbenannte Fußwege entlang eines Flusses oder Kanals werden nun nach dem Gewässer benannt, dem sie
  folgen.
* Wege und Straßen ohne Namen werden sinnvoller beschrieben, und die dafür verwendeten Wörter sind
  ordentlich übersetzt, statt auf Englisch zu erscheinen.

### Sprachen

In 2.0 sind zwanzig neue Sprachen hinzugekommen: Arabisch, Bengalisch, Bulgarisch, Katalanisch,
Kroatisch, Tschechisch, Hausa, Ungarisch, Indonesisch, Koreanisch, Marathi, Serbisch, Slowakisch,
Slowenisch, Suaheli, Tamil, Telugu, Thailändisch, Urdu und Vietnamesisch. Diese Sprachen befinden
sich alle im Alpha-Stadium, und wir freuen uns über Rückmeldungen zu ihrer Genauigkeit. Insgesamt
ist Soundscape nun in 46 Sprachen verfügbar, und auch diese Dokumentationswebsite wurde übersetzt.

Ägyptisches Arabisch wurde in Arabisch überführt und Luganda zurückgezogen, da beide nicht genug
übersetzten Text hatten, um nützlich zu sein.

Übersetzungen sind Gemeinschaftsarbeit, und wir freuen uns über Ihre Hilfe dabei oder über
Korrekturen, wenn sich etwas schlecht liest. Jede Zeichenkette kann unter
<https://hosted.weblate.org/projects/soundscape-android/android-app/> verbessert werden.

### Schlafmodus

Der Schlafmodus hat **Aufwachen beim Verlassen** erhalten. Wenn Sie Soundscape in den Schlafmodus
versetzen, können Sie es bitten, wieder aufzuwachen, sobald Sie das Gebiet verlassen. Das ist
nützlich, wenn Sie irgendwo ankommen und Ruhe haben möchten, bis Sie wieder aufbrechen.

### Entfernungen und Sprachausgabe

Gesprochene Entfernungen wurden gekürzt und natürlicher gestaltet, und Soundscape wechselt nun zu
größeren Einheiten, wenn Sie schnell unterwegs sind – Meilen oder Kilometer statt einer langen Zahl
in Fuß oder Metern. Jede Sprache entscheidet selbst, wie eine Bruchteilsentfernung gesagt wird; zuvor
war dies in ein englisch geprägtes Muster gezwängt.

### Offline-Karten

Offline-Karten kamen mit 1.0 und wurden seither stetig verbessert:

* Eine heruntergeladene Karte kann nun direkt aktualisiert werden, wenn eine neuere Fassung
  verfügbar ist – über den Detailbildschirm des Kartenausschnitts.
* Karten, die nicht verwendet werden können – etwa ein beschädigter Download –, werden nun deutlich
  gekennzeichnet, statt stillschweigend zu versagen.
* Downloads sind zuverlässiger, und der Bildschirm zeigt an, was gerade geschieht, während die Liste
  der verfügbaren Karten abgerufen wird, statt eines bildschirmfüllenden Ladekreises.
* Ein abgeschlossener Download wird erst dann als abgeschlossen angezeigt, wenn er wirklich
  einsatzbereit ist.
* Es gibt eine [Karte der verfügbaren Regionen]({{ "/users/help-offline-map-extracts.html" | relative_url }})
  auf dieser Website.

### Barrierefreiheit

Es wurde sehr viel am Verhalten der Bildschirmleseprogramme gearbeitet, insbesondere bei den
Einführungsbildschirmen, bei denen der Fokus zuvor an die falsche Stelle sprang. Weitere
Verbesserungen betreffen das bessere Vorlesen von Dateigrößen und Dezimalzahlen, korrekte Hinweise
der Art „Zum Aktivieren doppeltippen“ in Sprachen mit nachgestelltem Verb sowie sinnvolle Hinweise
dort, wo bislang gar keine hinterlegt waren.

### Menüs und Navigation

* **Soundscape beenden** ist nun der erste Eintrag im Hauptmenü statt weiter unten zu stehen.
* Das Hauptmenü zeigt an einer Seite keinen Streifen des Bildschirms mehr, der Nutzenden von
  Bildschirmleseprogrammen einen verwirrenden zusätzlichen Bereich zum Antippen bot.
* Die Zurück-Geste des Systems überspringt keine Ebene mehr, wenn Sie unter „Orte in der Nähe“ durch
  Kategorien blättern.
* Das *Audio-Tutorial* heißt jetzt **Geführtes Tutorial**.
* Die Einstellungen wurden aufgeräumt, und *Auf Standardwerte zurücksetzen* setzt nun wirklich alles
  zurück.

### Stabilität

2.0 enthält eine lange Liste behobener Abstürze und Einfrierungen, darunter das Einfrieren der App
im Startbildschirm, Einfrieren beim Zurücksetzen der Einstellungen, Abstürze bei beschädigten
heruntergeladenen Karten, Abstürze beim Öffnen der Routendetails vom Startbildschirm, Abstürze beim
Wechsel der Sprache sowie mehrere automatisch über den Play Store gemeldete Probleme. Auch das
Verhalten bei Akkuverwaltung und Start wurde auf Telefonen robuster gemacht, die
Hintergrund-Apps aggressiv beenden.

### Entfernte Funktionen
{: #things-that-have-been-removed }

* **Die Sprachsteuerung** wurde entfernt. Sie funktionierte nie zuverlässig genug, um sie
  beizubehalten, und die Medientasten an Kopfhörern decken weitgehend dasselbe ab – siehe
  [Hilfe zur Verwendung der Medientasten]({{ "/users/help-using-media-controls.html" | relative_url }}).
* **Das Sprachmenü innerhalb der App** ist verschwunden. Soundscape folgt nun der Sprache, die Sie
  für Ihr Telefon eingestellt haben – was die meisten Menschen ohnehin erwartet hatten. Zum Ändern
  stellen Sie die Sprache Ihres Telefons um oder legen in dessen Einstellungen eine App-spezifische
  Sprache fest, sofern das angeboten wird.

## Probleme melden

Wenn etwas nicht stimmt, würden wir gern davon erfahren. Schreiben Sie an den Help Desk unter
<soundscapeAndroid@scottishtecharmy.support> oder fragen Sie auf Slack, wenn Sie STA-Mitglied sind.

Wenn eine Ansage falsch war oder ausblieb, hilft uns eine Aufzeichnung Ihrer Fahrt außerordentlich –
wir können sie erneut abspielen und genau sehen, womit Soundscape gearbeitet hat. Eine Anleitung
dazu finden Sie unter
[Bereitstellen einer Standortaufzeichnung zur Fehlersuche]({% link testing/test-instructions.md %}#providing-a-debug-location-trace).

## Ein Hinweis zum iPhone

Alles oben Genannte betrifft die Android-App, aber es lohnt sich zu wissen, wohin die übrige Arbeit
dieser Version geflossen ist. Soundscape läuft nun auch auf dem iPhone, und beide Apps werden aus
demselben gemeinsamen Code erstellt – dieselben Bildschirme, dieselben Formulierungen und dieselben
Ansagen. Eine neue Funktion wie die oben beschriebenen Fahrtansagen erscheint dadurch auf beiden
zugleich, statt zweimal geschrieben zu werden. Diese gemeinsame Grundlage ist der Grund, warum 2.0
so lange gedauert hat, und sie sollte künftige Versionen auf beiden Plattformen schneller verfügbar
machen. Die iPhone-App ist derzeit über TestFlight auf Einladung erhältlich: Fragen Sie auf Slack,
wenn Sie STA-Mitglied sind, oder schreiben Sie an den Help Desk.
