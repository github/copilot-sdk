#!/usr/bin/env bash
# Generates SVG coverage badges from a JaCoCo CSV report.
#
# Usage: generate-coverage-badge.sh [jacoco.csv] [output-dir]
#   jacoco.csv  - Path to JaCoCo CSV report (default: target/site/jacoco-coverage/jacoco.csv)
#   output-dir  - Directory for the badge SVG (default: .github/badges)
set -euo pipefail

CSV="${1:-target/site/jacoco-coverage/jacoco.csv}"
BADGES_DIR="${2:-.github/badges}"
GENERATED_PREFIX="com.github.copilot.sdk.generated"

if [ ! -f "$CSV" ]; then
  echo "⚠️ No JaCoCo CSV report found at $CSV"
  exit 0
fi

calc_totals() {
  local scope=$1
  awk -F',' -v scope="$scope" -v generated_prefix="$GENERATED_PREFIX" '
    NR > 1 {
      is_generated = index($2, generated_prefix) == 1
      if (scope == "overall" ||
          (scope == "generated" && is_generated) ||
          (scope == "handwritten" && !is_generated)) {
        missed += $4
        covered += $5
      }
    }
    END { print missed + 0, covered + 0 }
  ' "$CSV"
}

format_pct() {
  local missed=$1
  local covered=$2
  local total=$((missed + covered))
  if [ "$total" -eq 0 ]; then
    echo "0"
  else
    awk "BEGIN { printf \"%.1f\", ($covered / $total) * 100 }" | sed 's/\.0$//'
  fi
}

pick_color() {
  local pct=$1
  local color="#e05d44"  # red <60
  if   awk "BEGIN{exit!($pct>=100)}"; then color="#4c1"     # bright green
  elif awk "BEGIN{exit!($pct>=90)}";  then color="#97ca00"  # green
  elif awk "BEGIN{exit!($pct>=80)}";  then color="#a4a61d"  # yellow-green
  elif awk "BEGIN{exit!($pct>=70)}";  then color="#dfb317"  # yellow
  elif awk "BEGIN{exit!($pct>=60)}";  then color="#fe7d37"  # orange
  fi
  echo "$color"
}

generate_badge() {
  local label=$1
  local value=$2
  local output=$3
  local pct=${value%\%}
  local color
  color=$(pick_color "$pct")
  local lw=$(( ${#label} * 7 + 12 ))
  local vw=$(( ${#value} * 7 + 16 ))
  local tw=$((lw + vw))

  cat > "$output" <<EOF
<svg xmlns="http://www.w3.org/2000/svg" width="${tw}" height="20">
  <linearGradient id="b" x2="0" y2="100%">
    <stop offset="0" stop-color="#bbb" stop-opacity=".1"/>
    <stop offset="1" stop-opacity=".1"/>
  </linearGradient>
  <mask id="a"><rect width="${tw}" height="20" rx="3" fill="#fff"/></mask>
  <g mask="url(#a)">
    <rect width="${lw}" height="20" fill="#555"/>
    <rect x="${lw}" width="${vw}" height="20" fill="${color}"/>
    <rect width="${tw}" height="20" fill="url(#b)"/>
  </g>
  <g fill="#fff" text-anchor="middle" font-family="DejaVu Sans,Verdana,Geneva,sans-serif" font-size="11">
    <text x="$((lw/2))" y="15" fill="#010101" fill-opacity=".3">${label}</text>
    <text x="$((lw/2))" y="14">${label}</text>
    <text x="$((lw + vw/2))" y="15" fill="#010101" fill-opacity=".3">${value}</text>
    <text x="$((lw + vw/2))" y="14">${value}</text>
  </g>
</svg>
EOF
}

mkdir -p "$BADGES_DIR"

read -r overall_missed overall_covered <<< "$(calc_totals overall)"
read -r handwritten_missed handwritten_covered <<< "$(calc_totals handwritten)"
read -r generated_missed generated_covered <<< "$(calc_totals generated)"

overall_pct=$(format_pct "$overall_missed" "$overall_covered")
handwritten_pct=$(format_pct "$handwritten_missed" "$handwritten_covered")
generated_pct=$(format_pct "$generated_missed" "$generated_covered")

echo "Overall coverage: ${overall_pct}%"
echo "Handwritten coverage: ${handwritten_pct}%"
echo "Generated coverage: ${generated_pct}%"

generate_badge "coverage" "${overall_pct}%" "${BADGES_DIR}/jacoco.svg"
generate_badge "coverage handwritten" "${handwritten_pct}%" "${BADGES_DIR}/jacoco-handwritten.svg"
generate_badge "coverage generated" "${generated_pct}%" "${BADGES_DIR}/jacoco-generated.svg"

echo "Badges generated in ${BADGES_DIR}"
