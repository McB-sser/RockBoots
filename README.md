# RockBoots

RockBoots ist ein Paper-Minecraft-Plugin fuer 1.20.x, das ein spezielles Paar Stiefel hinzufuegt: die `Rock Boots`. Diese Stiefel geben Spielern eine eigene Flug- und Schwebe-Mechanik mit Energieleiste, Upgrade-System und mehreren Spezialeffekten. Das Plugin ist so gebaut, dass sich die Rock Boots wie ein eigenes Werkzeug anfuehlen und nicht einfach nur wie Creative-Flug.

Im Kern ermoeglichen die Rock Boots kontrolliertes Schweben in der Luft. Die Flugzeit ist begrenzt und wird als Energie gespeichert. Zusaetzlich koennen die Stiefel mit passenden Stiefeln und verzauberten Buechern erweitert werden. Je nach Upgrade wird das Fliegen schneller, sicherer oder vielseitiger.

## Kurzfassung

RockBoots ist ein movement-orientiertes Item-Plugin fuer Paper, das aus einem einfachen Paar Stiefel ein ausbaubares Flugwerkzeug macht. Spieler craften die Boots aus Cobblestone, laden sie mit Erfahrung auf und verbessern sie ueber ein eingebautes Upgrade-Menue mit passenden Enchantment-Buechern. Dadurch entstehen kontrolliertes Schweben, ein Glasteppich in der Luft, Gleitphasen bei Energieende und mehr Geschwindigkeit beim Fliegen.

## Was das Plugin macht

Die Rock Boots fuegen ein craftbares Spezial-Item hinzu, das:

- begrenztes Fliegen und Schweben erlaubt
- beim Tragen eine eigene Energie verwaltet
- mit Erfahrungspunkten wieder aufgeladen wird
- ein Upgrade-Menue direkt am Item besitzt
- mehrere Spezialeffekte ueber Verzauberungsbuecher freischaltet
- den Stiefeltyp optisch und funktional auf andere Boots-Arten umstellen kann

## Installation

1. Baue das Plugin mit Maven oder nimm die fertige JAR aus `target/`.
2. Lege die Plugin-Datei in den `plugins/`-Ordner deines Paper-Servers.
3. Starte oder re-starte den Server.

Das Plugin registriert beim Join automatisch das Rezept fuer alle Spieler.

## Crafting

Die Rock Boots werden aus vier Cobblestone hergestellt.

Crafting-Muster:

```text
C C
C C
   
```

`C = Cobblestone`

Das Ergebnis ist ein Paar graue, unzerstoerbare `Rock Boots`.

## Erste Nutzung

Nach dem Craften ziehst du die Rock Boots ganz normal im Stiefel-Slot an. Erst wenn sie angelegt sind, sind die Spezialfunktionen aktiv.

Die Grundversion startet mit:

- `60 Sekunden` maximaler Flug-Energie
- `5 Sekunden` aktueller Start-Energie
- `Unbreakable`
- eigener Lore mit Status und Upgrade-Stufen

## Steuerung im Spiel

Die Bedienung ist absichtlich einfach gehalten:

- `Leertaste / Doppelsprung`: startet das Schweben bzw. den Flug
- `In der Luft bleiben`: verbraucht Energie
- `Shift auf festem Boden`: beendet den Flugmodus
- `Shift + Rechtsklick mit Rock Boots in der Hand`: oeffnet das Upgrade-Menue

Wichtig:

- Das Upgrade-Menue oeffnet sich nur, wenn du die Rock Boots in der Haupthand haeltst und dabei schleichst.
- Ein normaler Rechtsklick ohne Sneaken soll das Vanilla-Verhalten nicht stoeren.
- Wenn keine Energie mehr vorhanden ist, endet der aktive Flug oder geht je nach Upgrade in einen Gleitmodus ueber.

## Energie-System

Die Rock Boots besitzen eine eigene Flug-Energie, die in Sekunden gespeichert wird.

- Solange du aktiv fliegst oder auf dem Glasteppich schwebst, wird `1 Energie pro Sekunde` verbraucht.
- Die Grundversion hat `60 Sekunden` maximale Energie.
- Beim Einsammeln von Erfahrung wird Energie wiederhergestellt.
- Pro XP-Ereignis werden `2 Energie pro erhaltenem XP-Punkt` aufgeladen.
- Die Energie wird nie ueber das aktuelle Maximum hinaus gefuellt.

Die aktuelle Energie wird angezeigt:

- in der Lore des Items
- in einer Actionbar waehrend der Nutzung
- mit Warnsignalen kurz vor dem Leerlaufen

## Upgrade-Menue

Das Plugin besitzt ein eigenes Inventar-Menue mit Vorschau. Du oeffnest es mit `Shift + Rechtsklick`, waehrend du die Rock Boots in der Haupthand haeltst.

Im Menue gibt es feste Slots fuer:

- einen normalen Stiefeltyp
- die Rock-Boots-Vorschau
- `Unbreaking`
- `Frost Walker`
- `Efficiency`
- `Feather Falling`
- `Soul Speed`

So benutzt du das Menue:

1. Nimm die Rock Boots in die Haupthand.
2. Schleiche und mache einen Rechtsklick.
3. Lege optional einen anderen Stiefel in den Typ-Slot, wenn du das Material aendern willst.
4. Lege passende verzauberte Buecher in die dafuer vorgesehenen Slots.
5. Die Vorschau aktualisiert sich automatisch.

