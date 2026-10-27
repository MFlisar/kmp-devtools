require "json"
require "yaml"

def mapping(config, key)
  value = config.fetch(key, {})
  value = {} if value.nil?
  abort "#{key} must be a mapping." unless value.is_a?(Hash)
  value
end

def config_value(value)
  unless value.nil? || value.is_a?(String) || value.is_a?(Numeric)
    abort "Config values written to workflow outputs must be scalar values."
  end
  result = value.to_s.strip
  abort "Config values written to workflow outputs must be single-line values." if result.match?(/[\r\n]/)
  result
end

config = YAML.safe_load_file(ENV.fetch("CONFIG_FILE"))
abort "The config file must contain a mapping." unless config.is_a?(Hash)

publish = mapping(config, "publish")
project = mapping(config, "project")
general = mapping(config, "config")
publish_github = mapping(publish, "github")

publish_target = config_value(publish["target"])
unless %w[none maven-central github].include?(publish_target)
  abort "publish.target must be one of: none, maven-central, github."
end

publish_enabled = publish_target != "none"
maven_central_enabled = publish_target == "maven-central"
github_enabled = publish_target == "github"
java_version = config_value(general["java-version"])
abort "config.java-version must be set." if java_version.empty?

page_title = config_value(project["repo-name"])
abort "project.repo-name must be set." if page_title.empty?

target_repository = config_value(publish_github["repository"])
if target_repository.empty?
  target_owner = config_value(publish_github["user"])
  target_repo = config_value(publish_github["repo"])
  target_repository = "#{target_owner}/#{target_repo}" unless target_owner.empty? || target_repo.empty?
end
target_branch = config_value(publish_github["branch"])
target_path = config_value(publish_github["path"])

if github_enabled
  unless target_repository.match?(/\A[[:alnum:]_.-]+\/[[:alnum:]_.-]+\z/)
    abort "publish.github.repository or publish.github.user and publish.github.repo must identify owner/repository."
  end
  abort "publish.github.branch must be set when GitHub publishing is enabled." if target_branch.empty?
  unless target_path.empty? || (!target_path.include?("\\") && target_path.split("/").all? { |part| !part.empty? && part != "." && part != ".." })
    abort "publish.github.path must be a relative path without empty, '.' or '..' segments."
  end
end

apps = config.fetch("apps", [])
abort "apps must be a list." unless apps.is_a?(Array)
abort "Each entry in apps must be a mapping." unless apps.all? { |app| app.is_a?(Hash) }
demos = apps.select { |app| !app["publish"].nil? }
demo_apps = demos.map do |app|
  main_path = config_value(app["main-path"])
  unless main_path.match?(/\A[A-Za-z0-9._-]+(?:\/[A-Za-z0-9._-]+)*\z/) &&
      main_path.split("/").none? { |part| part == "." || part == ".." }
    abort "apps.main-path must be a relative path without empty, '.' or '..' segments for a published demo."
  end

  target_paths = app["target-paths"]
  unless target_paths.is_a?(Array) && target_paths.all? { |path| path.is_a?(String) }
    abort "apps.target-paths must be a list of strings."
  end

  publish = app["publish"]
  abort "apps.publish must be a mapping." unless publish.is_a?(Hash)
  app_target_path = config_value(publish["target-path"])
  abort "apps.publish.target-path must be included in apps.target-paths." unless target_paths.include?(app_target_path)
  unless app_target_path.match?(/\A[A-Za-z0-9._-]+(?:\/[A-Za-z0-9._-]+)*\z/) &&
      app_target_path.split("/").none? { |part| part == "." || part == ".." }
    abort "apps.publish.target-path must be a relative path without empty, '.' or '..' segments."
  end

  publish_path = config_value(publish["path"])
  unless publish_path.match?(/\A[A-Za-z0-9._-]+(?:\/[A-Za-z0-9._-]+)*\z/) &&
      publish_path.split("/").none? { |part| part == "." || part == ".." }
    abort "apps.publish.path must be a relative path without empty, '.' or '..' segments."
  end

  {
    "name" => config_value(app["name"]),
    "description" => config_value(publish["description"]),
    "publish-path" => publish_path,
    "task" => ":#{app_target_path.split("/").join(":")}:wasmJsBrowserDistribution",
    "html-path" => "#{app_target_path}/build/dist/wasmJs/productionExecutable"
  }
end
abort "apps.name must be set for each published demo." if demo_apps.any? { |app| app["name"].empty? }
publish_paths = demo_apps.map { |app| app["publish-path"] }
abort "apps.publish.path values must be unique." unless publish_paths.uniq.length == publish_paths.length
abort "apps.publish.path must not overlap the reserved api path." if publish_paths.any? { |path| path == "api" || path.start_with?("api/") }
overlapping_publish_paths = publish_paths.combination(2).any? do |path, other|
  path.start_with?("#{other}/") || other.start_with?("#{path}/")
end
abort "apps.publish.path values must not overlap." if overlapping_publish_paths

outputs = {
  "publish-enabled" => publish_enabled,
  "publish-target" => publish_target,
  "maven-central-enabled" => maven_central_enabled,
  "github-enabled" => github_enabled,
  "java-version" => java_version,
  "target-repository" => target_repository,
  "target-branch" => target_branch,
  "target-path" => target_path,
  "page-title" => page_title,
  "demo-enabled" => !demos.empty?,
  "demo-apps-json" => JSON.generate(demo_apps)
}

File.open(ENV.fetch("GITHUB_OUTPUT"), "a") do |output|
  outputs.each { |key, value| output.puts("#{key}=#{value}") }
end
