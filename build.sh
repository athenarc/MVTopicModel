
trained_model_path="path/to/trained/model"
tomcat_path="tomcat"
tomcat_url="https://www-us.apache.org/dist/tomcat/tomcat-8/v8.5.40/bin/apache-tomcat-8.5.40.tar.gz"
endpoint_name="mvt_infer"

cwd="$(pwd)"

if [ ! -d "${tomcat_path}" ]; then
	mkdir "${tomcat_path}"
	zipped="$(basename ${tomcat_url})"
	echo "Installing tomcat from ${tomcat_url} to ${tomcat_path}"
	wget -q "${tomcat_url}" \
		&& cd "${tomcat_path}" && tar xzf "${cwd}/apache-tomcat-8.5.40.tar.gz" \
		&& mv "apache-tomcat-8.5.40"/* ./ && rmdir "apache-tomcat-8.5.40" \
		&& cd "${cwd}" && rm "${zipped}"
	echo "Done"
fi

# modify restful pom
echo "Setting model path to ${trained_model_path}"
sed -i "s|<pom.model.path>.*|<pom.model.path>${trained_model_path}</pom.model.path>|" MVTopicModelRestAPI/pom.xml

# build and copy the war
echo "Building"
mvn clean package -P local
echo "Registering tomcat war as ${endpoint_name}"
cp MVTopicModelRestAPI/target/MVTopicModelRestAPI.war "${tomcat_path}/webapps/${endpoint_name}.war"

# tomcat
echo "Starting tomcat. Logs @ $(ls -t ${tomcat_path}/logs | head -1)"
"${tomcat_path}"/bin/startup.sh

echo "Endpoint name: ${endpoint_name}. Try http://localhost:8080/${endpoint_name}/hello"
