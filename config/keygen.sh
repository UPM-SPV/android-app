#!/bin/sh
set -e

KEYSTORE_PATH="config/local/keystore.jks"

if [ -e "${KEYSTORE_PATH}" ]; then
	BOLD="\033[1m"
	BLINK="\033[5m"
	RED="\033[1;31m"
	RESET="\033[0m"

	prompt="YES, DELETE MY KEYS"

	echo -e "${BOLD}!!! ${BLINK}${RED}WARNING ${RESET}${BOLD}!!!${RESET}"
	echo ""
	echo "PRIVATE KEYS HAVE ALREADY BEEN GENERATED"
	echo "RUNNING THIS SCRIPT WILL DELETE THEM AND GENERATE NEW ONES"
	echo ""
	echo -en "${BOLD}ARE YOU SURE?${RESET} (type '${BOLD}${prompt}${RESET}' in uppercase) "
	read -r ans

	if [ "${ans}" != "${prompt}" ]; then
		echo "Aborting."
		exit 1
	fi

	rm "${KEYSTORE_PATH}"
fi

mkdir -p $(dirname "${KEYSTORE_PATH}")
keytool -genkey -v -keystore "${KEYSTORE_PATH}" -alias key -keyalg RSA -keysize 4096 -validity 1 -keypass keypassword -storepass keypassword -dname "CN=SPV"

echo "Done."
