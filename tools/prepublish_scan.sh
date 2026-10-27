#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Stefan Richter
# SPDX-License-Identifier: MIT
# Scans a git repository before it is published or pushed: commit identities, e-mail addresses,
# forbidden terms (company names, private domains, old project names), home-directory paths,
# secrets and unwanted files - in the whole history or in a range of commits. Read-only.
#
#   prepublish_scan.sh [--range <rev-range>] [--forbid <regex>]... [--allow-email <regex>]... [--repo <dir>]
#
#   --range        commits to scan (default: --all = every branch and tag). Before a push: @{u}..HEAD
#                  Merge commits are scanned with their full diffs (-m).
#   --forbid       extended regex (case-insensitive) that must not appear anywhere; repeatable.
#                  Also read from $SCAN_FORBID (one regex, e.g. 'acme|acme\.example').
#   --allow-email  extended regex of e-mail addresses that are fine; repeatable. Always allowed:
#                  the repo-local user.email, *@users.noreply.github.com, noreply@github.com,
#                  example.com/.org/.net addresses.
#   --repo         repository directory (default: current directory)
#
# Exit status: 0 = nothing found, 1 = findings (review them), 2 = usage error.
set -uo pipefail

RANGE=(--all)
FORBID=()
ALLOW=('@users\.noreply\.github\.com$' '^noreply@github\.com$' '@example\.(com|org|net)$')
REPO=.
[[ -n "${SCAN_FORBID:-}" ]] && FORBID+=("$SCAN_FORBID")
usage() { sed -n '3,18p' "$0" | sed 's/^# \{0,1\}//'; }
need() { [[ $# -ge 2 && -n "$2" && "$2" != --* ]] || { echo "option $1 needs a value" >&2; usage >&2; exit 2; }; }
while (($#)); do
  case "$1" in
    --range) need "$@"; RANGE=("$2"); shift 2 ;;
    --forbid) need "$@"; FORBID+=("$2"); shift 2 ;;
    --allow-email) need "$@"; ALLOW+=("$2"); shift 2 ;;
    --repo) need "$@"; REPO="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "unknown option: $1" >&2; exit 2 ;;
  esac
done
cd "$REPO" || exit 2
git rev-parse --git-dir >/dev/null 2>&1 || { echo "not a git repository: $REPO" >&2; exit 2; }

own=$(git config --local --get user.email || true)
# shellcheck disable=SC2016  # the sed expression is meant literally
[[ -n "$own" ]] && ALLOW+=("^$(printf '%s' "$own" | sed 's/[.[\*^$()+?{|]/\\&/g')\$")
allow_re=$(IFS='|'; echo "${ALLOW[*]}")

found=0
section() { printf '\n== %s\n' "$1"; }
hit() { found=1; printf '%s\n' "$@"; }

section "Repo-local identity"
if [[ -z "$own" ]]; then
  hit "!! no repo-local user.email - set it: git config user.email '<id>+<login>@users.noreply.github.com'"
else
  echo "user.name=$(git config --local --get user.name || echo '?')  user.email=$own"
  [[ "$own" =~ @users\.noreply\.github\.com$ ]] || hit "!! repo-local user.email is not a GitHub noreply address"
fi

section "Commit and tag identities (${RANGE[*]})"
ids=$(git log "${RANGE[@]}" --format='%an <%ae>%n%cn <%ce>' | sort | uniq -c | sort -rn)
echo "$ids"
bad=$(git log "${RANGE[@]}" --format='%ae%n%ce' | sort -u | grep -viE "$allow_re" || true)
[[ -n "$bad" ]] && hit "!! identities with an address that is not allowed:" "$bad"
tags=$(git for-each-ref refs/tags --format='%(taggeremail)' | tr -d '<>' | sort -u | grep -v '^$' | grep -viE "$allow_re" || true)
[[ -n "$tags" ]] && hit "!! annotated tags by:" "$tags"

# Ref names, annotated tag messages and tagger identities (scanned below with the history).
refs_text=$(git for-each-ref --format='%(refname) %(taggername) %(taggeremail)%0a%(contents)' refs/heads refs/tags refs/remotes 2>/dev/null)
# Full history text: messages, both identities and every diff, including merge commits (-m).
history() { git log "${RANGE[@]}" -p -m --format='commit %h %an <%ae> | %cn <%ce>%n%B' 2>/dev/null; printf '%s\n' "$refs_text"; }

section "E-mail addresses in content and messages"
# The local part must not start with "+" or "-" (diff prefixes); "@2x.png" etc. are image names, not mail.
mails=$(history | grep -oiE '[A-Z0-9._%][A-Z0-9._%+-]*@[A-Z0-9.-]+\.[A-Z]{2,}' \
  | grep -viE '@[0-9]x\.(png|jpe?g|webp|gif|svg)$' | sort | uniq -c | sort -rn \
  | awk '{print $2}' | grep -viE "$allow_re" || true)
if [[ -n "$mails" ]]; then
  hit "!! review these addresses (licence texts and the Code of Conduct may be fine):" "$mails"
else
  echo "none besides allowed ones"
fi

if ((${#FORBID[@]})); then
  forbid_re=$(IFS='|'; echo "${FORBID[*]}")
  section "Forbidden terms /$forbid_re/"
  f1=$(history | grep -inE "$forbid_re" | head -40 || true)
  f2=$(git grep -inE "$forbid_re" -- . 2>/dev/null | head -40 || true)
  [[ -n "$f1" ]] && hit "!! in history (diffs, messages, identities, ref names, tag messages):" "$f1"
  [[ -n "$f2" ]] && hit "!! in the current tree:" "$f2"
  [[ -z "$f1$f2" ]] && echo "none"
else
  section "Forbidden terms"
  echo "(none given - pass --forbid 'company|private-domain\\.example' or set SCAN_FORBID)"
fi

section "Home-directory paths and host names"
paths=$(history \
  | grep -oE '(/Users|/home)/[A-Za-z][A-Za-z0-9._-]+/|C:(\\\\|\\|/)Users(\\\\|\\|/)[A-Za-z][A-Za-z0-9._-]+|[A-Za-z0-9-]+\.(local|lan|internal|corp)\b' \
  | grep -vE '^/(Users|home)/(runner|runneradmin|user|you|me|username|example|name|USER|NAME)/' \
  | sort | uniq -c | sort -rn | head -20 || true)
[[ -n "$paths" ]] && hit "!! review (private paths reveal user names):" "$paths" || echo "none"

section "Secrets"
if command -v gitleaks >/dev/null; then
  if [[ "${RANGE[*]}" == "--all" ]]; then
    gitleaks git --no-banner --redact . || found=1
  else
    gitleaks git --no-banner --redact --log-opts="${RANGE[*]}" . || found=1
  fi
else
  echo "(gitleaks not installed - brew install gitleaks; falling back to patterns)"
  sec=$(history | grep -nE \
    'BEGIN ([A-Z]+ )?PRIVATE KEY|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{20,}|gho_[A-Za-z0-9]{36}|xox[abprs]-[A-Za-z0-9-]+|sk-[A-Za-z0-9_-]{20,}|AIza[0-9A-Za-z_-]{35}|-----BEGIN CERTIFICATE' \
    | head -20 || true)
  [[ -n "$sec" ]] && hit "!! possible secrets:" "$sec" || echo "no known secret patterns"
fi

section "Sensitive or unwanted files ever added"
files=$(git log "${RANGE[@]}" --diff-filter=A --name-only --format= | sort -u \
  | grep -iE '\.(p12|pfx|pem|key|p8|keystore|jks|mobileprovision|provisionprofile|env)$|(^|/)\.env(\.|$)|id_(rsa|ed25519)|credentials|secret|(^|/)(\.DS_Store|Thumbs\.db|desktop\.ini)$' || true)
[[ -n "$files" ]] && hit "!! review:" "$files" || echo "none"

section "Large blobs (> 1 MB)"
big=$(git rev-list --objects "${RANGE[@]}" \
  | git cat-file --batch-check='%(objecttype) %(objectsize) %(rest)' \
  | awk '$1=="blob" && $2 > 1000000 {printf "%8.1f MB  %s\n", $2/1048576, $3}' | sort -rn | head -15)
[[ -n "$big" ]] && printf '%s\n' "$big" "(large files are not always wrong - check they belong in the repo)" || echo "none"

section "Tracked files that .gitignore would ignore"
ign=$(git ls-files -ci --exclude-standard 2>/dev/null | head -20)
[[ -n "$ign" ]] && hit "!! tracked but ignored (build output?):" "$ign" || echo "none"

echo
if ((found)); then
  echo "RESULT: findings above - review them before publishing or pushing."
  exit 1
fi
echo "RESULT: nothing found."
