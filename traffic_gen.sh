#!/usr/bin/env bash
# Générateur de trafic API PPM — anime les dashboards Grafana (HTTP throughput,
# latence, Hikari) et remplit Loki. Read-only : uniquement des GET + des erreurs
# volontaires (404/401) pour varier les codes HTTP. Aucune écriture en base.
set -u

# Cibles / identifiants surchargeables par l'environnement. Les valeurs par défaut
# sont le compte ADMIN de démo seedé par DataInitializer (aucun secret réel) :
#   PPM_BASE, PPM_USER, PPM_PASS  (ex: PPM_PASS='...' ./traffic_gen.sh 120)
BASE="${PPM_BASE:-http://127.0.0.1:8082}"
EMAIL="${PPM_USER:-m.benali@biat.com.tn}"
PASS="${PPM_PASS:-Biat@2026!}"
DURATION="${1:-300}"   # durée en secondes (défaut 5 min)

login() {
  curl -s -X POST "$BASE/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASS\"}" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}

TOKEN="$(login)"
if [ -z "$TOKEN" ]; then echo "LOGIN FAILED"; exit 1; fi
echo "Login OK, token acquis. Trafic pendant ${DURATION}s..."
AUTH="Authorization: Bearer $TOKEN"

# Endpoints GET valides (200)
GET_OK=(
  "/api/auth/me"
  "/api/projects"
  "/api/projects/1" "/api/projects/2" "/api/projects/3" "/api/projects/4" "/api/projects/5"
  "/api/projects/manager/3"
  "/api/users" "/api/users/1" "/api/users/6" "/api/users/10"
  "/api/portefeuilles" "/api/portefeuilles/1" "/api/portefeuilles/2" "/api/portefeuilles/3"
  "/api/portefeuilles/unassigned-projects"
  "/api/tasks/project/1" "/api/tasks/project/2" "/api/tasks/project/4"
  "/api/dashboard" "/api/dashboard/portfolio"
  "/api/capacity"
  "/api/notifications/me" "/api/notifications/me/unread-count"
  "/api/assignments/me"
  "/api/audit"
  "/api/calendars"
  "/api/dependencies/project/1" "/api/dependencies/project/3"
)
# Endpoints qui renvoient 404 (IDs inexistants) — variété de codes
GET_404=( "/api/projects/9999" "/api/users/9999" "/api/portefeuilles/8888" )

start=$(date +%s)
n=0; ok=0; notfound=0; unauth=0; refresh=$start
while [ $(( $(date +%s) - start )) -lt "$DURATION" ]; do
  # Rafraîchir le token toutes les ~10 min (sécurité, ici inutile sur 5 min)
  if [ $(( $(date +%s) - refresh )) -gt 600 ]; then TOKEN="$(login)"; AUTH="Authorization: Bearer $TOKEN"; refresh=$(date +%s); fi

  # 1 appel valide
  ep="${GET_OK[$RANDOM % ${#GET_OK[@]}]}"
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" "$BASE$ep")
  n=$((n+1)); [ "$code" = "200" ] && ok=$((ok+1))

  # 1 appel sur 6 : une 404
  if [ $((RANDOM % 6)) -eq 0 ]; then
    ep="${GET_404[$RANDOM % ${#GET_404[@]}]}"
    code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" "$BASE$ep")
    n=$((n+1)); [ "$code" = "404" ] && notfound=$((notfound+1))
  fi

  # 1 appel sur 10 : requête non authentifiée (401/403)
  if [ $((RANDOM % 10)) -eq 0 ]; then
    code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/projects")
    n=$((n+1)); unauth=$((unauth+1))
  fi

  # 1 appel sur 15 : un mauvais login (401) pour animer le compteur d'échecs
  if [ $((RANDOM % 15)) -eq 0 ]; then
    curl -s -o /dev/null "$BASE/api/auth/login" -H "Content-Type: application/json" \
      -d '{"email":"ghost@biat.com.tn","password":"wrong"}'
    n=$((n+1))
  fi

  # Rythme : ~5-10 req/s
  sleep 0.12
  if [ $((n % 50)) -eq 0 ]; then
    echo "  [$(( $(date +%s) - start ))s] total=$n  ok200=$ok  404=$notfound  401noauth=$unauth"
  fi
done
echo "TERMINÉ : $n requêtes en ${DURATION}s (200=$ok, 404=$notfound, 401noauth=$unauth)"
