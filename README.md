# kmp-devtools

Gradle-Tools für KMP-Projekte. Die Build- und Settings-Plugins lesen ihre gemeinsame Konfiguration aus `.config.yml` im Projektverzeichnis.

## Gradle-Tasks

| Task | Funktion |
|---|---|
| `updateMarkdownFiles` | Aktualisiert README und Markdown-Dokumentation anhand der Konfiguration. |
| `macActions` | Bietet Aktionen für einen Mac an: Projekt synchronisieren, XCFrameworks bauen oder Xcode öffnen. |
| `devTools` | Öffnet ein Menü mit zusätzlichen Werkzeugen. |
| `launch4j` | Erstellt einen Windows-Launcher; wird über `BuildFileUtil.registerLaunch4JTask()` registriert. |
| `extractProguardMap` | Extrahiert die ProGuard-Mapping-Datei aus dem Release-AAB; wird über `BuildFileUtil.registerExtractProguardMapFromAABTask()` registriert und nach `bundleRelease` ausgeführt. |

Im Menü von `devTools` stehen zur Verfügung:

| Werkzeug | Funktion |
|---|---|
| `updateDevTools` (`u`) | Aktualisiert die kmp-devtools-Version in Versionskatalog, `settings.gradle.kts` und GitHub-Workflows. |
| `downloadFiles` (`d`) | Lädt die vorgesehenen GitHub-Workflow-Dateien ins Projekt. |

## Automatische Versionssynchronisierung

Das Settings-Plugin liest `kmpdevtools` aus dem Abschnitt `[versions]` in
`gradle/mflisar.versions.toml`. Diese Version ist die Soll-Version für den literalen
Plugin-Eintrag in `settings.gradle.kts` und die kmp-devtools-Workflow-Referenzen in
`.github/workflows/*.yml` und `*.yaml`.
Bei Abweichungen werden die Dateien aktualisiert und der aktuelle Gradle-Lauf mit
einer entsprechenden Fehlermeldung abgebrochen. Danach muss der Sync beziehungsweise
Build erneut gestartet werden. Sind alle Versionen konsistent, läuft Gradle normal weiter.
Das manuelle Werkzeug `updateDevTools` verwendet dieselbe Update-Logik und aktualisiert
zusätzlich die Soll-Version im Katalog. Die zuvor eingetragene Settings-Plugin-Version
muss noch auflösbar sein, damit die automatische Synchronisierung ausgeführt werden kann.

## Dieses Repository manuell veroeffentlichen

Der lokale Workflow `.github/workflows/publish.yml` wird ausschliesslich ueber
**Run workflow** gestartet und liest keine `.config.yml`. `version` legt Version
und Tag fest; `target` wird explizit als `maven-central` oder `github` ausgewaehlt.
Es gibt keinen `auto`-Modus und keine Ableitung des Ziels aus der Version.
Ein Versionssuffix kennzeichnet lediglich das anschliessende GitHub Release als
Pre-Release. Vor dem Publish werden Version und erforderliche Secrets geprueft;
nach erfolgreichem Publish entstehen Tag und Release mit sortierter Commit-Liste.

Fuer Maven Central werden `GPG_KEY`, `GPG_PASSWORD`, `MAVEN_CENTRAL_USERNAME` und
`MAVEN_CENTRAL_PASSWORD` benoetigt. Fuer `github` ist das Secret
`TARGET_REPOSITORY_TOKEN` mit Schreibzugriff erforderlich. Das Ziel ist fest
ueber die Workflow-Umgebungsvariablen `TARGET_REPOSITORY`
(`MFlisar/maven-repository`), `TARGET_BRANCH` (`main`) und `TARGET_PATH` (`artifacts`)
definiert, nicht ueber Benutzereingaben. Ein leerer `TARGET_PATH` bedeutet
Repository-Root. Alle drei Module
erzeugen bei `PUBLISH_TARGET=github` ein lokales Maven-Repository unter
`build/maven-repo`, das der Workflow ins Ziel-Repository uebertraegt.
Bestehende Versionen bleiben unveraendert; Maven-Metadaten werden zusammengefuehrt.

## Wiederverwendbare GitHub-Workflows

Die folgenden Workflows werden im Consumer-Projekt auf Job-Ebene über `uses` aufgerufen:

