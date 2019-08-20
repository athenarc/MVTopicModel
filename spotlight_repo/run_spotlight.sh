# need dbpedia-spotlight-1.0.0.jar and en.tar.gz for the server and models
java -cp "$(find /home/npittaras/.m2/repository/ -iname "*jaxb*.jar" | tr '\n' ':')" -jar dbpedia-spotlight-1.0.0.jar ./en http://localhost:2222/rest
