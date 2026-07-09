#!/bin/sh
set -e

KEYSTORE_PATH="config/local/keystore.jks"

if [ -e "${KEYSTORE_PATH}" ]; then
	echo "Found existing keystore."
	echo "Refusing to continue."
	exit 1
fi

if [ -z "${KEYSTORE}" ]; then
	echo "Secret variables are not configured."
	echo "Go to Repository > Settings > Secrets and Variables > Actions"
	echo 'and make sure `KEYSTORE` is configured as a Base64-encoded Java keystore'
	echo 'containing a signing key named `key` with password `keypassword`.'
	exit 1
fi

echo "Saving keystore to '${KEYSTORE_PATH}'"
mkdir -p "$(dirname ${KEYSTORE_PATH})"
echo "${KEYSTORE}" | base64 -d > "${KEYSTORE_PATH}"

echo "Done."