Fertige Consumer-Vorlagen heißen `delegating-*.yml`, liegen unter `files/workflows` und referenzieren Version
`9.0.5`. Kopiere die YAML-Dateien nach `.github/workflows` im Library-Projekt.
Pages und Markdown-Dokumentation laufen bei Pushes auf `main` oder `master`;
Publishing wird manuell über „Run workflow“ gestartet und erstellt das Release selbst.

| Workflow | Zweck | Erforderliche Berechtigungen |
|---|---|---|
| `reuseable-publish.yml` | Manuell mit Input `version`: `X.Y.Z` veröffentlicht über `publish.release-target`, `X.Y.Z-suffix` über `publish.test-target` (Maven Central oder GitHub-Maven-Repository); danach Tag + GitHub Release (mit Suffix als Pre-Release) inkl. sortierter Commit-Liste. | `contents: write` |
| `reuseable-update-github-pages.yml` | Baut Dokka und konfigurierte Wasm-Demos und veröffentlicht GitHub Pages. | `contents: read`, `pages: write`, `id-token: write` |
| `reuseable-update-markdown-docs.yml` | Aktualisiert Markdown-Dokumentation, prüft Links und pusht Änderungen. | `contents: write` |

Gemeinsamer Input ist `config-file`: ein Pfad relativ zum Root des Consumer-Projekts,
standardmäßig `.config.yml`. Ohne `with` wird dieser Standard verwendet. Projektwerte
wie die Java-Version werden daraus gelesen. Die festen `env`-Werte bleiben im Workflow
sichtbar, sind aber keine aufrufbaren Einstellungen; sie zeigen auch, welche Werte
später bei Bedarf in die Config ausgelagert werden können.

Beim Markdown-Linkcheck werden die GitHub-Pages-Root-URL des Repositorys und alle
unter `apps.publish.url-path` konfigurierten Publish-URLs automatisch ausgeschlossen
(mit optionalem abschließendem Slash, Query oder Fragment). Die Config-Action liefert
diese relativen Pfade als `publish-url-paths-json`; der Markdown-Workflow erzeugt
daraus direkt vor Lychee die URL-Filter. Andere URLs werden weiterhin geprüft.
Fuer `central.sonatype.com` begrenzt eine im Runner-Temp-Verzeichnis erzeugte
Lychee-Konfiguration die Parallelitaet auf eine Anfrage und den Mindestabstand
auf zwei Sekunden. HTTP 429 bleibt ein Fehler; die Drosselung reduziert lediglich
das Risiko einer Ratenbegrenzung.

```yaml
name: Documentation
on:
  push:
    branches: [main]

permissions:
  contents: write

jobs:
  docs:
    uses: MFlisar/kmp-devtools/.github/workflows/reuseable-update-markdown-docs.yml@main
    with:
      config-file: .config.yml
```

Trigger und optional `run-name` definiert der Consumer. Für Maven Central benötigt der Publishing-Workflow
`GPG_KEY`, `GPG_PASSWORD`, `MAVEN_CENTRAL_USERNAME` und `MAVEN_CENTRAL_PASSWORD`,
die der Aufrufer explizit über `secrets` oder über `secrets: inherit` weitergibt.
Die Publishing-Vorlage verwendet `secrets: inherit` (innerhalb desselben Benutzers,
derselben Organisation oder desselben Enterprise). Für andere Aufrufer werden
die benötigten Secrets explizit zugeordnet. Die anderen Vorlagen benötigen
keine Secret-Weitergabe; `GITHUB_TOKEN` steht automatisch zur Verfügung.
Für das Ziel `github` wird stattdessen `TARGET_REPOSITORY_TOKEN` mit
Schreibzugriff auf das konfigurierte Ziel-Repository benötigt.
Für GitHub Pages muss das Consumer-Repository für die Bereitstellung über GitHub
Actions eingerichtet sein.

### Publishing (manuell)

Ein reusable Workflow kann nicht direkt in der UI gestartet werden; der Consumer
legt dafür einen Workflow mit `workflow_dispatch` an:

```yaml
name: Publish
run-name: "Publish ${{ inputs.version }} (${{ contains(inputs.version, '-') && 'test / pre-release' || 'release' }})"
on:
  workflow_dispatch:
    inputs:
      version:
        description: "Version: X.Y.Z = release (release-target), X.Y.Z-suffix = pre-release (test-target)"
        required: true
        type: string
      target:
        description: "Target: auto = from config (release-target / test-target)"
        required: true
        type: choice
        options: [auto, maven-central, github]
        default: auto

permissions:
  contents: write

jobs:
  publish:
    uses: MFlisar/kmp-devtools/.github/workflows/reuseable-publish.yml@main
    with:
      version: ${{ inputs.version }}
      target: ${{ inputs.target }}
    secrets: inherit
```

