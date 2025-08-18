#!/bin/bash

platform='unknown'
unamestr=`uname`
case "$unamestr" in
	Linux)
		platform='linux'
		rootdir="$(dirname $(readlink -f $0))"
	;;
	Darwin)
		platform='mac'
		rootdir="$(cd $(dirname $0); pwd -P)"
	;;
esac

# Build the project before launching the app
if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven (mvn) не найден. Пожалуйста, установите Maven и повторите." >&2
  exit 1
fi

# Быстрая сборка: пропустить тесты и checkstyle
mvn -q -DskipTests -Dcheckstyle.skip=true -f "$rootdir/pom.xml" package || exit 1

case "$platform" in
	mac)
		java -Xdock:name=OpenPnP --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/java.awt.color=ALL-UNNAMED -jar $rootdir/target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
	;;
	linux)
		java $1 --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.desktop/java.awt.color=ALL-UNNAMED -jar $rootdir/target/openpnp-gui-0.0.1-alpha-SNAPSHOT.jar
	;;
esac
