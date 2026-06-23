#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

ruby -e 'require "yaml"; Psych.parse_file("docs/api/openapi.yaml"); text = File.read("docs/api/openapi.yaml"); abort "openapi field missing" unless text.match?(/^openapi:/); abort "paths field missing" unless text.match?(/^paths:/)'

ROOT_DIR="$ROOT_DIR" ruby <<'RUBY'
root = ENV.fetch("ROOT_DIR")
files = [File.join(root, "API.md"), *Dir[File.join(root, "docs/api/*.md")]]
errors = []

files.each do |file|
  File.read(file).scan(/\[[^\]]+\]\(([^)]+)\)/).flatten.each do |target|
    next if target.match?(%r{\A(?:https?://|#)})

    path = target.split("#", 2).first
    resolved = File.expand_path(path, File.dirname(file))
    errors << "#{file.delete_prefix("#{root}/")}: missing link target #{target}" unless File.exist?(resolved)
  end
end

abort errors.join("\n") unless errors.empty?
RUBY

echo "API docs validation passed"
