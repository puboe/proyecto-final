# Proyecto final ITBA
##Instalación Web-app
Para la instalación de la web-app se debe:
1. Hacer un pull del repositorio.
2. Instalar **Java**, **Apache Maven**.
3. Instalar **Apache Tomcat** en el directorio ../tomcat
4. El directorio debe quedar así:
	env
	repo
	-backend
	-web
	tomcat
5. Siempre parado en directorio raiz del repo, ejecutar:
	* web/mvn clean install
6. Se generará un archivo **nub3s-1.0** en **web/target/**
7. Copiar este archivo a la carpeta webapps de Tomcat ejecutando:
	* cp web/target/nub3s-1.0.war tomcat/webapps/nub3s.war
8. Iniciar tomcat ejecutando:
	../tomcat/startup.sh
9. Verificar el funcionamiento entrando a clouds.it.itba.edu.ar
