#!/bin/sh
set -e

CNF_PATH="config/local"
CRT_PATH="${CNF_PATH}/cacert.pem"

if [ -z "${CA_CRT}" ]; then
	echo "Secret variables are not configured."
	echo "Go to Repository > Settings > Secrets and Variables > Actions"
	echo 'and make sure `CA_CRT` is set to a valid PEM certificate.'
	exit 1
fi

echo "Saving CA certificate to '${CRT_PATH}'"
mkdir -p "${CNF_PATH}"
echo "${CA_CRT}" > "${CRT_PATH}"

echo "Done."
