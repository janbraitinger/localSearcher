# Basisimage
FROM node:latest

# Arbeitsverzeichnis
WORKDIR /app

# Kopieren der Anwendung und der Daten in das Container-Image
COPY src/webapplication /app
COPY src/index.txt /app/data

# Setzen des Arbeitsverzeichnisses für das Ausführen von Befehlen
WORKDIR /app/webapplication

# Installation der Abhängigkeiten
RUN npm install

# Freigabe des Ports
EXPOSE 3000

# Startbefehl
CMD ["npm", "start"]

# Mounten des /data-Ordners als Volume
VOLUME ["/app/data"]