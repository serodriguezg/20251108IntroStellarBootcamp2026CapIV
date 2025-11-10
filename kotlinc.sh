#!/bin/bash
set -e

echo "🔍 Actualizando paquetes..."
sudo apt update -y

echo "📦 Instalando dependencias necesarias (Java y unzip)..."
sudo apt install -y openjdk-17-jdk wget unzip

echo "☕ Verificando versión de Java..."
java -version

echo "⬇️ Descargando Kotlin compiler..."
KOTLIN_VERSION="1.9.25"
wget https://github.com/JetBrains/kotlin/releases/download/v${KOTLIN_VERSION}/kotlin-compiler-${KOTLIN_VERSION}.zip -O kotlin.zip

echo "📂 Descomprimiendo Kotlin..."
unzip kotlin.zip -d $HOME
rm kotlin.zip

echo "⚙️ Añadiendo kotlinc al PATH..."
echo "export PATH=\$PATH:\$HOME/kotlinc/bin" >> ~/.bashrc
source ~/.bashrc

echo "✅ Instalación completada. Verificando versión de Kotlin..."
kotlinc -version

echo "🎉 Kotlin ${KOTLIN_VERSION} instalado correctamente."