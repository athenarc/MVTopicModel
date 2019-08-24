
# Paths should ne already set at the application properties
# keeping these here in case it needs to be automated more in the future.
trained_model_path="path/to/trained/model"
topics_information_query="queries/getTopicsInformation.sql"
document_topics_information_query="queries/getDocumentsPerTopic.sql"
document_visuzalized_details_query="queries/documentVisualizationInfo.sql"

  
tomcat_path="tomcat"
tomcat_url="https://www-eu.apache.org/dist/tomcat/tomcat-8/v8.5.45/bin/apache-tomcat-8.5.45.tar.gz"
endpoint_name="mvtm_api"
ip_address="$(hostname --ip-address| sed 's/ //')" # the hostname command produces a trailing whitespace

cwd="$(pwd)"

if [ ! -f "${tomcat_path}/bin/startup.sh" ]; then
	rm -rf "${tomcat_path}" && mkdir "${tomcat_path}"
	zipped="$(basename ${tomcat_url})"
	unzipped="$(basename ${zipped} .tar.gz)"
	echo "Installing tomcat from ${tomcat_url} to ${tomcat_path}"
	echo "Fetching..."
	wget -q "${tomcat_url}" 
	
	if [ -z "${zipped}" ] || [ !  -f "${zipped}" ]; then echo "Can't fetch tomcat from the web."; exit 1; fi


	echo "Untarring..."
	cd "${tomcat_path}" && mv "../${zipped}" ./ &&  tar xzf "${zipped}" \
		&& mv "${unzipped}"/* ./ && rm -r "${unzipped}" ${zipped} \
		&& cd "${cwd}"
	echo "Done"
else
	shut="${tomcat_path}/bin/shutdown.sh"
	echo "Shutting down existing server via ${shut} ..."
	bash "${shut}"
fi

# modify restful pom
# echo "Assumes property paths are set"
# sed -i "s|<pom.model.path>.*|<pom.model.path>${trained_model_path}</pom.model.path>|" MVTopicModelRestAPI/pom.xml

# build and copy the war
echo "Building"
mvn -q clean package -D skipTests=True
echo "Registering tomcat war as ${endpoint_name}"
cp MVTopicModelRestAPI/target/mvtm_api.war "${tomcat_path}/webapps/${endpoint_name}.war"

# tomcat
cp "${HOME}/.m2/repository/org/postgresql/postgresql/42.1.1.jre7/postgresql-42.1.1.jre7.jar" "${tomcat_path}/lib/"
echo "Starting tomcat. Logs @ $(ls -t ${tomcat_path}/logs | head -1)"
"${tomcat_path}"/bin/startup.sh
sleep 3

echo "Endpoint name: ${endpoint_name}. Trying \"curl http://${ip_address}:8080/${endpoint_name}/hello\""
curl "http://${ip_address}:8080/${endpoint_name}/hello"
