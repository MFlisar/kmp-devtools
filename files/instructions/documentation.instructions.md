---
applyTo: "documentation/**/*.md,documentation/**/*.partial"
---

# Documentation authoring

Write documentation that helps the intended audience understand, use, configure, and extend this project. Base API and behavior claims on the current source code and project configuration; do not assume conventions or features from other projects.

Every Markdown page must begin with a descriptive first-level heading (`# Page title`). Use clear, simple filenames, preferably lowercase kebab-case. Organize pages around reader needs and use folders where they make related material easier to find; folder names and structures are project-specific.

Keep documentation focused and accurate:

- Explain the practical getting-started path early, including prerequisites, setup, and a minimal first use where relevant.
- Describe important concepts, configuration options, constraints, and behavior such as lifecycle, state, data flow, errors, or performance when relevant to the project.
- Prefer short, runnable examples that match the current APIs and conventions. When an existing source example can be reused with the repository's snippet mechanism, reference it rather than maintaining a duplicate. Do not invent source code solely to create a snippet, and keep snippet names unique if the tooling requires it.
- Format references to functions, types, properties, commands, and configuration keys as code.
- Use headings and links to help readers navigate. Maintain any configured page index or navigation list, and link related pages instead of repeating long explanations.
- Use admonitions only for important prerequisites, caveats, or behavior that is easy to miss, and only with syntax supported by the renderer.

Place images near the content they explain, with useful alt text and paths that work from the page. Do not duplicate images or content that the project already inserts automatically.

If documentation, a README, or other output is generated, edit its configured source files rather than the generated output. After documentation or snippet changes, run the repository's documented generation, formatting, and validation commands when available. Keep generated output in sync and avoid including unrelated working-tree changes.
