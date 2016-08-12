# Proyecto final ITBA
## Instalación Web-app

Para la instalación de la web-app se debe:

1. Hacer un pull del repositorio.
2. Instalar **Java** y **Apache Maven**.
3. Pararse en el directorio raíz del repositorio.
4. Instalar **Apache Tomcat** en el directorio tomcat.
5. El directorio debe quedar así:
   - root
   - | -- env
   - | -- repo
   - | &nbsp; &nbsp; | -- backend
   - | &nbsp; &nbsp; | -- web
   - | -- tomcat
6. Siempre parado en directorio raíz del repo, ejecutar:
   - `web/mvn clean install`
7. Se generará un archivo **nub3s-1.0** en web/target/
8. Copiar este archivo a la carpeta webapps de Tomcat ejecutando:
   - `cp web/target/nub3s-1.0.war tomcat/webapps/nub3s.war`
9. Iniciar Tomcat ejecutando:
   - `tomcat/startup.sh`
10. Verificar el funcionamiento entrando a [clouds.it.itba.edu.ar](http://clouds.it.itba.edu.ar)
