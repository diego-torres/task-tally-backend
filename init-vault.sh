#!/bin/sh
set -e

echo "⏳ Waiting for Vault to be available..."
until vault status >/dev/null 2>&1; do
  sleep 1
done

echo "✅ Vault is up!"

echo "➡️ Enabling KV secrets engine at secret/"
vault secrets enable -path=secret -version=2 kv || true

echo "➡️ Writing tasktally policy"
vault policy write tasktally - <<EOF
path "secret/data/tasktally/*" {
  capabilities = ["create","update","read","delete","list"]
}
path "secret/metadata/tasktally/*" {
  capabilities = ["list"]
}
EOF

echo "➡️ Creating app token with tasktally policy"
APP_TOKEN=$(vault token create -policy="tasktally" -ttl=24h -format=json | jq -r .auth.client_token)

OUT_DIR=${OUT_DIR:-/work/bootstrap}
mkdir -p "$OUT_DIR"
echo "$APP_TOKEN" > "$OUT_DIR/dev-app-token.txt"

echo "✅ Vault initialized. Token written to $OUT_DIR/dev-app-token.txt"