Unter *Actions → Publish → Run workflow* werden Branch, `version` und `target` abgefragt.
Der Typ ergibt sich aus der Version: `X.Y.Z` (jede Stelle beliebig viele Ziffern) ist ein
Release über `publish.release-target`, `X.Y.Z-suffix` (z.B. `1.1.0-beta01`) ein
Pre-Release über `publish.test-target`. Mit `target` = `maven-central` oder `github`
wird das konfigurierte Ziel überschrieben (für `github` muss `publish.github` gesetzt sein);
Release/Pre-Release richtet sich weiterhin nach der Version. Der erste Schritt „Show publish plan“ gibt
Modus, Ziel und konkrete Destination im Log, als Notice und in der Job Summary aus.
Vor dem Publishing wird geprüft, dass die Version eines dieser Formate hat,
Tag und Release noch nicht existieren, die Version größer als das letzte Release ist
und das gewählte Ziel nicht `none` ist. Danach werden Tag und GitHub Release `version`
auf dem Commit des gewählten Branches erstellt (Pre-Release bei Suffix) und die
sortierte Commit-Liste seit dem letzten Release-Tag als Release Notes eingetragen.
Die Job Summary des Laufs zeigt außerdem das letzte Release, veröffentlichte
Artefakte und den Link zum Release. Als Outputs stehen `tag`, `release-url` und
`publish-target` zur Verfügung.

Gradle wählt das Ziel über die Umgebungsvariablen `PUBLISH_TYPE` (`release`/`test`) und
optional `PUBLISH_TARGET` (`auto`/`maven-central`/`github`, überschreibt die Config);
ohne diese Variablen (z.B. lokal) wird `publish.release-target` verwendet.

Die Workflows checken das Consumer-Repository aus und verwenden zentral
`MFlisar/kmp-devtools/.github/actions/read-config@main`; eine lokale Kopie der Action
ist nicht erforderlich. Der Config-Pfad wird an diese Action weitergegeben;
Gradle liest weiterhin seine `.config.yml` im Projekt-Root.
`publish.yml` bleibt ein lokaler, nicht wiederverwendbarer Workflow dieses Repositorys.

## Aufbau von `.config.yml`

Die Datei enthält die projektweiten Angaben für Publishing, Module und die Dokumentationsgenerierung. Pfade beziehen sich auf das Projektverzeichnis.

| Abschnitt | Inhalt |
|---|---|
| `publish` | `group-id`, Publishing-Ziele `release-target` und `test-target` (jeweils `none`, `maven-central` oder `github`; `test-target` ist optional, Standard `none`) und bei GitHub die Angaben `user`, `repo`, `branch` und `path`. Die README-Generierung verwendet nur `release-target`. |
| `config` | `java-version` sowie `developer` mit Name, E-Mail, Maven-ID und GitHub-Benutzername. |
| `project` | Projekt-Namespace, Repository-Name, Veröffentlichungsjahr (`release`) und Lizenz mit Name und Repository-Pfad. |
| `apps` | Liste der Apps mit `name`, `namespace`, `version-name`, `main-path` und `target-paths`. Der optionale Abschnitt `publish` mit `path`, `description` und `target-path` aktiviert die Veröffentlichung der App als Demo; GitHub Pages unterstützt derzeit `target-path`-Werte, die mit `web` enden. Der README-Abschnitt „Demo“ verlinkt auf `main-path`. |
| `modules` | `primary-artifact-id`, eine Modulliste mit Artifact-ID, Beschreibung und Pfad sowie eine XCFramework-Liste mit Name, Pfad und Targets. |
| `documentation` | Dokumentationsordner, Ordner für Partials und Screenshots sowie optional der Pfad zum Haupt-Screenshot. |
| `settings` | Optionale Einstellungen; `log-dependencies` ist standardmäßig `false`. |

Die obersten Abschnitte `publish`, `config`, `apps`, `project`, `modules` und `documentation` sind erforderlich. `publish.github` wird benötigt, wenn `release-target` oder `test-target` `github` ist. Das frühere Feld `publish.target` wird nicht mehr unterstützt. Bei Modulen sind `plugin` und `internal` optionale Flags; bei `documentation` ist `main-screenshot-path` optional.
