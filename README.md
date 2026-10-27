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
