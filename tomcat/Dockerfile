FROM tomcat:11-jdk21-temurin

ENTRYPOINT ["catalina.sh"]
CMD ["run"]

COPY ./target/dropbox-lite.war /usr/local/tomcat/webapps/dropbox-lite.war

# EXPOSE 8080 -- not needed when using compose