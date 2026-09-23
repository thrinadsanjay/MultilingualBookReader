#!/usr/bin/env bash
# Runs on the reading server after CI has rsync'd backend/ and copied .env.
set -euo pipefail

DEST="${DEST:-/opt/svara}"
cd "$DEST/backend"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is not installed on this host. Install Docker Engine and the compose plugin." >&2
  exit 1
fi

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose)
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose)
else
  echo "docker compose is not installed on this host." >&2
  exit 1
fi

probe() {
  if command -v curl >/dev/null 2>&1; then
    curl -fsS "$1"
  else
    python3 -c "import urllib.request,sys; urllib.request.urlopen(sys.argv[1])" "$1"
  fi
}

"${COMPOSE[@]}" up -d --build
"${COMPOSE[@]}" ps
for _ in $(seq 1 30); do
  if probe http://127.0.0.1:8080/health >/dev/null; then
    echo "svara-api is healthy on 127.0.0.1:8080"
    break
  fi
  sleep 2
done
probe http://127.0.0.1:8080/health
probe http://127.0.0.1:8080/api/v1/ocr/health

if [ "${INSTALL_NGINX:-}" = "true" ]; then
  if [ -z "${BACKEND_PUBLIC_URL:-}" ]; then
    echo "DEPLOY_NGINX=true needs BACKEND_PUBLIC_URL so nginx knows the server_name." >&2
    exit 1
  fi
  host=$(printf '%s' "$BACKEND_PUBLIC_URL" | sed -E 's#^[a-zA-Z][a-zA-Z0-9+.-]*://##' | cut -d/ -f1 | cut -d: -f1)
  if [ -z "$host" ]; then
    echo "BACKEND_PUBLIC_URL did not contain a host." >&2
    exit 1
  fi
  site=/etc/nginx/sites-available/svara
  enabled=/etc/nginx/sites-enabled/svara
  tmp=$(mktemp)
  sed "s/__SVARA_SERVER_NAME__/${host}/g" "$DEST/deploy/nginx-svara.conf" > "$tmp"
  if command -v sudo >/dev/null 2>&1 && sudo -n true 2>/dev/null; then
    sudo cp "$tmp" "$site"
    sudo ln -sfn "$site" "$enabled"
    sudo nginx -t
    sudo systemctl reload nginx
  else
    echo "Install the nginx site yourself:"
    echo "  sudo cp $tmp $site && sudo ln -sfn $site $enabled && sudo nginx -t && sudo systemctl reload nginx"
    echo "Then: sudo certbot --nginx -d $host"
  fi
  rm -f "$tmp"
fi
