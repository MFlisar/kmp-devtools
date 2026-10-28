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

## Wiederverwendbare GitHub-Workflows

Die folgenden Workflows werden im Consumer-Projekt auf Job-Ebene über `uses` aufgerufen:

Fertige Consumer-Vorlagen heißen `delegating-*.yml`, liegen unter `files/workflows` und referenzieren Version
`9.0.5`. Kopiere die vier YAML-Dateien nach `.github/workflows` im Library-Projekt.
Pages und Markdown-Dokumentation laufen bei Pushes auf `main` oder `master`;
Publishing und Release Notes beim Veröffentlichen eines Releases.

| Workflow | Zweck | Erforderliche Berechtigungen |
|---|---|---|
| `reuseable-publish.yml` | Veröffentlicht je nach `publish.target` auf Maven Central oder in einem GitHub-Maven-Repository; `none` überspringt Publishing. | `contents: read` |
| `reuseable-update-github-pages.yml` | Baut Dokka und konfigurierte Wasm-Demos und veröffentlicht GitHub Pages. | `contents: read`, `pages: write`, `id-token: write` |
| `reuseable-update-markdown-docs.yml` | Aktualisiert Markdown-Dokumentation, prüft Links und pusht Änderungen. | `contents: write` |
| `reuseable-update-tag-release-notes.yml` | Ergänzt bestehende Release Notes um die sortierte Commit-Liste. | `contents: write` |

Der einzige Input ist `config-file`: ein Pfad relativ zum Root des Consumer-Projekts,
standardmäßig `.config.yml`. Ohne `with` wird dieser Standard verwendet. Projektwerte
wie die Java-Version werden daraus gelesen. Die festen `env`-Werte bleiben im Workflow
sichtbar, sind aber keine aufrufbaren Einstellungen; sie zeigen auch, welche Werte
später bei Bedarf in die Config ausgelagert werden können.

Beim Markdown-Linkcheck werden die GitHub-Pages-Root-URL des Repositorys und alle
unter `apps.publish.url-path` konfigurierten Publish-URLs automatisch ausgeschlossen
(mit optionalem abschließendem Slash, Query oder Fragment). Die Config-Action liefert
diese relativen Pfade als `publish-url-paths-json`; der Markdown-Workflow erzeugt
daraus direkt vor Lychee die URL-Filter. Andere URLs werden weiterhin geprüft.

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

Trigger und optional `run-name` definiert der Consumer. Publishing wird üblicherweise
bei `release: types: [published]` aufgerufen. Für Maven Central benötigt der Publishing-Workflow
`GPG_KEY`, `GPG_PASSWORD`, `MAVEN_CENTRAL_USERNAME` und `MAVEN_CENTRAL_PASSWORD`,
die der Aufrufer explizit über `secrets` oder über `secrets: inherit` weitergibt.
Die Publishing-Vorlage verwendet `secrets: inherit` (innerhalb desselben Benutzers,
derselben Organisation oder desselben Enterprise). Für andere Aufrufer werden
die benötigten Secrets explizit zugeordnet. Die anderen drei Vorlagen benötigen
keine Secret-Weitergabe; `GITHUB_TOKEN` steht automatisch zur Verfügung.
Für `publish.target: github` wird stattdessen `TARGET_REPOSITORY_TOKEN` mit
Schreibzugriff auf das konfigurierte Ziel-Repository benötigt.
Für Release Notes muss bereits ein Release zum aktuellen Tag existieren; bei einem
Tag-Push muss der Consumer dessen Erstellung vor dem Aufruf sicherstellen.
Für GitHub Pages muss das Consumer-Repository für die Bereitstellung über GitHub
Actions eingerichtet sein.

Die Workflows checken das Consumer-Repository aus und verwenden zentral
`MFlisar/kmp-devtools/.github/actions/read-config@main`; eine lokale Kopie der Action
ist nicht erforderlich. Der Config-Pfad wird an diese Action weitergegeben;
Gradle liest weiterhin seine `.config.yml` im Projekt-Root.
`publish.yml` bleibt ein lokaler, nicht wiederverwendbarer Workflow dieses Repositorys.

## Aufbau von `.config.yml`

Die Datei enthält die projektweiten Angaben für Publishing, Module und die Dokumentationsgenerierung. Pfade beziehen sich auf das Projektverzeichnis.

| Abschnitt | Inhalt |
|---|---|
| `publish` | `group-id`, Publishing-Ziel (`none`, `maven-central` oder `github`) und bei GitHub die Angaben `user`, `repo`, `branch` und `path`. |
| `config` | `java-version` sowie `developer` mit Name, E-Mail, Maven-ID und GitHub-Benutzername. |
| `project` | Projekt-Namespace, Repository-Name, Veröffentlichungsjahr (`release`) und Lizenz mit Name und Repository-Pfad. |
| `apps` | Liste der Apps mit `name`, `namespace`, `version-name`, `main-path` und `target-paths`. Der optionale Abschnitt `publish` mit `path`, `description` und `target-path` aktiviert die Veröffentlichung der App als Demo; GitHub Pages unterstützt derzeit `target-path`-Werte, die mit `web` enden. Der README-Abschnitt „Demo“ verlinkt auf `main-path`. |
| `modules` | `primary-artifact-id`, eine Modulliste mit Artifact-ID, Beschreibung und Pfad sowie eine XCFramework-Liste mit Name, Pfad und Targets. |
| `documentation` | Dokumentationsordner, Ordner für Partials und Screenshots sowie optional der Pfad zum Haupt-Screenshot. |
| `settings` | Optionale Einstellungen; `log-dependencies` ist standardmäßig `false`. |

Die obersten Abschnitte `publish`, `config`, `apps`, `project`, `modules` und `documentation` sind erforderlich. `publish.github` wird benötigt, wenn `publish.target: github` gesetzt ist. Bei Modulen sind `plugin` und `internal` optionale Flags; bei `documentation` ist `main-screenshot-path` optional.
