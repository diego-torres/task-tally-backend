#!/bin/sh
set -e

: "${VAULT_ADDR:=http://vault:8200}"
: "${VAULT_TOKEN:=root}"
: "${OUT_DIR:=/work/bootstrap}"

echo "VAULT_ADDR=$VAULT_ADDR"

echo "⏳ Waiting for Vault to be available..."
i=0
until vault status >/dev/null 2>&1; do
  i=$((i+1))
  if [ "$i" -gt 120 ]; then
    echo "❌ Vault did not become ready in time"
    exit 1
  fi
  sleep 1
done
echo "✅ Vault is up!"

echo "➡️ Ensuring KV v2 mounted at secret/"
if vault secrets list | grep -q '^secret/'; then
  echo "✅ KV already mounted at secret/"
else
  vault secrets enable -path=secret -version=2 kv || true
fi

echo "➡️ Writing tasktally policy"
vault policy write tasktally - <<'EOF'
path "secret/data/tasktally/*" {
  capabilities = ["create","update","read","delete","list"]
}
path "secret/metadata/tasktally/*" {
  capabilities = ["read","delete","list"]
}
EOF
echo "✅ Policy installed"

echo "➡️ Creating app token with tasktally policy (ttl=24h)"
# Use awk to extract the token without jq; more robust than sed
RAW_JSON="$(vault token create -policy=tasktally -ttl=24h -format=json)"
APP_TOKEN="$(printf "%s" "$RAW_JSON" | awk -F '"' '/client_token/ {print $4; exit}')"

if [ -z "$APP_TOKEN" ]; then
  echo "❌ Failed to parse client_token from Vault output; dumping for debug:"
  echo "$RAW_JSON"
  exit 1
fi

mkdir -p "$OUT_DIR"
# Write with a trailing newline to keep editors happy
printf "%s
" "$APP_TOKEN" > "$OUT_DIR/dev-app-token.txt"
chmod 600 "$OUT_DIR/dev-app-token.txt"
echo "✅ Token written to $OUT_DIR/dev-app-token.txt"
echo "🎉 Vault bootstrap complete."