Die eingelegten Upgrade-Items werden in den Rock Boots gespeichert. Wenn du das Menue spaeter erneut oeffnest, bleiben die eingesetzten Buecher und der gewaehlte Stiefeltyp am Item erhalten.

## Funktionen im Detail

### 1. Schweben und Fliegen

Mit angelegten Rock Boots kannst du dich ueber die normale Bewegungsphysik hinaus in der Luft halten. Das Plugin aktiviert fuer Spieler gezielt den Flugmodus, ohne dass Creative benoetigt wird. Die Fluggeschwindigkeit ist absichtlich niedriger als normaler Creative-Flug und wird ueber Upgrades erweitert.

### 2. Unbreaking: mehr maximale Flugzeit

Ein `Unbreaking`-Buch erhoeht nicht die Haltbarkeit im Vanilla-Sinn, sondern vergroessert die maximale Energie der Rock Boots.

Die Berechnung ist:

```text
Maximale Energie = 60 * (1 + 3 * Unbreaking-Level + 4 * Frost-Walker-Level)
```

Zusatzregel:

- Das Maximum ist auf `900 Sekunden` begrenzt.

Beispiel:

- ohne Upgrades: `60s`
- mit Unbreaking I: `240s`
- mit Unbreaking III: `600s`

### 3. Frost Walker: Glasteppich unter dem Spieler

`Frost Walker` aktiviert keinen normalen Eis-Effekt. Stattdessen erzeugen die Rock Boots einen temporaeren Glasteppich unter dem Spieler.

Das bedeutet:

- unter dir entstehen waehrend des Schwebens Glasbloecke
- der Teppich bewegt sich mit dir mit
- verlaesst du ihn, werden alte Glasbloecke wieder entfernt
- mit hoeherem Frost-Walker-Level wird der Teppich groesser
- der Effekt kann zum stabilen Schweben und fuer vertikale Bewegung genutzt werden

Zusatzverhalten:

- springst du vom Teppich hoch, wird der Teppich mit nach oben gezogen
- schleichst du auf dem Teppich, wird er kontrolliert nach unten versetzt
- beendest du den Modus, werden die erzeugten Glasbloecke wieder geloescht

### 4. Efficiency: schnellere Fluggeschwindigkeit

`Efficiency` erhoeht die Fluggeschwindigkeit der Rock Boots.

Technisch:

- Basis-Fluggeschwindigkeit der Rock Boots: `0.05`
- pro Efficiency-Level kommen `0.008` dazu
- die Geschwindigkeit wird maximal auf Vanilla-Flugtempo `0.1` begrenzt

Das Upgrade ist sinnvoll, wenn sich die Boots ohne Verstaerkung zu traege anfuehlen.

### 5. Feather Falling: Nachgleiten bei leerer Energie

`Feather Falling` sorgt dafuer, dass du nach dem Ende der Energie nicht sofort abstuerzt. Stattdessen geht der Flug in einen kurzen Gleit- bzw. Sinkflug ueber.

Der Effekt:

- startet nur, wenn du in der Luft warst und die Energie waehrenddessen ausgeht
- haelt den Flug fuer kurze Zeit weiter aktiv
- senkt dich kontrolliert und langsamer nach unten
- reduziert die harte Unterbrechung beim Energieende deutlich

Dauer:

- pro Feather-Falling-Level gibt es `40 Ticks` Gleitzeit
- das entspricht `2 Sekunden` pro Level

### 6. Soul Speed: mehr Tempo beim Sprint-Flug

`Soul Speed` gibt keinen Boden-Buff wie in Vanilla, sondern wirkt nur beim Fliegen waehrend des Sprintens.

Der Effekt:

- solange du in der Luft fliegst und sprintest, bekommst du zusaetzliche Geschwindigkeit
- pro Soul-Speed-Level kommen `0.01` Geschwindigkeit dazu
- auch hier gilt das globale Maximum von `0.1`

Das Upgrade ist vor allem fuer schnelles Vorwaertskommen waehrend laengerer Fluege gedacht.

### 7. Stiefeltyp uebernehmen

Im Upgrade-Menue kannst du einen normalen Stiefel in den Typ-Slot legen. Dadurch wird das Material des Rock-Boots-Items auf diesen Stiefeltyp umgestellt.

Beispiele:

- Leather Boots
- Iron Boots
- Diamond Boots
- Netherite Boots

Dabei gilt:

- das Rock-Boots-Item bleibt weiterhin ein Rock-Boots-Item
- vorhandene Verzauberungen des eingelegten Stiefels werden uebernommen
- die Rock-Boots-Metadaten und Spezialfunktionen bleiben erhalten

## Anzeige und Feedback

Das Plugin gibt dem Spieler staendig Rueckmeldung:

- Actionbar mit Aktivstatus
- Energiebalken fuer die verbleibende Flugzeit
- zusaetzlicher Balken fuer den Sinkflug mit Feather Falling
- Warntoene bei niedriger Energie
- Fluggeraesche waehrend des aktiven Schwebens

Die Anzeige unterscheidet zwischen aktivem und inaktivem Zustand, damit man sofort erkennt, ob die Boots gerade wirklich arbeiten.

## Wichtige Hinweise zur Benutzung

- Die Rock Boots funktionieren nur, wenn sie im Stiefel-Slot getragen werden.
- Das Upgrade-Menue wird ueber die Boots in der Haupthand geoeffnet, nicht ueber Befehle.
- Das Plugin ist fuer Paper `1.20.x` ausgelegt.
- Es gibt aktuell keine eigenen Commands.
- Es gibt aktuell keine konfigurierbare Config-Datei.
