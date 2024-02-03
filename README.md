# Spring AI PoC
Dieses Projekt dient zur Verprobung der Anbindung von ChatGPT und Retrieval-Augmented Generation mithilfe von Spring AI.

Das Projekt basiert auf folgendem GitHub-Repo: https://github.com/rd-1-2022/ai-azure-retrieval-augmented-generation
Bitte beachte, dass das referenzierte Repo eine veraltete Version von SpringAI nutzt, und syntaktisch nicht mit der aktuellen Version kompatibel ist. 


Voraussetzungen zur Nutzung dieses Projekts:
* Auf deinem Rechner ist Docker installiert
* Du verfügst über einen API-Key für ChatGPT (wird weiter unten erklärt)

## Umgebung aufsetzen
### API-Key für ChatGPT erhalten
Um ein API-Token für ChatGPT zu erhalten, benötigst du einen ChatGPT-Account. Das Token kann dann hier abgerufen werden: https://platform.openai.com/api-keys
Die Nutzung der ChatGPT-API ist standardmäßig kostenpflichtig. 
Neue ChatGPT-Accounts erhalten allerdings ein Guthaben von 5€, welches drei Monate gültig ist und ohne Angabe von Zahlungsinformationen genutzt werden kann.
Falls das Kontingent von 5€ aufgebraucht ist, oder dein Konto älter als 3 Monate ist, musst du einen bezahlten Account anlegen.

Die API von ChatGPT verfügt über ein Rate Limiting. Dieses ist hier dokumentiert: https://platform.openai.com/account/limits

Dieses Projekt nutzt gpt-3.5-turbo zur Erzeugung von Antworten, sowie text-embedding-ada-002 zum Erzeugen von Embeddings.
Solange du weniger als 5€ ausgegeben hast (das gilt insbesondere, solange du dein kostenloses Kontingent verwendest), bist du pro Modell auf 3 Anfragen pro Minute beschränkt. 
Dadurch kann die Ausführung deiner Anfragen länger dauern bzw. in Timeouts laufen. 
Um das Projekt schnell auszuprobieren, ohne in Timeouts zu laufen, kannst du die bikes.json in src/main/resources auf drei Einträge kürzen.

### Anwendung starten
Bei dieser Anwendung handelt es sich um eine mit Gradle gebaute Spring Boot Anwendung. Sie kann mit `./gradlew bootRun` gestartet werden.

Soll RAG mit Elasticsearch verwendet werden, muss Elasticsearch über Docker gestartet werden. 
Dies geschieht mithilfe von `docker compose -f elasticsearch/docker-compose.yml up`.

### REST-Requests absetzen
In der Datei rest-api.http sind mehrere REST-Requests dokumentiert. Diese können in IntelliJ per Klick aufgerufen werden

### Milvus Vektordatenbank starten
Damit die Anwendung mit der Vektordatenbank Milvus verwendet werden kann, muss per Docker oder minikube/helm eine Milvus instanz gestartet werden. Anschließend können host und port in der application.yml gesetzt werden.
Durch die Konfiguration von milvus host & port werden alle notwendigen Beans von Spring-AI automatisch initialisiert

## Anfrage an ChatGPT
Um eine Anfrage an ChatGPT zu stellen, kann der Endpunkt `GET localhost:8080/chatgpt?message=MeineAnfrage` aufgerufen werden.
Die Anfrage wird über Spring AI direkt an ChatGPT weitergeleitet und das Ergebnis zurückgegeben.

## Retrieval-Augmented Generation
Bei Retrival-Augmented Generation stellt der Nutzer eine natürlichsprachige Anfrage an das System.
Das System verwendet zur Beantwortung dieser Anfrage seinen eigenen Datenbestand, welche nicht in den Trainingsdaten von ChatGPT enthalten sind.
Dies geschieht folgendermaßen:

Vor der Anfrage des Nutzers wird für jedes Datum ein Embedding generiert, welches dessen Semantik in Form eines Zahlenvektors repräsentiert. 
Die Embeddings werden in einem Vektorspeicher gespeichert.

Stellt der Nutzer eine Anfrage, wird das Embedding dieser Anfrage generiert. 
Daraufhin werden die K Datensätze aus dem Vektorspeicher aufgerufen, deren Embedding dem der Nutzeranfrage am ähnlichsten ist. 
Dadurch werden die K Datensätze abgerufen, welche semantisch am besten zur Anfrage des Nutzers passen. K ist hierbei eine beliebige natürliche Zahl.

Mithilfe der abgerufenen Datensätze wird nun eine Prompt für ChatGPT generiert. 
Sie enthält eine vom Entwickler definierte Anfrage, in die der Inhalt der Datensätze eingefügt wird. 
Diese gibt ChatGPT Kontextionformationen und Anweisungen, wie die Anfrage des Nutzers zu beantworten ist. 
Die natürlichsprachige Anfrage des Nutzers wird daran angehängt.
Mithilfe dieser Prompt wird eine Anfrage an ChatGPT gestellt, und das Ergebnis wird dem Nutzer angezeigt.

### RAG mit einem simplen Vektorspeicher
Um RAG mit einem simplen Vektorspeicher auszuprobieren, muss initial der Endpunkt `POST localhost:8080/rag/simple/ingest` aufgerufen werden.
Dieser list Beispieldaten aus src/main/resources/bikes.json aus, erzeugt Embeddings und speichert sie in einem In-Memory-Vektorspeicher. 
Dies kann wegen des Rate Limitings von ChatGPT einen Moment dauern. Um das Rate Limiting zu umgehen, kann die bikes.json wie oben beschrieben gekürzt werden.
Damit dieser Endpunkt nicht nach jedem Neustart der Anwendung erneut aufgerufen wird, wird der Vektorspeicher unter data/vectorstore.json persistiert. 
Bei einem Neustart wer Anwendung wird dieser Speicher wieder geladen.

Sind die Daten indiziert, kann mithilfe des Endpunkts `GET localhost:8080/rag/simple?message=MeineAnfrage` eine RAG-Anfrage gestellt werden.

### RAG mit Elasticsearch als Vektorspeicher
Der PoC unterstützt auch Elasticsearch als Vektorspeicher. Um Elasticsearch zu nutzen, muss die docker-compose.yml wie oben beschrieben ausgeführt werden. 
Diese startet neben einer Elasticsearch-Instanz auch ein Kibana.
Weil für Elasticsearch noch keine VectorStore-Implementierung für Spring AI existiert, wurde diese im Rahmen dieses PoCs selbst implementiert.

Die Indizierung wird mithilfe des Endpunkts `POST localhost:8080/rag/elasticsearch/ingest` angestoßen. 
Hierbei list die Anwendung die bikes.json aus, erzeugt Embeddings und speichert die Daten anschließend im Index `spring-ai-document`.
Die Embeddings werden hierbei mit dem Elasticsearch-Datentyp `dense-vector` gespeichert.

Eine RAG-Anfrage kann mithilfe des Endpunkts `GET localhost:8080/rag/elasticsearch?message=MeineAnfrage` gestellt werden. 
Der ElasticsearchVectorStore nutzt hierbei einen KnnSearchRequest, um die zum übergebenen Embedding ähnlichsen Dokumente bei Elasticsearch abzufragen. 
